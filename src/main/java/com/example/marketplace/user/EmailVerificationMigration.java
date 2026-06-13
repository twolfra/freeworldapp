package com.example.marketplace.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time migration: users created before email verification was added have
 * emailVerified=false but no verificationToken. Mark them as verified so they
 * don't get locked out.
 */
@Component
public class EmailVerificationMigration {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationMigration.class);

    private final UserRepository userRepo;

    public EmailVerificationMigration(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingUsers() {
        List<User> legacy = userRepo.findAll().stream()
                .filter(u -> !u.isEmailVerified() && u.getVerificationToken() == null)
                .toList();
        if (!legacy.isEmpty()) {
            log.info("Email verification migration: marking {} pre-existing user(s) as verified", legacy.size());
            legacy.forEach(u -> u.setEmailVerified(true));
            userRepo.saveAll(legacy);
        }
    }
}
