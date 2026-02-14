package com.advertmarket.app.error;

import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.InsufficientBalanceException;
import com.advertmarket.shared.exception.InvalidStateTransitionException;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.logging.MdcKeys;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler producing RFC 9457 Problem Detail.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler
        extends ResponseEntityExceptionHandler {

    private final LocalizationService localization;
    private final MetricsFacade metrics;

    /** Handles entity-not-found exceptions. */
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(
            EntityNotFoundException ex, Locale locale) {
        var code = ErrorCode.resolve(ex.getErrorCode());
        int status = code != null
                ? code.httpStatus() : HttpStatus.NOT_FOUND.value();
        return buildProblem(ex, code, status, locale);
    }

    /** Handles invalid state transition exceptions. */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidTransition(
            InvalidStateTransitionException ex, Locale locale) {
        var code = ErrorCode.INVALID_STATE_TRANSITION;
        return buildProblem(ex, code, code.httpStatus(), locale);
    }

    /** Handles insufficient balance exceptions. */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(
            InsufficientBalanceException ex, Locale locale) {
        var code = ErrorCode.INSUFFICIENT_BALANCE;
        return buildProblem(ex, code, code.httpStatus(), locale);
    }

    /** Handles generic domain exceptions. */
    @SuppressWarnings("fenum:argument")
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(
            DomainException ex, Locale locale) {
        log.warn("Domain exception: code={}, message={}",
                ex.getErrorCode(), ex.getMessage(), ex);
        var code = ErrorCode.resolve(ex.getErrorCode());
        int status = code != null
                ? code.httpStatus()
                : HttpStatus.INTERNAL_SERVER_ERROR.value();
        return buildProblem(ex, code, status, locale);
    }

    /** Safety net for access denied propagated through @PreAuthorize. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(
            AccessDeniedException ex, Locale locale) {
        log.debug("Access denied in controller advice: {}",
                ex.getMessage());
        metrics.incrementCounter(
                MetricNames.AUTH_ACCESS_DENIED);
        var code = ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS;
        var problem = ProblemDetail.forStatus(code.httpStatus());
        problem.setType(URI.create(code.typeUri()));
        problem.setTitle(localization.msg(
                code.titleKey(), locale));
        problem.setDetail(localization.msg(
                code.detailKey(), locale));
        addCommonProperties(problem, code.name());
        return problem;
    }

    /** Handles all unexpected exceptions. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleFallback(
            Exception ex, Locale locale) {
        log.error("Unhandled exception", ex);
        metrics.incrementCounter(
                MetricNames.ERRORS_UNHANDLED,
                "type", ex.getClass().getSimpleName());
        var code = ErrorCode.INTERNAL_ERROR;
        var problem = ProblemDetail.forStatus(code.httpStatus());
        problem.setType(URI.create(code.typeUri()));
        problem.setTitle(localization.msg(
                code.titleKey(), locale));
        problem.setDetail(localization.msg(
                code.detailKey(), locale));
        addCommonProperties(problem, code.name());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var locale = LocaleContextHolder.getLocale();
        var code = ErrorCode.VALIDATION_FAILED;
        var problem = ProblemDetail.forStatus(code.httpStatus());
        problem.setType(URI.create(code.typeUri()));
        problem.setTitle(localization.msg(
                code.titleKey(), locale));
        problem.setDetail(ex.getBindingResult()
                .getFieldErrors().stream()
                .map(e -> e.getField() + ": "
                        + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(localization.msg(
                        code.detailKey(), locale)));
        addCommonProperties(problem, code.name());
        return ResponseEntity.status(code.httpStatus())
                .body(problem);
    }

    @Override
    protected ResponseEntity<Object>
            handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var locale = LocaleContextHolder.getLocale();
        var code = ErrorCode.VALIDATION_FAILED;
        var problem = ProblemDetail.forStatus(
                HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(code.typeUri()));
        problem.setTitle(localization.msg(
                code.titleKey(), locale));
        problem.setDetail(localization.msg(
                code.detailKey(), locale));
        addCommonProperties(problem, code.name());
        return ResponseEntity.badRequest().body(problem);
    }

    @SuppressWarnings("fenum:argument")
    private ProblemDetail buildProblem(
            DomainException ex,
            @Nullable ErrorCode code,
            int status,
            Locale locale) {
        metrics.incrementCounter(
                MetricNames.ERRORS_DOMAIN,
                "code", ex.getErrorCode());

        var problem = ProblemDetail.forStatus(status);
        if (code != null) {
            problem.setType(URI.create(code.typeUri()));
            problem.setTitle(localization.msg(
                    code.titleKey(), locale));
            problem.setDetail(localization.msg(
                    code.detailKey(), locale));
        } else {
            problem.setTitle(ex.getErrorCode());
            problem.setDetail(ex.getMessage());
        }
        addCommonProperties(problem, ex.getErrorCode());
        return problem;
    }

    @SuppressWarnings("fenum:argument")
    private void addCommonProperties(
            ProblemDetail problem, String errorCode) {
        problem.setProperty("error_code", errorCode);
        problem.setProperty("timestamp",
                Instant.now().toString());
        var correlationId = MDC.get(
                MdcKeys.CORRELATION_ID);
        if (correlationId != null) {
            problem.setProperty("correlation_id",
                    correlationId);
        }
    }
}
