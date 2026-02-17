package com.advertmarket.financial.ton.client;

import com.advertmarket.financial.api.model.TonTransactionInfo;
import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonResponse;

/**
 * Adapter that wraps ton4j {@link TonCenter} behind {@link TonBlockchainPort}.
 *
 * <p>NOT {@code @Component} — wired via
 * {@link com.advertmarket.financial.config.TonConfig}.
 */
@Slf4j
@RequiredArgsConstructor
public class TonCenterBlockchainAdapter implements TonBlockchainPort {

    private final TonCenter tonCenter;
    private final MetricsFacade metrics;

    @Override
    public @NonNull List<TonTransactionInfo> getTransactions(
            @NonNull String address, int limit) {
        metrics.incrementCounter(MetricNames.TON_API_REQUEST, "method", "getTransactions");
        try {
            var response = tonCenter.getTransactions(address, limit);
            checkResponse(response, "getTransactions");
            return response.getResult().stream()
                    .filter(tx -> {
                        var txId = tx.getTransactionId();
                        if (txId == null || txId.getHash() == null) {
                            log.warn("Skipping TON transaction with missing "
                                    + "txId/hash for address={}", address);
                            return false;
                        }
                        if (tx.getInMsg() == null) {
                            log.warn("Skipping TON transaction with missing "
                                    + "inMsg: txHash={}", txId.getHash());
                            return false;
                        }
                        return true;
                    })
                    .map(tx -> {
                        var txId = tx.getTransactionId();
                        var inMsg = tx.getInMsg();
                        return new TonTransactionInfo(
                                txId.getHash(),
                                parseLong(txId.getLt()),
                                inMsg.getSource(),
                                inMsg.getDestination() != null ? inMsg.getDestination() : address,
                                parseLong(inMsg.getValue()),
                                parseLong(tx.getFee()),
                                tx.getUtime() != null ? tx.getUtime() : 0L
                        );
                    })
                    .toList();
        } catch (DomainException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw wrapThrowable("getTransactions", ex);
        }
    }

    @Override
    public @NonNull String sendBoc(@NonNull String base64Boc) {
        metrics.incrementCounter(MetricNames.TON_API_REQUEST, "method", "sendBoc");
        try {
            var response = tonCenter.sendBocReturnHash(base64Boc);
            checkResponse(response, "sendBoc");
            return response.getResult().getHash();
        } catch (DomainException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw wrapThrowable("sendBoc", ex);
        }
    }

    @Override
    public long getMasterchainSeqno() {
        metrics.incrementCounter(MetricNames.TON_API_REQUEST, "method", "getMasterchainInfo");
        try {
            var response = tonCenter.getMasterchainInfo();
            checkResponse(response, "getMasterchainInfo");
            return response.getResult().getLast().getSeqno();
        } catch (DomainException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw wrapThrowable("getMasterchainInfo", ex);
        }
    }

    @Override
    public long getAddressBalance(@NonNull String address) {
        metrics.incrementCounter(MetricNames.TON_API_REQUEST, "method", "getAddressBalance");
        try {
            var response = tonCenter.getAddressBalance(address);
            checkResponse(response, "getAddressBalance");
            return Long.parseLong(response.getResult());
        } catch (DomainException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw wrapThrowable("getAddressBalance", ex);
        }
    }

    @Override
    public long getSeqno(@NonNull String address) {
        metrics.incrementCounter(MetricNames.TON_API_REQUEST, "method", "getSeqno");
        try {
            return tonCenter.getSeqno(address);
        } catch (DomainException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw wrapThrowable("getSeqno", ex);
        }
    }

    @Override
    public long estimateFee(@NonNull String address, @NonNull String base64Body) {
        metrics.incrementCounter(MetricNames.TON_API_REQUEST, "method", "estimateFee");
        try {
            var response = tonCenter.estimateFee(address, base64Body);
            checkResponse(response, "estimateFee");
            var fees = response.getResult().getSourceFees();
            return nullToZero(fees.getInFwdFee())
                    + nullToZero(fees.getStorageFee())
                    + nullToZero(fees.getGasFee())
                    + nullToZero(fees.getFwdFee());
        } catch (DomainException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw wrapThrowable("estimateFee", ex);
        }
    }

    private void checkResponse(TonResponse<?> response, String method) {
        if (!response.isOk()) {
            metrics.incrementCounter(MetricNames.TON_API_ERROR, "method", method);
            throw new DomainException(ErrorCodes.TON_API_ERROR,
                    "TON Center API error in %s: %s".formatted(method, response.getError()));
        }
    }

    private DomainException wrapThrowable(String method, Throwable ex) {
        if (ex instanceof Error error && isFatalError(error)) {
            throw error;
        }
        metrics.incrementCounter(MetricNames.TON_API_ERROR, "method", method);
        log.error("TON Center API call failed: method={}", method, ex);
        return new DomainException(ErrorCodes.TON_API_ERROR,
                "TON Center API call failed: " + method, ex);
    }

    private static boolean isFatalError(Error error) {
        return error instanceof VirtualMachineError
                || error instanceof LinkageError;
    }

    private static long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private static long nullToZero(Long value) {
        return value != null ? value : 0L;
    }
}
