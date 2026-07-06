package de.freeworldapp.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation id (AP 4.5): every request gets an X-Request-Id response header
 * and the same id in the logging MDC, so a frontend error toast can be traced
 * to the exact log lines. Honours an incoming X-Request-Id (e.g. from a
 * load balancer) when present and sane.
 */
@Component
@Order(-10)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String incoming = req.getHeader("X-Request-Id");
        String requestId = (incoming != null && incoming.matches("[A-Za-z0-9-]{8,64}"))
                ? incoming
                : UUID.randomUUID().toString().substring(0, 8);

        MDC.put("requestId", requestId);
        res.setHeader("X-Request-Id", requestId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("requestId");
        }
    }
}
