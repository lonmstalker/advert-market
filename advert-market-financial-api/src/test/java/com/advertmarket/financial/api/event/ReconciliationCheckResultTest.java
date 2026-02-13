package com.advertmarket.financial.api.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReconciliationCheckResult")
class ReconciliationCheckResultTest {

    @Test
    @DisplayName("Creates defensive copy of details map")
    void defensiveCopy_detailsMap() {
        var mutable = new HashMap<String, Object>();
        mutable.put("balance", 100L);

        var result = new ReconciliationCheckResult(
                ReconciliationCheckStatus.PASS, mutable);

        mutable.put("extra", "value");

        assertThat(result.details()).hasSize(1);
        assertThat(result.details()).containsKey("balance");
    }

    @Test
    @DisplayName("Details map is immutable")
    void detailsMap_isImmutable() {
        var result = new ReconciliationCheckResult(
                ReconciliationCheckStatus.FAIL,
                Map.of("discrepancy", 42L));

        assertThatThrownBy(
                () -> result.details().put("new", "value"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Status is accessible")
    void statusAccessible() {
        var result = new ReconciliationCheckResult(
                ReconciliationCheckStatus.PASS, Map.of());
        assertThat(result.status())
                .isEqualTo(ReconciliationCheckStatus.PASS);
    }
}
