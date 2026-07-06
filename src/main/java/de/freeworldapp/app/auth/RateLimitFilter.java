package de.freeworldapp.app.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter for write-heavy endpoints.
 * Limits: 20 requests per IP per minute on /api/auth/login and /api/users (registration),
 *         60 requests per IP per minute on /api/messages.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int WINDOW_SECONDS = 60;
    private static final int LOGIN_LIMIT    = 20;
    private static final int MESSAGE_LIMIT  = 60;

    // IP → deque of request timestamps (epoch millis)
    private final Map<String, Deque<Long>> loginBucket   = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> messageBucket = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String method = req.getMethod();
        String path   = req.getRequestURI();

        if ("POST".equalsIgnoreCase(method) &&
                (path.equals("/api/auth/login") || path.equals("/api/users"))) {
            if (!allow(loginBucket, clientIp(req), LOGIN_LIMIT)) {
                reject(res); return;
            }
        } else if ("POST".equalsIgnoreCase(method) && path.equals("/api/messages")) {
            if (!allow(messageBucket, clientIp(req), MESSAGE_LIMIT)) {
                reject(res); return;
            }
        }

        chain.doFilter(req, res);
    }

    private boolean allow(Map<String, Deque<Long>> bucket, String ip, int limit) {
        long now  = Instant.now().toEpochMilli();
        long cutoff = now - WINDOW_SECONDS * 1000L;
        Deque<Long> timestamps = bucket.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) timestamps.pollFirst();
            if (timestamps.size() >= limit) return false;
            timestamps.addLast(now);
        }
        return true;
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
