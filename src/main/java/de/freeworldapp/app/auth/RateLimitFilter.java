package de.freeworldapp.app.auth;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP token-bucket rate limiting (Bucket4j) for abuse-prone endpoints.
 * State is in-memory and per-instance — swap for a Redis-backed store when
 * running more than one backend node.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String method, String path, int capacity, Duration period) {}

    private static final List<Rule> RULES = List.of(
            new Rule("POST", "/api/auth/login",               20, Duration.ofMinutes(1)),
            new Rule("POST", "/api/users",                    20, Duration.ofMinutes(1)),
            new Rule("POST", "/api/messages",                 60, Duration.ofMinutes(1)),
            new Rule("POST", "/api/images",                   10, Duration.ofMinutes(1)),
            new Rule("POST", "/api/reports",                   5, Duration.ofMinutes(1)),
            new Rule("POST", "/api/auth/resend-verification",  3, Duration.ofMinutes(15)),
            new Rule("POST", "/api/auth/forgot-password",      5, Duration.ofMinutes(15)),
            new Rule("POST", "/api/contact",                   3, Duration.ofMinutes(15))
    );

    // "path|ip" → bucket
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        Rule rule = match(req.getMethod(), req.getRequestURI());
        if (rule != null) {
            String key = rule.path() + "|" + clientIp(req);
            Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(rule));
            if (!bucket.tryConsume(1)) {
                reject(res);
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private Rule match(String method, String path) {
        for (Rule r : RULES) {
            if (r.method().equalsIgnoreCase(method) && r.path().equals(path)) return r;
        }
        return null;
    }

    private Bucket newBucket(Rule rule) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(rule.capacity())
                        .refillGreedy(rule.capacity(), rule.period()))
                .build();
    }

    private String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }

    private void reject(HttpServletResponse res) throws IOException {
        res.setStatus(429);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"Too many requests. Please slow down.\"}");
    }
}
