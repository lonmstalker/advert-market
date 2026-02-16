package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.config.ChannelStatisticsCollectorProperties;
import com.advertmarket.marketplace.channel.service.ChannelStatisticsCollectorScheduler;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricsFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jooq.autoconfigure.ExceptionTranslatorExecuteListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = ChannelStatisticsCollectorIntegrationTest.TestConfig.class)
@DisplayName("Channel statistics collector integration")
class ChannelStatisticsCollectorIntegrationTest {

    private static final long OWNER_ID = 901L;
    private static final long CHANNEL_ID = -1009001L;
    private static final long CHANNEL_ID_2 = -1009002L;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        ContainerProperties.registerAll(registry);
    }

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
    }

    @Autowired
    private DSLContext dsl;

    @Autowired
    private TelegramChannelPort telegramChannelPort;

    @Autowired
    private ChannelStatisticsCollectorScheduler scheduler;

    @BeforeEach
    void setUp() {
        DatabaseSupport.cleanAllTables(dsl);
        reset(telegramChannelPort);
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
    }

    @Test
    @DisplayName("Updates subscriber count from Telegram")
    void updatesSubscriberCount() {
        when(telegramChannelPort.getChatMemberCount(CHANNEL_ID))
                .thenReturn(1337);

        scheduler.collectChannelStatistics();

        Integer subscriberCount = dsl.select(CHANNELS.SUBSCRIBER_COUNT)
                .from(CHANNELS)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .fetchOne(CHANNELS.SUBSCRIBER_COUNT);
        assertThat(subscriberCount).isEqualTo(1337);
    }

    @Test
    @DisplayName("Deactivates channel when Telegram reports CHANNEL_NOT_FOUND")
    void deactivatesWhenChannelMissing() {
        when(telegramChannelPort.getChatMemberCount(CHANNEL_ID))
                .thenThrow(new DomainException(
                        ErrorCodes.CHANNEL_NOT_FOUND,
                        "channel is missing"));

        scheduler.collectChannelStatistics();

        Boolean isActive = dsl.select(CHANNELS.IS_ACTIVE)
                .from(CHANNELS)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .fetchOne(CHANNELS.IS_ACTIVE);
        assertThat(isActive).isFalse();
    }

    @Test
    @DisplayName("Retries transient SERVICE_UNAVAILABLE and updates on success")
    void retriesTransientFailureAndEventuallyUpdates() {
        when(telegramChannelPort.getChatMemberCount(CHANNEL_ID))
                .thenThrow(new DomainException(
                        ErrorCodes.SERVICE_UNAVAILABLE,
                        "temporary outage"))
                .thenReturn(2024);

        scheduler.collectChannelStatistics();

        Integer subscriberCount = dsl.select(CHANNELS.SUBSCRIBER_COUNT)
                .from(CHANNELS)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .fetchOne(CHANNELS.SUBSCRIBER_COUNT);
        assertThat(subscriberCount).isEqualTo(2024);
        verify(telegramChannelPort, times(2))
                .getChatMemberCount(CHANNEL_ID);
    }

    @Test
    @DisplayName("Mixed batch continues after one channel fails")
    void mixedBatchContinuesAfterFailure() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID_2, OWNER_ID);
        when(telegramChannelPort.getChatMemberCount(CHANNEL_ID))
                .thenThrow(new DomainException(
                        ErrorCodes.CHANNEL_NOT_FOUND,
                        "missing"));
        when(telegramChannelPort.getChatMemberCount(CHANNEL_ID_2))
                .thenReturn(512);

        scheduler.collectChannelStatistics();

        Boolean firstActive = dsl.select(CHANNELS.IS_ACTIVE)
                .from(CHANNELS)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .fetchOne(CHANNELS.IS_ACTIVE);
        Integer secondSubscribers = dsl.select(CHANNELS.SUBSCRIBER_COUNT)
                .from(CHANNELS)
                .where(CHANNELS.ID.eq(CHANNEL_ID_2))
                .fetchOne(CHANNELS.SUBSCRIBER_COUNT);

        assertThat(firstActive).isFalse();
        assertThat(secondSubscribers).isEqualTo(512);
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {

        @Bean
        DSLContext dslContext(DataSource dataSource) {
            var configuration = new DefaultConfiguration()
                    .set(dataSource)
                    .set(SQLDialect.POSTGRES)
                    .set(new DefaultExecuteListenerProvider(
                            ExceptionTranslatorExecuteListener.DEFAULT));
            return DSL.using(configuration);
        }

        @Bean
        TelegramChannelPort telegramChannelPort() {
            return mock(TelegramChannelPort.class);
        }

        @Bean
        MetricsFacade metricsFacade() {
            return new MetricsFacade(new SimpleMeterRegistry());
        }

        @Bean
        ChannelStatisticsCollectorProperties
                channelStatisticsCollectorProperties() {
            return new ChannelStatisticsCollectorProperties(
                    true, 100, 0, 2);
        }

        @Bean
        ChannelStatisticsCollectorScheduler
                channelStatisticsCollectorScheduler(
                DSLContext dsl,
                TelegramChannelPort telegramChannelPort,
                MetricsFacade metricsFacade,
                ChannelStatisticsCollectorProperties properties) {
            return new ChannelStatisticsCollectorScheduler(
                    dsl, telegramChannelPort, metricsFacade, properties);
        }
    }
}
