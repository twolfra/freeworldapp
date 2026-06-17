package com.example.marketplace.auth;

import com.example.marketplace.user.Role;
import com.example.marketplace.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Authorization helper for admin-only endpoints. Builds on the request-scoped
 * identity that {@link AuthFilter} stores via {@link SecurityContext}, and
 * loads the caller's role from the database.
 */
@Component
public class AdminGuard {

    private final UserRepository userRepo;

    public AdminGuard(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /** True if the authenticated caller exists and has the ADMIN role. */
    public boolean isAdmin() {
        UUID id = SecurityContext.authenticatedId();
        if (id == null) return false;
        return userRepo.findById(id)
                .map(u -> u.getRole() == Role.ADMIN)
                .orElse(false);
    }
}
