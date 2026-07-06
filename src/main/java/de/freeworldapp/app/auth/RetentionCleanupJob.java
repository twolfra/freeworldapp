package de.freeworldapp.app.auth;

import de.freeworldapp.app.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * DSGVO retention (AP 4.4): expired sessions and expired verification/reset
 * tokens are purged daily. Runs at 04:10 server time and once at startup
 * would be redundant — the data is only about expired artefacts.
 */
@Component
public class RetentionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupJob.class);

    private final SessionRepository sessionRepo;
    private final PasswordResetTokenRepository resetRepo;
    private final UserRepository userRepo;

    public RetentionCleanupJob(SessionRepository sessionRepo,
                               PasswordResetTokenRepository resetRepo,
                               UserRepository userRepo) {
        this.sessionRepo = sessionRepo;
        this.resetRepo = resetRepo;
        this.userRepo = userRepo;
    }

    @Scheduled(cron = "0 10 4 * * *")
    @Transactional
    public void run() {
        Instant now = Instant.now();
        int sessions = sessionRepo.deleteByExpiresAtBefore(now);
        resetRepo.deleteByExpiresAtBefore(now);
        int verifications = userRepo.clearExpiredVerificationTokens(now);
        log.info("Retention cleanup: {} expired sessions removed, {} expired verification tokens cleared",
                sessions, verifications);
    }
}
