package com.corporate.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter for anonymous endpoints. Buckets are held in-memory so
 * limits are per-process — good enough for single-instance dev/staging, and
 * consciously the smallest hammer that keeps register/login/enquiry endpoints
 * from being spam-flooded. Replace with a shared store (Redis) if we scale out.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /**
     * A rate-limited route. {@code prefix=false} matches the request URI exactly;
     * {@code prefix=true} matches when the URI starts with {@code path} (for routes
     * carrying a variable segment, e.g. /api/quotes/{token}/accept).
     */
    private record Route(String method, String path, boolean prefix, Bandwidth limit) {}

    private static final Route[] ROUTES = new Route[] {
            new Route("POST", "/api/auth/register", false,
                    Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build()),
            new Route("POST", "/api/auth/login", false,
                    Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build()),
            // Forgot-password: bound tight to stop mail-bombing and enumeration probing.
            new Route("POST", "/api/auth/forgot-password", false,
                    Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build()),
            // Reset-password: token is a 32-hex opaque value (infeasible to guess);
            // this bound is defence-in-depth against brute-forcing.
            new Route("POST", "/api/auth/reset-password", false,
                    Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build()),
            new Route("POST", "/api/enquiries", false,
                    Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build()),
            // Each agent chat costs real Claude tokens — keep this tight.
            new Route("POST", "/api/agent/chat", false,
                    Bandwidth.builder().capacity(8).refillGreedy(8, Duration.ofMinutes(1)).build()),
            // Bulk-order estimate is a DB-pricing call — cheap, but bound it so a
            // scripted paste can't spam the pricing query.
            new Route("POST", "/api/bulk-order/estimate", false,
                    Bandwidth.builder().capacity(20).refillGreedy(20, Duration.ofMinutes(1)).build()),
            // Anonymous quote accept/decline — bound so a leaked token can't be
            // hammered. Matches /api/quotes/{token}/accept and .../decline.
            new Route("POST", "/api/quotes/", true,
                    Bandwidth.builder().capacity(20).refillGreedy(20, Duration.ofMinutes(1)).build()),
    };

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final boolean trustForwardedFor;

    public RateLimitFilter(@Value("${app.ratelimit.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }

    /** Clears all buckets. For tests only. */
    public void reset() {
        buckets.clear();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        Route route = matchRoute(req);
        if (route == null) {
            chain.doFilter(req, res);
            return;
        }

        String ip = clientIp(req);
        String key = route.path() + "|" + ip;
        Bucket bucket = buckets.computeIfAbsent(key,
                k -> Bucket.builder().addLimit(route.limit()).build());

        var probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(req, res);
            return;
        }

        long retryAfterSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("""
                {"timestamp":"%s","status":429,"error":"Too Many Requests","message":"Rate limit exceeded, retry in %d seconds"}
                """.formatted(Instant.now(), retryAfterSeconds));
    }

    private Route matchRoute(HttpServletRequest req) {
        for (Route r : ROUTES) {
            if (!r.method().equals(req.getMethod())) continue;
            String uri = req.getRequestURI();
            if (r.prefix() ? uri.startsWith(r.path()) : r.path().equals(uri)) {
                return r;
            }
        }
        return null;
    }

    private String clientIp(HttpServletRequest req) {
        if (trustForwardedFor) {
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
            }
        }
        return req.getRemoteAddr();
    }
}
