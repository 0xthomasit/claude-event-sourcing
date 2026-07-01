package com.example.order.infrastructure.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that establishes MDC context for every inbound request.
 * <p>
 * Propagates an existing {@code X-Correlation-Id} header if present,
 * or generates a new UUID. This value is attached to every log line
 * and returned in the response header for end-to-end tracing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_CORRELATION_KEY   = "correlationId";
    private static final String MDC_REQUEST_URI_KEY   = "requestUri";
    private static final String MDC_METHOD_KEY        = "httpMethod";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpRequest  = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_KEY,  correlationId);
        MDC.put(MDC_REQUEST_URI_KEY,  httpRequest.getRequestURI());
        MDC.put(MDC_METHOD_KEY,       httpRequest.getMethod());

        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
