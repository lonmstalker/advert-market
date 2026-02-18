package com.advertmarket.financial.ton.service;

import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.config.TonProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.sequence.SequenceAllocator;
import com.iwebpp.crypto.TweetNaclFast;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.mnemonic.Mnemonic;
import org.ton.ton4j.smartcontract.types.WalletV4R2Config;
import org.ton.ton4j.smartcontract.wallet.v4.WalletV4R2;

/**
 * Wallet service for TON blockchain operations.
 *
 * <p>Generates per-deal deposit addresses using WalletV4R2 subwallets
 * and submits signed transactions with distributed locking.
 *
 * <p>NOT {@code @Component} — wired via {@link com.advertmarket.financial.config.TonConfig}.
 */
@Slf4j
@SuppressFBWarnings(
        value = "CT_CONSTRUCTOR_THROW",
        justification = "Key derivation in constructor is intentional — "
                + "fail-fast on invalid mnemonic at startup")
public class TonWalletService implements TonWalletPort {

    private static final Duration TX_LOCK_TTL = Duration.ofSeconds(300);
    private static final int MAX_SEND_RETRIES = 3;
    private static final int RECOVERY_TX_FETCH_LIMIT = 20;
    private static final int DEPLOY_SEQNO_POLL_ATTEMPTS = 10;
    private static final Duration DEPLOY_SEQNO_POLL_DELAY = Duration.ofMillis(300);
    private static final Duration DEPLOY_SEND_RETRY_DELAY = Duration.ofMillis(500);

    private final TonBlockchainPort blockchainPort;
    private final DistributedLockPort lockPort;
    private final SequenceAllocator sequenceAllocator;
    private final MetricsFacade metrics;
    private final boolean isTestnet;
    private final TweetNaclFast.Signature.KeyPair keyPair;

    /**
     * Creates a new wallet service.
     *
     * @param blockchainPort    port for blockchain API calls
     * @param lockPort          distributed lock for TX serialization
     * @param sequenceAllocator bulk sequence allocator for subwallet IDs
     * @param metrics           metrics facade
     * @param props             TON configuration properties
     */
    public TonWalletService(TonBlockchainPort blockchainPort,
                            DistributedLockPort lockPort,
                            SequenceAllocator sequenceAllocator,
                            MetricsFacade metrics,
                            TonProperties props) {
        this.blockchainPort = blockchainPort;
        this.lockPort = lockPort;
        this.sequenceAllocator = sequenceAllocator;
        this.metrics = metrics;
        this.isTestnet = "testnet".equalsIgnoreCase(props.network());
        this.keyPair = deriveKeyPair(props.wallet().mnemonic());
    }

    @Override
    public @NonNull DepositAddressInfo generateDepositAddress(@NonNull DealId dealId) {
        long subwalletId = sequenceAllocator.next();

        WalletV4R2 wallet = WalletV4R2.builder()
                .keyPair(keyPair)
                .walletId(subwalletId)
                .build();

        String address = isTestnet
                ? wallet.getAddress().toNonBounceableTestnet()
                : wallet.getAddress().toNonBounceable();

        log.debug("Generated deposit address for deal={}, subwallet={}, address={}",
                dealId.value(), subwalletId, address);

        return new DepositAddressInfo(address, subwalletId);
    }

    @Override
    public @NonNull String submitTransaction(int subwalletId,
                                              @NonNull String destinationAddress,
                                              long amountNano) {
        String lockKey = "ton:subwallet-tx:" + subwalletId;
        return lockPort.withLock(lockKey, TX_LOCK_TTL, () ->
                doSubmitTransaction(subwalletId, destinationAddress, amountNano));
    }

    private String doSubmitTransaction(int subwalletId, String destinationAddress,
                                       long amountNano) {
        WalletV4R2 wallet = WalletV4R2.builder()
                .keyPair(keyPair)
                .walletId(subwalletId)
                .build();

        String walletAddress = wallet.getAddress().toBounceable();
        long seqno = resolveSeqno(wallet, walletAddress);

        String base64Boc = buildSignedBoc(wallet, subwalletId, seqno,
                destinationAddress, amountNano);

        for (int attempt = 0; attempt < MAX_SEND_RETRIES; attempt++) {
            try {
                String txHash = blockchainPort.sendBoc(base64Boc);
                metrics.incrementCounter(MetricNames.TON_TX_SUBMITTED,
                        "direction", "OUT");
                log.info("TON transaction submitted: txHash={}, subwallet={}, "
                                + "dest={}, amount={}, seqno={}",
                        txHash, subwalletId, destinationAddress,
                        amountNano, seqno);
                return txHash;
            } catch (DomainException ex) {
                String recovered = handleSendFailure(walletAddress, seqno,
                        subwalletId, destinationAddress, amountNano, attempt);
                if (recovered != null) {
                    return recovered;
                }
            }
        }

        throw new DomainException(ErrorCodes.TON_TX_FAILED,
                "Failed to submit TON transaction after "
                        + MAX_SEND_RETRIES + " retries");
    }

    private String handleSendFailure(String walletAddress, long originalSeqno,
                                      int subwalletId,
                                      String destinationAddress,
                                      long amountNano,
                                      int attempt) {
        long currentSeqno = fetchCurrentSeqnoAfterSendFailure(
                walletAddress, originalSeqno);
        if (currentSeqno > originalSeqno) {
            log.warn("Seqno advanced {} -> {} after sendBoc failure, "
                            + "recovering TX hash for subwallet={}",
                    originalSeqno, currentSeqno, subwalletId);
            return recoverTxHash(
                    walletAddress,
                    destinationAddress,
                    amountNano);
        }

        if (attempt < MAX_SEND_RETRIES - 1) {
            log.warn("sendBoc failed (attempt {}/{}), seqno unchanged, "
                            + "retrying: subwallet={}",
                    attempt + 1, MAX_SEND_RETRIES, subwalletId);
        }
        return null;
    }

    private long resolveSeqno(WalletV4R2 wallet, String walletAddress) {
        try {
            return blockchainPort.getSeqno(walletAddress);
        } catch (DomainException ex) {
            if (isUninitializedWallet(ex)) {
                log.info("TON wallet is not initialized yet, deploying wallet first: wallet={}",
                        walletAddress);
                deployWallet(wallet, walletAddress);
                return waitForWalletSeqno(walletAddress);
            }
            throw ex;
        }
    }

    private void deployWallet(WalletV4R2 wallet, String walletAddress) {
        String deployBoc = wallet.prepareDeployMsg().toCell().toBase64();
        for (int attempt = 1; attempt <= MAX_SEND_RETRIES; attempt++) {
            try {
                String deployTxHash = blockchainPort.sendBoc(deployBoc);
                log.info("TON wallet deploy submitted: wallet={}, txHash={}",
                        walletAddress, deployTxHash);
                return;
            } catch (DomainException ex) {
                if (isWalletInitialized(walletAddress)) {
                    log.warn("TON wallet deploy send failed but wallet is already initialized: "
                                    + "wallet={}, attempt={}/{}",
                            walletAddress,
                            attempt,
                            MAX_SEND_RETRIES);
                    return;
                }
                if (attempt == MAX_SEND_RETRIES) {
                    throw ex;
                }
                log.warn("TON wallet deploy send failed, retrying: wallet={}, attempt={}/{}",
                        walletAddress,
                        attempt,
                        MAX_SEND_RETRIES);
                sleepQuietly(DEPLOY_SEND_RETRY_DELAY);
            }
        }
    }

    private boolean isWalletInitialized(String walletAddress) {
        try {
            blockchainPort.getSeqno(walletAddress);
            return true;
        } catch (DomainException ex) {
            if (isUninitializedWallet(ex)) {
                return false;
            }
            throw ex;
        }
    }

    private long waitForWalletSeqno(String walletAddress) {
        for (int attempt = 1; attempt <= DEPLOY_SEQNO_POLL_ATTEMPTS; attempt++) {
            try {
                return blockchainPort.getSeqno(walletAddress);
            } catch (DomainException ex) {
                if (!isUninitializedWallet(ex)) {
                    throw ex;
                }
                if (attempt == DEPLOY_SEQNO_POLL_ATTEMPTS) {
                    break;
                }
                sleepQuietly(DEPLOY_SEQNO_POLL_DELAY);
            }
        }
        throw new DomainException(
                ErrorCodes.TON_TX_FAILED,
                "Wallet deployment was submitted but seqno is still unavailable"
                        + " after " + DEPLOY_SEQNO_POLL_ATTEMPTS + " attempts");
    }

    private long fetchCurrentSeqnoAfterSendFailure(
            String walletAddress, long fallbackSeqno) {
        try {
            return blockchainPort.getSeqno(walletAddress);
        } catch (DomainException ex) {
            if (isUninitializedWallet(ex)) {
                return fallbackSeqno;
            }
            throw ex;
        }
    }

    @SuppressWarnings("ReferenceEquality")
    private static boolean isUninitializedWallet(DomainException ex) {
        if (ex.getErrorCode() != ErrorCodes.TON_API_ERROR) {
            return false;
        }
        return containsInErrorChain(ex, "exitCode: -13");
    }

    private static boolean containsInErrorChain(
            Throwable throwable, String needle) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(needle)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void sleepQuietly(Duration delay) {
        if (Thread.currentThread().isInterrupted()) {
            throw new DomainException(
                    ErrorCodes.TON_TX_FAILED,
                    "Interrupted while waiting for TON wallet deployment");
        }
        LockSupport.parkNanos(delay.toNanos());
        if (Thread.interrupted()) {
            throw new DomainException(
                    ErrorCodes.TON_TX_FAILED,
                    "Interrupted while waiting for TON wallet deployment");
        }
    }

    private String recoverTxHash(String walletAddress,
            String destinationAddress,
            long amountNano) {
        var recent = blockchainPort.getOutgoingTransfers(
                walletAddress,
                RECOVERY_TX_FETCH_LIMIT);
        String recovered = recent.stream()
                .filter(tx -> tx.amountNano() == amountNano)
                .filter(tx -> isSameTonAddress(
                        tx.toAddress(),
                        destinationAddress))
                .map(tx -> tx.txHash())
                .findFirst()
                .orElse("");
        if (!recovered.isBlank()) {
            metrics.incrementCounter(MetricNames.TON_TX_SUBMITTED,
                    "direction", "OUT");
            log.info("Recovered TX hash after seqno advance: {}",
                    recovered);
            return recovered;
        }
        metrics.incrementCounter(MetricNames.TON_TX_SUBMITTED,
                "direction", "OUT");
        log.warn("Seqno advanced but no matching outgoing TX found, "
                + "returning empty hash");
        return "";
    }

    private static boolean isSameTonAddress(
            String leftAddress,
            String rightAddress) {
        try {
            return Address.of(leftAddress).toRaw()
                    .equals(Address.of(rightAddress).toRaw());
        } catch (IllegalArgumentException ignored) {
            return leftAddress.equals(rightAddress);
        }
    }

    private String buildSignedBoc(WalletV4R2 wallet, int subwalletId,
                                   long seqno, String destinationAddress,
                                   long amountNano) {
        WalletV4R2Config txConfig = WalletV4R2Config.builder()
                .walletId(subwalletId)
                .seqno(seqno)
                .destination(Address.of(destinationAddress))
                .amount(BigInteger.valueOf(amountNano))
                .bounce(false)
                .build();

        var externalMessage = wallet.prepareExternalMsg(txConfig);
        return externalMessage.toCell().toBase64();
    }

    // security: prevent mnemonic leak via exception chain
    @SuppressWarnings("ThrowInsideCatchWithoutCause")
    private static TweetNaclFast.Signature.KeyPair deriveKeyPair(
            String mnemonic) {
        try {
            var pair = Mnemonic.toKeyPair(
                    Arrays.asList(mnemonic.split("\\s+")));
            return TweetNaclFast.Signature.keyPair_fromSeed(pair.getSecretKey());
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.error("Failed to derive key pair: invalid mnemonic format");
            throw new IllegalStateException(
                    "Failed to derive key pair from mnemonic");
        }
    }
}
