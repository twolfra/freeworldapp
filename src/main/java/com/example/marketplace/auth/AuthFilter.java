package com.example.marketplace.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private final SessionRepository sessionRepo;

    public AuthFilter(SessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String method = req.getMethod();
        String path   = req.getRequestURI();

        // OPTIONS always passes through
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(req, res);
            return;
        }

        // GETs are public unless they access private message data
        if ("GET".equalsIgnoreCase(method) && !isSensitivePath(path)) {
            chain.doFilter(req, res);
            return;
        }

        // Public write endpoints — no token required
        if ("POST".equalsIgnoreCase(method) &&
                (path.equals("/api/auth/login")
              || path.equals("/api/auth/logout")
              || path.equals("/api/auth/resend-verification")
              || path.equals("/api/users")
              || path.equals("/api/contact"))) {
            chain.doFilter(req, res);
            return;
        }

        // All other requests require a valid, non-expired session token
        String token = req.getHeader("X-Session-Token");
        if (token == null || token.isBlank()) {
            reject(res, "Authentication required.");
            return;
        }

        var sessionOpt = sessionRepo.findByTokenWithUser(token);
        if (sessionOpt.isEmpty()) {
            reject(res, "Invalid or expired session.");
            return;
        }

        Session session = sessionOpt.get();
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(Instant.now())) {
            sessionRepo.delete(session);
            reject(res, "Session expired. Please sign in again.");
            return;
        }

        // A blocked account's live sessions are rejected immediately, not just at next login.
        if (session.getUser().isBlocked()) {
            reject(res, "This account has been blocked.");
            return;
        }

        req.setAttribute("authenticatedUserId", session.getUser().getId());
        chain.doFilter(req, res);
    }

    // GET endpoints that require authentication (user-private data)
    private boolean isSensitivePath(String path) {
        return path.startsWith("/api/messages/conversation")
            || path.startsWith("/api/messages/unread-count")
            || path.startsWith("/api/likes")
            || path.equals("/api/subscriptions/feed")
            || path.startsWith("/api/admin");
    }

    private void reject(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
