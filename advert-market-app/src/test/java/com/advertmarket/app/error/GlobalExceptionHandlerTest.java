package com.advertmarket.app.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.InsufficientBalanceException;
import com.advertmarket.shared.exception.InvalidStateTransitionException;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.Money;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private LocalizationService localization;
    private MetricsFacade metrics;
    private GlobalExceptionHandler handler;
    private final Locale locale = Locale.ENGLISH;

    @BeforeEach
    void setUp() {
        localization = mock(LocalizationService.class);
        metrics = mock(MetricsFacade.class);
        handler = new GlobalExceptionHandler(
                localization, metrics);

        when(localization.msg(anyString(), eq(locale)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("EntityNotFoundException returns 404")
    void entityNotFound_returns404() {
        var ex = new EntityNotFoundException("Deal", "abc-123");

        var problem = handler.handleEntityNotFound(ex, locale);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getProperties())
                .containsEntry("error_code", "DEAL_NOT_FOUND");
    }

    @Test
    @DisplayName("InvalidStateTransition returns 409")
    void invalidTransition_returns409() {
        var ex = new InvalidStateTransitionException(
                "Deal", "DRAFT", "PUBLISHED");

        var problem = handler.handleInvalidTransition(
                ex, locale);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties())
                .containsEntry("error_code",
                        "INVALID_STATE_TRANSITION");
    }

    @Test
    @DisplayName("InsufficientBalance returns 422")
    void insufficientBalance_returns422() {
        var ex = new InsufficientBalanceException(
                AccountId.escrow(DealId.generate()),
                Money.ofNano(1000),
                Money.ofNano(500));

        var problem = handler.handleInsufficientBalance(
                ex, locale);

        assertThat(problem.getStatus()).isEqualTo(422);
        assertThat(problem.getProperties())
                .containsEntry("error_code",
                        "INSUFFICIENT_BALANCE");
    }

    @Test
    @DisplayName("DomainException with known code uses its status")
    void domainException_knownCode_usesStatus() {
        var ex = new DomainException(
                "RATE_LIMIT_EXCEEDED", "Too many requests");

        var problem = handler.handleDomainException(
                ex, locale);

        assertThat(problem.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("DomainException with unknown code returns 500")
    void domainException_unknownCode_returns500() {
        var ex = new DomainException(
                "CUSTOM_ERROR", "Something custom");

        var problem = handler.handleDomainException(
                ex, locale);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getTitle()).isEqualTo("CUSTOM_ERROR");
    }

    @Test
    @DisplayName("Fallback handler returns 500")
    void fallback_returns500() {
        var ex = new RuntimeException("unexpected");

        var problem = handler.handleFallback(ex, locale);

        assertThat(problem.getStatus()).isEqualTo(500);
    }

    @Test
    @DisplayName("Fallback handler increments error counter")
    void fallback_incrementsCounter() {
        handler.handleFallback(
                new RuntimeException("oops"), locale);

        verify(metrics).incrementCounter(
                eq("errors.unhandled"),
                eq("type"), eq("RuntimeException"));
    }

    @Test
    @DisplayName("ProblemDetail includes timestamp property")
    void problemDetail_includesTimestamp() {
        var ex = new EntityNotFoundException("Deal", "1");

        var problem = handler.handleEntityNotFound(ex, locale);

        assertThat(problem.getProperties())
                .containsKey("timestamp");
    }
}
