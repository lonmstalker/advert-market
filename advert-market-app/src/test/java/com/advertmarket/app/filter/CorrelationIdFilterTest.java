package com.advertmarket.app.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.advertmarket.shared.logging.MdcKeys;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("CorrelationIdFilter")
@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private FilterChain filterChain;

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Should use provided X-Correlation-Id header value")
    void usesProvidedCorrelationId() throws Exception {
        var id = UUID.randomUUID().toString();
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, id);
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isEqualTo(id);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should generate UUID when header is missing")
    void generatesUuidWhenMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        var generated = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(generated).isNotNull();
        assertThat(UUID.fromString(generated)).isNotNull();
    }

    @Test
    @DisplayName("Should generate UUID when header is blank")
    void generatesUuidWhenBlank() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "   ");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        var generated = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(UUID.fromString(generated)).isNotNull();
    }

    @Test
    @DisplayName("Should generate UUID when header is not a valid UUID")
    void generatesUuidWhenInvalid() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "not-a-uuid");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        var generated = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(generated).isNotEqualTo("not-a-uuid");
        assertThat(UUID.fromString(generated)).isNotNull();
    }

    @Test
    @DisplayName("Should set correlation ID in MDC during filter chain execution")
    void setsMdcDuringExecution() throws Exception {
        var id = UUID.randomUUID().toString();
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, id);
        var response = new MockHttpServletResponse();
        var captured = new AtomicReference<String>();

        filter.doFilterInternal(request, response,
                (req, res) -> captured.set(
                        MDC.get(MdcKeys.CORRELATION_ID)));

        assertThat(captured.get()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should clear MDC after filter chain completes")
    void clearsMdcAfterExecution() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER,
                UUID.randomUUID().toString());
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("Should clear MDC even when filter chain throws")
    void clearsMdcOnException() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER,
                UUID.randomUUID().toString());
        var response = new MockHttpServletResponse();

        try {
            filter.doFilterInternal(request, response,
                    (req, res) -> {
                        throw new RuntimeException("test");
                    });
        } catch (RuntimeException ignored) {
            // expected
        }

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull();
    }
}
