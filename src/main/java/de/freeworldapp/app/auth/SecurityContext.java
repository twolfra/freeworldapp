package de.freeworldapp.app.auth;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

public class SecurityContext {

    public static UUID authenticatedId() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        return (UUID) attrs.getRequest().getAttribute("authenticatedUserId");
    }
}
