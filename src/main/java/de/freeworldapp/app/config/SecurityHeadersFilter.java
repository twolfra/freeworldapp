package de.freeworldapp.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security response headers for every request (AP 1.3).
 * Set CSP_REPORT_ONLY=true to trial CSP changes without breaking the app;
 * default is enforcing.
 */
@Component
@Order(0)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    // 'unsafe-inline' in style-src is required for React style={} attributes;
    // scripts stay external (Vite build), so script-src inherits 'self'.
    static final String CSP = "default-src 'self'; "
            + "img-src 'self' https://storage.googleapis.com https://*.tile.openstreetmap.org data: blob:; "
            + "connect-src 'self' wss:; "
            + "style-src 'self' 'unsafe-inline'; "
            + "frame-ancestors 'none'; "
            + "base-uri 'self'; "
            + "object-src 'none'";

    private final boolean cspReportOnly;

    public SecurityHeadersFilter(@Value("${app.security.csp-report-only:false}") boolean cspReportOnly) {
        this.cspReportOnly = cspReportOnly;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        res.setHeader(cspReportOnly ? "Content-Security-Policy-Report-Only" : "Content-Security-Policy", CSP);
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        res.setHeader("Permissions-Policy", "camera=(), geolocation=(self), microphone=()");

        // Cloud Run terminates TLS in front of the container → check the forwarded proto too.
        boolean https = req.isSecure() || "https".equalsIgnoreCase(req.getHeader("X-Forwarded-Proto"));
        if (https) {
            res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        chain.doFilter(req, res);
    }
}
