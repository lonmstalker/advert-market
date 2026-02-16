package com.advertmarket.financial.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@DisplayName("TonConfig wiring")
class TonConfigTest {

    @Test
    @DisplayName("tonBlockchainPort bean is marked as @Primary")
    void tonBlockchainPortBeanIsPrimary() throws NoSuchMethodException {
        Method beanMethod = TonConfig.class.getMethod(
                "tonBlockchainPort",
                com.advertmarket.financial.ton.client.TonCenterBlockchainAdapter.class,
                io.github.resilience4j.circuitbreaker.CircuitBreaker.class,
                io.github.resilience4j.bulkhead.Bulkhead.class);

        assertThat(beanMethod.isAnnotationPresent(Primary.class)).isTrue();
    }

    @Test
    @DisplayName("TonProperties.Deposit is exposed as bean")
    void tonDepositPropertiesBeanIsPublished() throws NoSuchMethodException {
        Method beanMethod = TonConfig.class.getMethod(
                "tonDepositProperties",
                TonProperties.class);

        assertThat(beanMethod.isAnnotationPresent(Bean.class)).isTrue();
    }
}
