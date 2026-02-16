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
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
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
        int seqno = blockchainPort.getSeqno(walletAddress);

        WalletV4R2Config txConfig = WalletV4R2Config.builder()
                .walletId(subwalletId)
                .seqno(seqno)
                .destination(Address.of(destinationAddress))
                .amount(BigInteger.valueOf(amountNano))
                .bounce(false)
                .build();

        wallet.createTransferBody(txConfig);
        Cell signedBody = wallet.createInternalSignedBody(txConfig);
        String base64Boc = signedBody.toBase64();

        String txHash;
        try {
            txHash = blockchainPort.sendBoc(base64Boc);
        } catch (DomainException ex) {
            log.error("Failed to send TON transaction: subwallet={}, dest={}, amount={}",
                    subwalletId, destinationAddress, amountNano, ex);
            throw new DomainException(ErrorCodes.TON_TX_FAILED,
                    "Failed to submit TON transaction: " + ex.getMessage(), ex);
        }

        metrics.incrementCounter(MetricNames.TON_TX_SUBMITTED,
                "direction", "OUT");

        log.info("TON transaction submitted: txHash={}, subwallet={}, dest={}, amount={}",
                txHash, subwalletId, destinationAddress, amountNano);

        return txHash;
    }

    private static TweetNaclFast.Signature.KeyPair deriveKeyPair(String mnemonic) {
        try {
            var pair = Mnemonic.toKeyPair(Arrays.asList(mnemonic.split("\\s+")));
            return TweetNaclFast.Signature.keyPair_fromSeed(pair.getSecretKey());
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Failed to derive key pair from mnemonic", ex);
        }
    }
}