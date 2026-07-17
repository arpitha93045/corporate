package com.corporate.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Assigns a correlation id to every request and exposes it two ways:
 *   - in the SLF4J MDC under "requestId" so every log line for the request
 *     (across all downstream filters, controllers, services) carries it —
 *     see {@code logback-spring.xml}, which prints it in both console and
 *     JSON layouts.
 *   - echoed back in the {@code X-Request-Id} response header so a client or
 *     proxy can quote it when reporting a problem.
 *
 * If the caller (typically a trusted reverse proxy) already sent an
 * {@code X-Request-Id}, we reuse it — but only if it looks sane, so a
 * malicious client can't inject newlines or huge values into our logs.
 * Otherwise we mint a random UUID.
 *
 * Registered at highest precedence (see {@link com.corporate.config.WebObservabilityConfig})
 * so the id is present before Spring Security's chain runs.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    // Accept only compact, log-safe ids from upstream. Anything else is replaced.
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String id = (incoming != null && SAFE_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
