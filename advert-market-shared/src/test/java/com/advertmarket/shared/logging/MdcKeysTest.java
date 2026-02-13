package com.advertmarket.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.model.DealId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("MdcKeys utility tests")
class MdcKeysTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("putUserId sets userId in MDC")
    void putUserIdSetsValue() {
        MdcKeys.putUserId(42L);
        assertThat(MDC.get(MdcKeys.USER_ID)).isEqualTo("42");
    }

    @Test
    @DisplayName("clearUserId removes userId from MDC")
    void clearUserIdRemovesValue() {
        MdcKeys.putUserId(42L);
        MdcKeys.clearUserId();
        assertThat(MDC.get(MdcKeys.USER_ID)).isNull();
    }

    @Test
    @DisplayName("putDealId sets dealId in MDC")
    void putDealIdSetsValue() {
        UUID uuid = UUID.randomUUID();
        DealId dealId = DealId.of(uuid);
        MdcKeys.putDealId(dealId);
        assertThat(MDC.get(MdcKeys.DEAL_ID))
                .isEqualTo(uuid.toString());
    }

    @Test
    @DisplayName("clearDealId removes dealId from MDC")
    void clearDealIdRemovesValue() {
        MdcKeys.putDealId(DealId.of(UUID.randomUUID()));
        MdcKeys.clearDealId();
        assertThat(MDC.get(MdcKeys.DEAL_ID)).isNull();
    }

    @Test
    @DisplayName("Constants have expected values")
    void constantValues() {
        assertThat(MdcKeys.CORRELATION_ID)
                .isEqualTo("correlationId");
        assertThat(MdcKeys.USER_ID).isEqualTo("userId");
        assertThat(MdcKeys.DEAL_ID).isEqualTo("dealId");
    }
}
