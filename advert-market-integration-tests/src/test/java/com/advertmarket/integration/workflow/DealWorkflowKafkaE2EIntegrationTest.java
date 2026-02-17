package com.advertmarket.integration.workflow;

import static com.advertmarket.db.generated.tables.Deals.DEALS;
import static com.advertmarket.db.generated.tables.LedgerEntries.LEDGER_ENTRIES;
import static com.advertmarket.db.generated.tables.NotificationOutbox.NOTIFICATION_OUTBOX;
import static com.advertmarket.db.generated.tables.TonTransactions.TON_TRANSACTIONS;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.app.AdvertMarketApplication;
import com.advertmarket.app.config.SecurityConfig;
import com.advertmarket.app.config.WorkerEventPortStubConfig;
import com.advertmarket.communication.api.notification.NotificationPort;
import com.advertmarket.communication.api.notification.NotificationRequest;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.adapter.FinancialEventAdapter;
import com.advertmarket.deal.service.DealTransitionService;
import com.advertmarket.delivery.api.event.DeliveryVerifiedEvent;
import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.financial.api.model.TonTransactionInfo;
import com.advertmarket.financial.api.port.DepositPort;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.financial.api.port.FinancialEventPort;
import com.advertmarket.financial.api.port.ReconciliationResultPort;
import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.config.TonConfig;
import com.advertmarket.financial.config.TonProperties;
import com.advertmarket.financial.ton.service.ConfirmationPolicyService;
import com.advertmarket.financial.ton.config.TonResilienceConfig;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.RedisSupport;
import com.advertmarket.integration.support.SharedContainers;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.UserId;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.awaitility.Awaitility;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Honest E2E workflow tests against real Kafka + Postgres + Redis.
 *
 * <p>Only external boundaries are stubbed:
 * TON blockchain, TON wallet, Telegram notification delivery.
 */
@SpringBootTest(
        classes = DealWorkflowKafkaE2EIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "app.outbox.poll-interval=100ms",
                "app.ton.deposit.poll-interval=150ms",
                "app.ton.deposit.max-poll-duration=30m",
                "app.ton.deposit.batch-size=100",
                "app.marketplace.channel.statistics.enabled=false",
                "app.deal.timeout.poll-interval=1h"
        })
@Import(DealWorkflowKafkaE2EIntegrationTest.StubConfig.class)
@DisplayName("Workflow Kafka E2E")
class DealWorkflowKafkaE2EIntegrationTest {

    private static final long ADVERTISER_ID = 101L;
    private static final long OWNER_ID = 202L;
    private static final long OPERATOR_ID = 303L;
    private static final long CHANNEL_ID = -1001234567011L;
    private static final int COMMISSION_RATE_BP = 1000;
    private static final String OWNER_TON_ADDRESS = "UQ_owner_wallet_test";
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(35);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        ContainerProperties.registerAllWithKafka(registry);
        registry.add("app.cors.allowed-origins[0]", () -> "https://example.test");
        registry.add("app.auth.jwt.secret", () ->
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        registry.add("app.auth.jwt.expiration", () -> "3600");
        registry.add("app.auth.anti-replay-window-seconds", () -> "300");
        registry.add("app.auth.rate-limiter.max-attempts", () -> "10");
        registry.add("app.auth.rate-limiter.window-seconds", () -> "60");
        registry.add("app.locale-currency.fallback-currency", () -> "USD");
        registry.add("app.locale-currency.language-map.ru", () -> "RUB");
        registry.add("app.locale-currency.language-map.en", () -> "USD");
        registry.add("app.marketplace.channel.bot-user-id", () -> "42");
        registry.add("app.marketplace.team.max-managers", () -> "5");
        registry.add("app.internal-api.api-key", () -> "it-internal-api-key");
        registry.add("app.internal-api.allowed-networks[0]", () -> "127.0.0.1/32");
        registry.add("app.canary.admin-token", () -> "it-canary-token");
        registry.add("app.pii.encryption.key", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("app.telegram.bot-token", () -> "123456:it-bot-token");
        registry.add("app.telegram.bot-username", () -> "it_bot");
        registry.add("app.telegram.webhook.secret", () -> "it-webhook-secret");
        registry.add("app.telegram.webapp.url", () -> "https://example.test/app");
        registry.add("app.ton.api.key", () -> "it-ton-api-key");
        registry.add("app.ton.network", () -> "testnet");
        registry.add("app.ton.wallet.mnemonic", () ->
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about");
    }

    @BeforeAll
    static void migrate() {
        DatabaseSupport.ensureMigrated();
        ensureKafkaTopics();
    }

    private static void ensureKafkaTopics() {
        var adminProps = Map.<String, Object>of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                SharedContainers.kafkaBootstrapServers());
        var requiredTopics = List.of(
                new NewTopic(TopicNames.DEAL_STATE_CHANGED, 3, (short) 1),
                new NewTopic(TopicNames.DEAL_DEADLINES, 3, (short) 1),
                new NewTopic(TopicNames.FINANCIAL_COMMANDS, 6, (short) 1),
                new NewTopic(TopicNames.FINANCIAL_EVENTS, 6, (short) 1),
                new NewTopic(TopicNames.DELIVERY_COMMANDS, 3, (short) 1),
                new NewTopic(TopicNames.DELIVERY_EVENTS, 3, (short) 1),
                new NewTopic(TopicNames.COMMUNICATION_NOTIFICATIONS, 3, (short) 1));

        try (var admin = AdminClient.create(adminProps)) {
            Set<String> existing = admin.listTopics()
                    .names()
                    .get(10, TimeUnit.SECONDS);
            var missing = requiredTopics.stream()
                    .filter(topic -> !existing.contains(topic.name()))
                    .toList();
            if (!missing.isEmpty()) {
                admin.createTopics(missing)
                        .all()
                        .get(10, TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to ensure Kafka topics for workflow E2E",
                    ex);
        }
    }

    @Autowired
    private DSLContext dsl;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private DealTransitionService transitionService;

    @Autowired
    private DepositPort depositPort;

    @Autowired
    private EscrowPort escrowPort;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JsonFacade jsonFacade;

    @Autowired
    private StubTonBlockchainPort tonBlockchainPort;

    @Autowired
    private StubTonWalletPort tonWalletPort;

    @Autowired
    private StubNotificationPort notificationPort;

    @Autowired
    private FinancialEventPort financialEventPort;

    @BeforeEach
    void setUp() {
        DatabaseSupport.cleanAllTables(dsl);
        RedisSupport.flushAll();
        tonBlockchainPort.reset();
        tonWalletPort.reset();
        notificationPort.reset();
        assertThat(financialEventPort)
                .isInstanceOf(FinancialEventAdapter.class);

        TestDataFactory.upsertUser(dsl, ADVERTISER_ID);
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.upsertUser(dsl, OPERATOR_ID);
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
        dsl.update(USERS)
                .set(USERS.TON_ADDRESS, OWNER_TON_ADDRESS)
                .where(USERS.ID.eq(OWNER_ID))
                .execute();
    }

    @Test
    @DisplayName("Deposit happy path: AWAITING_PAYMENT -> FUNDED with persisted tx hash")
    void depositHappyPath_transitionsToFunded() {
        long amountNano = 50_000_000_000L;
        UUID dealId = insertDeal(DealStatus.OFFER_PENDING, amountNano);
        transition(
                dealId,
                DealStatus.ACCEPTED,
                ActorType.CHANNEL_OWNER,
                OWNER_ID,
                "owner accepted offer",
                null,
                null);
        awaitDealStatus(dealId, DealStatus.AWAITING_PAYMENT);

        var address = awaitDepositAddress(dealId);
        var txHash = "tx-deposit-happy-" + dealId;
        tonBlockchainPort.putInbound(
                address,
                List.of(new TonTransactionInfo(
                        txHash,
                        10001L,
                        "UQ_advertiser_source_1",
                        address,
                        amountNano,
                        1_000_000L,
                        Instant.now().getEpochSecond())));

        awaitDealStatus(dealId, DealStatus.FUNDED);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_COMMANDS, EventTypes.WATCH_DEPOSIT);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_EVENTS, EventTypes.DEPOSIT_CONFIRMED);
        awaitOutboxDelivered(dealId, TopicNames.COMMUNICATION_NOTIFICATIONS, EventTypes.NOTIFICATION);

        String persistedDepositTx = dsl.select(DEALS.DEPOSIT_TX_HASH)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.DEPOSIT_TX_HASH);
        assertThat(persistedDepositTx).isEqualTo(txHash);
        assertThat(latestInboundStatus(dealId)).isEqualTo("CONFIRMED");
        assertThat(notificationPort.requests()).isNotEmpty();
    }

    @Test
    @DisplayName("Large deposit: review required -> approve -> FUNDED")
    void largeDepositReviewApprove_transitionsToFunded() {
        long amountNano = 2_000_000_000_000L;
        UUID dealId = insertDeal(DealStatus.OFFER_PENDING, amountNano);
        transition(
                dealId,
                DealStatus.ACCEPTED,
                ActorType.CHANNEL_OWNER,
                OWNER_ID,
                "owner accepted offer",
                null,
                null);
        awaitDealStatus(dealId, DealStatus.AWAITING_PAYMENT);

        String address = awaitDepositAddress(dealId);
        String txHash = "tx-deposit-review-approve-" + dealId;
        tonBlockchainPort.putInbound(
                address,
                List.of(new TonTransactionInfo(
                        txHash,
                        20001L,
                        "UQ_advertiser_source_2",
                        address,
                        amountNano,
                        2_000_000L,
                        Instant.now().getEpochSecond())));

        awaitInboundStatus(dealId, "AWAITING_OPERATOR_REVIEW");
        assertThat(readDealStatus(dealId)).isEqualTo(DealStatus.AWAITING_PAYMENT);

        depositPort.approveDeposit(DealId.of(dealId));

        awaitDealStatus(dealId, DealStatus.FUNDED);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_EVENTS, EventTypes.DEPOSIT_CONFIRMED);

        assertThat(latestInboundStatus(dealId)).isEqualTo("CONFIRMED");
        assertThat(dsl.select(DEALS.DEPOSIT_TX_HASH)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.DEPOSIT_TX_HASH))
                .isEqualTo(txHash);
    }

    @Test
    @DisplayName("Large deposit: review required -> reject -> CANCELLED + REJECTED")
    void largeDepositReject_transitionsToCancelled() {
        long amountNano = 2_100_000_000_000L;
        UUID dealId = insertDeal(DealStatus.OFFER_PENDING, amountNano);
        transition(
                dealId,
                DealStatus.ACCEPTED,
                ActorType.CHANNEL_OWNER,
                OWNER_ID,
                "owner accepted offer",
                null,
                null);
        awaitDealStatus(dealId, DealStatus.AWAITING_PAYMENT);

        String address = awaitDepositAddress(dealId);
        tonBlockchainPort.putInbound(
                address,
                List.of(new TonTransactionInfo(
                        "tx-deposit-review-reject-" + dealId,
                        30001L,
                        "UQ_advertiser_source_3",
                        address,
                        amountNano,
                        1_000_000L,
                        Instant.now().getEpochSecond())));

        awaitInboundStatus(dealId, "AWAITING_OPERATOR_REVIEW");

        depositPort.rejectDeposit(DealId.of(dealId));

        awaitDealStatus(dealId, DealStatus.CANCELLED);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_EVENTS, EventTypes.DEPOSIT_FAILED);

        assertThat(latestInboundStatus(dealId)).isEqualTo("REJECTED");
        assertThat(dsl.select(DEALS.REFUNDED_TX_HASH)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.REFUNDED_TX_HASH))
                .isNull();
    }

    @Test
    @DisplayName("Delivery verified -> payout command -> payout completed -> tx hash persisted")
    void deliveryVerified_executesPayoutAndPersistsTxHash() throws Exception {
        long amountNano = 1_000_000_000L;
        UUID dealId = insertDeal(DealStatus.DELIVERY_VERIFYING, amountNano);
        primeDealWalletAndFunding(dealId, amountNano, "UQ_seed_source_delivery");

        var envelope = EventEnvelope.create(
                EventTypes.DELIVERY_VERIFIED,
                DealId.of(dealId),
                new DeliveryVerifiedEvent(777L, 3, 0, "hash-delivery-ok"));
        kafkaTemplate.send(
                        TopicNames.DELIVERY_EVENTS,
                        dealId.toString(),
                        jsonFacade.toJson(envelope))
                .get();

        awaitDealStatus(dealId, DealStatus.COMPLETED_RELEASED);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_COMMANDS, EventTypes.EXECUTE_PAYOUT);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_EVENTS, EventTypes.PAYOUT_COMPLETED);

        String payoutTxHash = dsl.select(DEALS.PAYOUT_TX_HASH)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.PAYOUT_TX_HASH);
        assertThat(payoutTxHash).isNotBlank();

        int payoutEntries = dsl.selectCount()
                .from(LEDGER_ENTRIES)
                .where(LEDGER_ENTRIES.DEAL_ID.eq(dealId))
                .and(LEDGER_ENTRIES.ENTRY_TYPE.eq(EntryType.OWNER_WITHDRAWAL.name()))
                .fetchOne(0, int.class);
        assertThat(payoutEntries).isGreaterThan(0);
    }

    @Test
    @DisplayName("Funded cancel emits refund command and persists refund tx hash")
    void fundedCancel_executesRefundAndPersistsTxHash() {
        long amountNano = 1_400_000_000L;
        UUID dealId = insertDeal(DealStatus.FUNDED, amountNano);
        primeDealWalletAndFunding(dealId, amountNano, "UQ_seed_source_cancel");

        transition(
                dealId,
                DealStatus.CANCELLED,
                ActorType.ADVERTISER,
                ADVERTISER_ID,
                "cancel after funding",
                null,
                null);

        awaitDealStatus(dealId, DealStatus.CANCELLED);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_COMMANDS, EventTypes.EXECUTE_REFUND);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_EVENTS, EventTypes.REFUND_COMPLETED);

        String refundTxHash = dsl.select(DEALS.REFUNDED_TX_HASH)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.REFUNDED_TX_HASH);
        assertThat(refundTxHash).isNotBlank();

        int refundEntries = dsl.selectCount()
                .from(LEDGER_ENTRIES)
                .where(LEDGER_ENTRIES.DEAL_ID.eq(dealId))
                .and(LEDGER_ENTRIES.ENTRY_TYPE.eq(EntryType.ESCROW_REFUND.name()))
                .fetchOne(0, int.class);
        assertThat(refundEntries).isGreaterThan(0);
    }

    @Test
    @DisplayName("DISPUTED -> PARTIALLY_REFUNDED emits partial refund+payout and persists both tx hashes")
    void disputedPartiallyRefunded_executesBothFinancialCommands() {
        long partialRefundNano = 650_000_000L;
        long partialPayoutNano = 350_000_000L;
        long dealAmountNano = partialRefundNano + partialPayoutNano;
        UUID dealId = insertDeal(DealStatus.DISPUTED, dealAmountNano);
        primeDealWalletAndFunding(dealId, dealAmountNano, "UQ_seed_source_partial");

        transition(
                dealId,
                DealStatus.PARTIALLY_REFUNDED,
                ActorType.PLATFORM_OPERATOR,
                OPERATOR_ID,
                "operator partial resolution",
                partialRefundNano,
                partialPayoutNano);

        awaitDealStatus(dealId, DealStatus.PARTIALLY_REFUNDED);
        awaitOutboxDelivered(
                dealId,
                TopicNames.DEAL_STATE_CHANGED,
                EventTypes.DEAL_STATE_CHANGED);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_COMMANDS, EventTypes.EXECUTE_REFUND);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_COMMANDS, EventTypes.EXECUTE_PAYOUT);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_EVENTS, EventTypes.REFUND_COMPLETED);
        awaitOutboxDelivered(dealId, TopicNames.FINANCIAL_EVENTS, EventTypes.PAYOUT_COMPLETED);

        String payoutTxHash = dsl.select(DEALS.PAYOUT_TX_HASH)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.PAYOUT_TX_HASH);
        String refundTxHash = dsl.select(DEALS.REFUNDED_TX_HASH)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.REFUNDED_TX_HASH);

        assertThat(payoutTxHash).isNotBlank();
        assertThat(refundTxHash).isNotBlank();
    }

    private UUID insertDeal(DealStatus status, long amountNano) {
        UUID dealId = UUID.randomUUID();
        long commissionNano = amountNano * COMMISSION_RATE_BP / 10_000L;
        Instant now = Instant.now();
        dealRepository.insert(new DealRecord(
                dealId,
                CHANNEL_ID,
                ADVERTISER_ID,
                OWNER_ID,
                null,
                status,
                amountNano,
                COMMISSION_RATE_BP,
                commissionNano,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                now,
                now));
        return dealId;
    }

    private void primeDealWalletAndFunding(
            UUID dealId,
            long amountNano,
            String fromAddress) {
        int subwalletId = 7000 + Math.floorMod(dealId.hashCode(), 1000);
        String depositAddress = "UQ_seed_deposit_" + subwalletId;
        DealId id = DealId.of(dealId);

        dealRepository.setDepositAddress(id, depositAddress, subwalletId);
        dealRepository.setFunded(id, Instant.now(), "seed-deposit-tx-" + dealId);
        escrowPort.confirmDeposit(
                id,
                "seed-ledger-tx-" + dealId,
                amountNano,
                amountNano,
                1,
                fromAddress);

        dsl.insertInto(TON_TRANSACTIONS)
                .set(TON_TRANSACTIONS.DEAL_ID, dealId)
                .set(TON_TRANSACTIONS.DIRECTION, "IN")
                .set(TON_TRANSACTIONS.AMOUNT_NANO, amountNano)
                .set(TON_TRANSACTIONS.FROM_ADDRESS, fromAddress)
                .set(TON_TRANSACTIONS.TO_ADDRESS, depositAddress)
                .set(TON_TRANSACTIONS.SUBWALLET_ID, subwalletId)
                .set(TON_TRANSACTIONS.TX_HASH, "seed-inbound-" + dealId)
                .set(TON_TRANSACTIONS.STATUS, "CONFIRMED")
                .set(TON_TRANSACTIONS.CONFIRMATIONS, 3)
                .set(TON_TRANSACTIONS.VERSION, 0)
                .set(TON_TRANSACTIONS.CONFIRMED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();
    }

    private void transition(
            UUID dealId,
            DealStatus targetStatus,
            ActorType actorType,
            Long actorId,
            String reason,
            Long partialRefundNano,
            Long partialPayoutNano) {
        transitionService.transition(new DealTransitionCommand(
                DealId.of(dealId),
                targetStatus,
                actorId,
                actorType,
                reason,
                partialRefundNano,
                partialPayoutNano));
    }

    private String awaitDepositAddress(UUID dealId) {
        AtomicLong subwallet = new AtomicLong(-1);
        var holder = new java.util.concurrent.atomic.AtomicReference<String>();
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    String address = dsl.select(DEALS.DEPOSIT_ADDRESS)
                            .from(DEALS)
                            .where(DEALS.ID.eq(dealId))
                            .fetchOne(DEALS.DEPOSIT_ADDRESS);
                    Integer subwalletId = dsl.select(DEALS.SUBWALLET_ID)
                            .from(DEALS)
                            .where(DEALS.ID.eq(dealId))
                            .fetchOne(DEALS.SUBWALLET_ID);
                    assertThat(address).isNotBlank();
                    assertThat(subwalletId).isNotNull();
                    holder.set(address);
                    subwallet.set(subwalletId);
                });
        assertThat(subwallet.get()).isPositive();
        return holder.get();
    }

    private void awaitDealStatus(UUID dealId, DealStatus expected) {
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() ->
                        assertThat(readDealStatus(dealId)).isEqualTo(expected));
    }

    private DealStatus readDealStatus(UUID dealId) {
        String raw = dsl.select(DEALS.STATUS)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.STATUS);
        return DealStatus.valueOf(raw);
    }

    private void awaitInboundStatus(UUID dealId, String expectedStatus) {
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() ->
                        assertThat(latestInboundStatus(dealId)).isEqualTo(expectedStatus));
    }

    private String latestInboundStatus(UUID dealId) {
        return dsl.select(TON_TRANSACTIONS.STATUS)
                .from(TON_TRANSACTIONS)
                .where(TON_TRANSACTIONS.DEAL_ID.eq(dealId))
                .and(TON_TRANSACTIONS.DIRECTION.eq("IN"))
                .orderBy(TON_TRANSACTIONS.ID.desc())
                .limit(1)
                .fetchOne(TON_TRANSACTIONS.STATUS);
    }

    private void awaitOutboxDelivered(UUID dealId, String topic, String eventType) {
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    Integer count = dsl.selectCount()
                            .from(NOTIFICATION_OUTBOX)
                            .where(NOTIFICATION_OUTBOX.DEAL_ID.eq(dealId))
                            .and(NOTIFICATION_OUTBOX.TOPIC.eq(topic))
                            .and(NOTIFICATION_OUTBOX.STATUS.eq("DELIVERED"))
                            .and(DSL.condition(
                                    "coalesce(payload ->> 'eventType', payload ->> 'event_type') = {0}",
                                    eventType))
                            .fetchOne(0, int.class);
                    var topicSnapshot = dsl.select(
                                    NOTIFICATION_OUTBOX.STATUS,
                                    DSL.field(
                                            "coalesce(payload ->> 'eventType', payload ->> 'event_type')",
                                            String.class).as("event_type"))
                            .from(NOTIFICATION_OUTBOX)
                            .where(NOTIFICATION_OUTBOX.DEAL_ID.eq(dealId))
                            .and(NOTIFICATION_OUTBOX.TOPIC.eq(topic))
                            .orderBy(NOTIFICATION_OUTBOX.ID.asc())
                            .fetch()
                            .map(r -> r.get("status", String.class)
                                    + ":" + r.get("event_type", String.class));
                    var allTopicsSnapshot = dsl.select(
                                    NOTIFICATION_OUTBOX.TOPIC,
                                    NOTIFICATION_OUTBOX.STATUS,
                                    DSL.field(
                                            "coalesce(payload ->> 'eventType', payload ->> 'event_type')",
                                            String.class).as("event_type"))
                            .from(NOTIFICATION_OUTBOX)
                            .where(NOTIFICATION_OUTBOX.DEAL_ID.eq(dealId))
                            .orderBy(NOTIFICATION_OUTBOX.ID.asc())
                            .fetch()
                            .map(r -> r.get(NOTIFICATION_OUTBOX.TOPIC)
                                    + ":" + r.get(NOTIFICATION_OUTBOX.STATUS)
                                    + ":" + r.get("event_type", String.class));
                    assertThat(count).isNotNull();
                    assertThat(count)
                            .as("outbox delivered: deal=%s topic=%s event=%s topicSnapshot=%s allSnapshot=%s",
                                    dealId,
                                    topic,
                                    eventType,
                                    topicSnapshot,
                                    allTopicsSnapshot)
                            .isGreaterThan(0);
                });
    }

    @TestConfiguration
    static class StubConfig {

        @Bean(name = "tonBlockchainPort")
        StubTonBlockchainPort tonBlockchainPort() {
            return new StubTonBlockchainPort();
        }

        @Bean(name = "tonWalletService")
        StubTonWalletPort tonWalletService() {
            return new StubTonWalletPort();
        }

        @Bean
        TonProperties.Deposit tonDepositProperties() {
            return new TonProperties.Deposit(
                    Duration.ofMillis(150),
                    Duration.ofMinutes(30),
                    100,
                    5);
        }

        @Bean
        ConfirmationPolicyService confirmationPolicyService() {
            return new ConfirmationPolicyService(new TonProperties.Confirmation());
        }

        @Bean
        @Primary
        StubNotificationPort notificationPort() {
            return new StubNotificationPort();
        }

        @Bean
        ReconciliationResultPort reconciliationResultPort() {
            return envelope -> {
                // no-op for workflow E2E; reconciliation is out of scope
            };
        }
    }

    @SpringBootConfiguration
    @EnableKafka
    @EnableAutoConfiguration
    @ComponentScan(
            basePackages = "com.advertmarket",
            excludeFilters = {
                    @ComponentScan.Filter(
                            type = FilterType.REGEX,
                            pattern = "com\\.advertmarket\\.integration\\..*"),
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = AdvertMarketApplication.class),
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = SecurityConfig.class)
                    ,
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = TonConfig.class)
                    ,
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = TonResilienceConfig.class)
                    ,
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = WorkerEventPortStubConfig.class)
            })
    static class TestApplication {
    }

    static final class StubTonBlockchainPort implements TonBlockchainPort {

        private final Map<String, List<TonTransactionInfo>> inboundByAddress =
                new ConcurrentHashMap<>();
        private final AtomicLong masterSeqno = new AtomicLong(5_000L);
        private final AtomicLong bocCounter = new AtomicLong(0L);

        void putInbound(String address, List<TonTransactionInfo> txs) {
            var sorted = txs.stream()
                    .sorted(Comparator.comparingLong(TonTransactionInfo::lt))
                    .toList();
            inboundByAddress.put(address, sorted);
        }

        void reset() {
            inboundByAddress.clear();
            masterSeqno.set(5_000L);
            bocCounter.set(0L);
        }

        @Override
        public List<TonTransactionInfo> getTransactions(String address, int limit) {
            var txs = inboundByAddress.getOrDefault(address, List.of());
            if (txs.size() <= limit) {
                return txs;
            }
            return txs.subList(0, limit);
        }

        @Override
        public String sendBoc(String base64Boc) {
            return "stub-boc-" + bocCounter.incrementAndGet();
        }

        @Override
        public long getMasterchainSeqno() {
            return masterSeqno.incrementAndGet();
        }

        @Override
        public long getAddressBalance(String address) {
            return 0L;
        }

        @Override
        public long getSeqno(String address) {
            return 0L;
        }

        @Override
        public long estimateFee(String address, String base64Body) {
            return 1_000_000L;
        }
    }

    static final class StubTonWalletPort implements TonWalletPort {

        private final AtomicInteger subwalletCounter = new AtomicInteger(1_000);
        private final AtomicInteger txCounter = new AtomicInteger(0);

        void reset() {
            subwalletCounter.set(1_000);
            txCounter.set(0);
        }

        @Override
        public DepositAddressInfo generateDepositAddress(DealId dealId) {
            int subwalletId = subwalletCounter.incrementAndGet();
            String address = "UQ_stub_deposit_" + subwalletId;
            return new DepositAddressInfo(address, subwalletId);
        }

        @Override
        public String submitTransaction(
                int subwalletId,
                String destinationAddress,
                long amountNano) {
            return "tx-out-" + subwalletId + "-" + txCounter.incrementAndGet();
        }
    }

    static final class StubNotificationPort implements NotificationPort {

        private final List<NotificationRequest> requests =
                new CopyOnWriteArrayList<>();

        void reset() {
            requests.clear();
        }

        List<NotificationRequest> requests() {
            return List.copyOf(requests);
        }

        @Override
        public boolean send(NotificationRequest request) {
            requests.add(request);
            return true;
        }
    }
}
