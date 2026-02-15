package com.advertmarket.financial.ton.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.financial.api.model.TonTransactionInfo;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonCenterException;
import org.ton.ton4j.toncenter.TonResponse;
import org.ton.ton4j.toncenter.model.EstimateFeeResponse;
import org.ton.ton4j.toncenter.model.MasterchainInfoResponse;
import org.ton.ton4j.toncenter.model.SendBocResponse;
import org.ton.ton4j.toncenter.model.TransactionResponse;

@DisplayName("TonCenterBlockchainAdapter — TON Center API adapter")
class TonCenterBlockchainAdapterTest {

    private TonCenter tonCenter;
    private MetricsFacade metrics;
    private TonCenterBlockchainAdapter adapter;

    @BeforeEach
    void setUp() {
        tonCenter = mock(TonCenter.class);
        metrics = mock(MetricsFacade.class);
        adapter = new TonCenterBlockchainAdapter(tonCenter, metrics);
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactions {

        @Test
        @DisplayName("Should map TransactionResponse to TonTransactionInfo list")
        void mapsTransactions() {
            var txId = new TransactionResponse.TransactionId();
            txId.setHash("abc123");
            txId.setLt("12345");

            var inMsg = new TransactionResponse.Message();
            inMsg.setSource("EQSource");
            inMsg.setDestination("EQDest");
            inMsg.setValue("1000000000");

            var tx = new TransactionResponse();
            tx.setTransactionId(txId);
            tx.setInMsg(inMsg);
            tx.setFee("5000000");
            tx.setUtime(1700000000L);

            var response = okResponse(List.of(tx));
            when(tonCenter.getTransactions("EQDest", 10)).thenReturn(response);

            List<TonTransactionInfo> result = adapter.getTransactions("EQDest", 10);

            assertThat(result).hasSize(1);
            TonTransactionInfo info = result.getFirst();
            assertThat(info.txHash()).isEqualTo("abc123");
            assertThat(info.lt()).isEqualTo(12345L);
            assertThat(info.fromAddress()).isEqualTo("EQSource");
            assertThat(info.toAddress()).isEqualTo("EQDest");
            assertThat(info.amountNano()).isEqualTo(1_000_000_000L);
            assertThat(info.feeNano()).isEqualTo(5_000_000L);
            assertThat(info.utime()).isEqualTo(1_700_000_000L);

            verify(metrics).incrementCounter(MetricNames.TON_API_REQUEST,
                    "method", "getTransactions");
        }

        @Test
        @DisplayName("Should throw DomainException on TonCenterException")
        void throwsOnApiError() {
            when(tonCenter.getTransactions(anyString(), any(Integer.class)))
                    .thenThrow(new TonCenterException("timeout"));

            assertThatThrownBy(() -> adapter.getTransactions("EQAddr", 10))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("getTransactions");
        }
    }

    @Nested
    @DisplayName("sendBoc")
    class SendBoc {

        @Test
        @DisplayName("Should return hash from sendBocReturnHash response")
        void returnsHash() {
            var bocResult = new SendBocResponse();
            bocResult.setHash("txhash_xyz");
            var response = okResponse(bocResult);
            when(tonCenter.sendBocReturnHash("base64boc")).thenReturn(response);

            String hash = adapter.sendBoc("base64boc");

            assertThat(hash).isEqualTo("txhash_xyz");
        }
    }

    @Nested
    @DisplayName("getMasterchainSeqno")
    class GetMasterchainSeqno {

        @Test
        @DisplayName("Should return seqno from masterchain info")
        void returnsSeqno() {
            var blockId = new MasterchainInfoResponse.BlockId();
            blockId.setSeqno(42L);
            var info = new MasterchainInfoResponse();
            info.setLast(blockId);
            var response = okResponse(info);
            when(tonCenter.getMasterchainInfo()).thenReturn(response);

            long seqno = adapter.getMasterchainSeqno();

            assertThat(seqno).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("getAddressBalance")
    class GetAddressBalance {

        @Test
        @DisplayName("Should parse string balance to long")
        void parsesBalance() {
            var response = okResponse("5000000000");
            when(tonCenter.getAddressBalance("EQAddr")).thenReturn(response);

            long balance = adapter.getAddressBalance("EQAddr");

            assertThat(balance).isEqualTo(5_000_000_000L);
        }
    }

    @Nested
    @DisplayName("getSeqno")
    class GetSeqno {

        @Test
        @DisplayName("Should delegate to TonCenter.getSeqno")
        void delegatesGetSeqno() {
            when(tonCenter.getSeqno("EQAddr")).thenReturn(7L);

            int seqno = adapter.getSeqno("EQAddr");

            assertThat(seqno).isEqualTo(7);
        }

        @Test
        @DisplayName("Should throw DomainException when TonCenter fails")
        void throwsOnFailure() {
            when(tonCenter.getSeqno("EQAddr"))
                    .thenThrow(new TonCenterException("connection refused"));

            assertThatThrownBy(() -> adapter.getSeqno("EQAddr"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("estimateFee")
    class EstimateFee {

        @Test
        @DisplayName("Should sum all source fee components")
        void sumsSourceFees() {
            var fees = new EstimateFeeResponse.Fees();
            fees.setInFwdFee(100L);
            fees.setStorageFee(200L);
            fees.setGasFee(300L);
            fees.setFwdFee(400L);

            var estimate = new EstimateFeeResponse();
            estimate.setSourceFees(fees);

            var response = okResponse(estimate);
            when(tonCenter.estimateFee("EQAddr", "body")).thenReturn(response);

            long fee = adapter.estimateFee("EQAddr", "body");

            assertThat(fee).isEqualTo(1000L);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> TonResponse<T> okResponse(T result) {
        var response = new TonResponse<T>();
        response.setOk(true);
        response.setResult(result);
        return response;
    }
}