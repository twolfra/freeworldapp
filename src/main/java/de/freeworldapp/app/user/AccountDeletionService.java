package de.freeworldapp.app.user;

import de.freeworldapp.app.auth.PasswordResetTokenRepository;
import de.freeworldapp.app.auth.SessionRepository;
import de.freeworldapp.app.image.StorageService;
import de.freeworldapp.app.like.LikeRepository;
import de.freeworldapp.app.notification.NotificationRepository;
import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.postimage.PostImage;
import de.freeworldapp.app.postimage.PostImageService;
import de.freeworldapp.app.report.Report;
import de.freeworldapp.app.report.ReportRepository;
import de.freeworldapp.app.request.RequestRepository;
import de.freeworldapp.app.subscription.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * DSGVO self-deletion (AP 4.4): the account is ANONYMIZED, not removed.
 * Posts, sessions, tokens, subscriptions, likes and notifications are deleted;
 * messages stay so the other side keeps its conversation — displayed as
 * "Deleted account" (the users row is kept, PII scrubbed, deleted=true).
 * Documented decision; the Datenschutzerklärung describes this behaviour.
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;
    private final PasswordResetTokenRepository resetRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final LikeRepository likeRepo;
    private final NotificationRepository notificationRepo;
    private final ReportRepository reportRepo;
    private final OfferRepository offerRepo;
    private final RequestRepository requestRepo;
    private final StorageService storage;
    private final PostImageService postImages;

    public AccountDeletionService(UserRepository userRepo, SessionRepository sessionRepo,
                                  PasswordResetTokenRepository resetRepo,
                                  SubscriptionRepository subscriptionRepo, LikeRepository likeRepo,
                                  NotificationRepository notificationRepo, ReportRepository reportRepo,
                                  OfferRepository offerRepo, RequestRepository requestRepo,
                                  StorageService storage, PostImageService postImages) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.resetRepo = resetRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.likeRepo = likeRepo;
        this.notificationRepo = notificationRepo;
        this.reportRepo = reportRepo;
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
        this.storage = storage;
        this.postImages = postImages;
    }

    @Transactional
    public void anonymize(User u) {
        UUID id = u.getId();

        // posts (incl. images/galleries/likes/reports referencing them)
        offerRepo.findByOfferedBy_Id(id).forEach(o -> {
            storage.delete(o.getImageUrl());
            postImages.deleteAll(PostImage.TargetType.OFFER, o.getId());
            likeRepo.deleteAllByTargetTypeAndTargetId(de.freeworldapp.app.like.Like.TargetType.OFFER, o.getId());
            reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.OFFER, o.getId());
        });
        requestRepo.findByRequestedBy_Id(id).forEach(r -> {
            storage.delete(r.getImageUrl());
            postImages.deleteAll(PostImage.TargetType.REQUEST, r.getId());
            likeRepo.deleteAllByTargetTypeAndTargetId(de.freeworldapp.app.like.Like.TargetType.REQUEST, r.getId());
            reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.REQUEST, r.getId());
        });
        offerRepo.deleteAll(offerRepo.findByOfferedBy_Id(id));
        requestRepo.deleteAll(requestRepo.findByRequestedBy_Id(id));

        sessionRepo.deleteByUser_Id(id);
        resetRepo.deleteByUser_Id(id);
        subscriptionRepo.deleteAllInvolvingUser(id);
        likeRepo.deleteAllByUserId(id);
        notificationRepo.deleteByUser_Id(id);
        reportRepo.deleteAllByReporterId(id);

        // scrub PII; messages and thanks remain, shown as "Deleted account"
        if (u.getAvatarUrl() != null) storage.delete(u.getAvatarUrl());
        String suffix = id.toString().substring(0, 8);
        u.setUsername("deleted-" + suffix);
        u.setEmail(id + "@deleted.invalid");
        u.setPasswordHash("{deleted}");   // never matches BCrypt — login impossible
        u.setEmailVerified(false);
        u.setVerificationToken(null);
        u.setVerificationTokenExpiresAt(null);
        u.setNotifyOnMessage(false);
        u.setUnsubscribeToken(null);
        u.setDisplayName(null);
        u.setBio(null);
        u.setAvatarUrl(null);
        u.setPostalCode(null);
        u.setCity(null);
        u.setDeleted(true);
        userRepo.save(u);

        log.info("Account {} anonymized (DSGVO deletion)", id);
    }
}
