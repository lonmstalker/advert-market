package com.advertmarket.deal.service;

import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.config.DealTimeoutProperties;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls for deals that have exceeded their deadline and transitions
 * them to EXPIRED status.
 *
 * <p>Uses distributed locking to prevent concurrent processing
 * across multiple instances. Funded deals emit a refund indication
 * via the transition reason.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DealTimeoutScheduler {

    private static final String LOCK_KEY = "scheduler:deal-timeout";
    private static final String TIMEOUT_REASON = "Deal expired due to timeout";

    private final DealRepository dealRepository;
    private final DealTransitionService dealTransitionService;
    private final DistributedLockPort lockPort;
    private final MetricsFacade metrics;
    private final DealTimeoutProperties props;

    @Scheduled(fixedDelayString = "${app.deal.timeout.poll-interval:60000}")
    public void processExpiredDeals() {
        var token = lockPort.tryLock(LOCK_KEY, props.lockTtl());
        if (token.isEmpty()) {
            log.debug("Could not acquire timeout scheduler lock, skipping");
            return;
        }

        try {
            doProcessExpiredDeals();
        } finally {
            lockPort.unlock(LOCK_KEY, token.get());
        }
    }

    private void doProcessExpiredDeals() {
        var expiredDeals = dealRepository.findExpiredDeals(props.batchSize());
        if (expiredDeals.isEmpty()) {
            return;
        }

        log.info("Processing {} expired deals", expiredDeals.size());

        for (var deal : expiredDeals) {
            processOne(deal);
        }
    }

    private void processOne(DealRecord deal) {
        var dealId = DealId.of(deal.id());
        try {
            var command = new DealTransitionCommand(
                    dealId,
                    DealStatus.EXPIRED,
                    0L,
                    ActorType.SYSTEM,
                    TIMEOUT_REASON);

            dealTransitionService.transition(command);

            metrics.incrementCounter(MetricNames.DEAL_TIMEOUT_PROCESSED,
                    "from_status", deal.status().name());

            if (deal.status().isFunded()) {
                metrics.incrementCounter(MetricNames.DEAL_TIMEOUT_REFUND_EMITTED,
                        "from_status", deal.status().name());
            }

            log.info("Expired deal={} from status={}", dealId, deal.status());
        } catch (Exception ex) {
            log.warn("Failed to expire deal={} from status={}: {}",
                    dealId, deal.status(), ex.getMessage());
        }
    }
}
