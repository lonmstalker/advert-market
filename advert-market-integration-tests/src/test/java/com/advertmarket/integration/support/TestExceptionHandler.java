package com.advertmarket.integration.support;

import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Shared exception handler for HTTP integration tests.
 *
 * <p>Produces ProblemDetail responses consistent with the production
 * error handler. Use via {@code @Import(TestExceptionHandler.class)}.
 */
@RestControllerAdvice
public class TestExceptionHandler
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        var code = ErrorCode.resolve(ex.getErrorCode());
        int status = code != null ? code.httpStatus() : 500;
        var pd = ProblemDetail.forStatus(status);
        if (code != null) {
            pd.setType(URI.create(code.typeUri()));
            pd.setTitle(code.name());
        }
        pd.setDetail(ex.getMessage());
        addProps(pd, ex.getErrorCode());
        return pd;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return handleDomain(ex);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(
                "urn:problem-type:validation-failed"));
        pd.setTitle("Validation failed");
        pd.setDetail(ex.getBindingResult()
                .getFieldErrors().stream()
                .map(e -> e.getField() + ": "
                        + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed"));
        addProps(pd, "VALIDATION_FAILED");
        return ResponseEntity.badRequest().body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(
                "urn:problem-type:validation-failed"));
        pd.setTitle("Malformed body");
        pd.setDetail("Missing or invalid JSON");
        addProps(pd, "VALIDATION_FAILED");
        return ResponseEntity.badRequest().body(pd);
    }

    private void addProps(ProblemDetail pd, String errorCode) {
        pd.setProperty("error_code", errorCode);
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("correlation_id",
                UUID.randomUUID().toString());
    }
}
