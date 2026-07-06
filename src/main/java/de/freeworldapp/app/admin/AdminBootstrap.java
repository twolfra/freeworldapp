package de.freeworldapp.app.admin;

import de.freeworldapp.app.user.Role;
import de.freeworldapp.app.user.User;
import de.freeworldapp.app.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Promotes the accounts listed in ADMIN_EMAILS (comma-separated) to the ADMIN
 * role on startup. Idempotent — a matching user that's already ADMIN is left
 * untouched. This is how the first admin is bootstrapped without manual DB edits.
 */
@Component
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepo;
    private final String adminEmails;

    public AdminBootstrap(UserRepository userRepo,
                          @Value("${app.admin.emails:}") String adminEmails) {
        this.userRepo = userRepo;
        this.adminEmails = adminEmails;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void promoteAdmins() {
        if (adminEmails == null || adminEmails.isBlank()) return;

        Set<String> emails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        for (String email : emails) {
            userRepo.findByEmailIgnoreCase(email).ifPresentOrElse(u -> {
                if (u.getRole() != Role.ADMIN) {
                    u.setRole(Role.ADMIN);
                    userRepo.save(u);
                    log.info("Admin bootstrap: promoted '{}' ({}) to ADMIN", u.getUsername(), email);
                }
            }, () -> log.warn("Admin bootstrap: no user found for ADMIN_EMAILS entry '{}'", email));
        }
    }
}
