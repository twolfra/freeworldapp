package de.freeworldapp.app.message;

import de.freeworldapp.app.email.EmailService;
import de.freeworldapp.app.notification.Notification;
import de.freeworldapp.app.notification.NotificationService;
import de.freeworldapp.app.user.User;
import de.freeworldapp.app.user.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sends an email to a message recipient — but only when it's actually useful:
 * the recipient is not currently connected (they'd see the message live),
 * has notifications enabled, and is a verified, non-blocked account.
 *
 * Runs asynchronously so the message send path (WebSocket / HTTP) never blocks
 * on the outbound Brevo HTTP call.
 */
@Service
public class MessageNotificationService {

    private final UserRepository userRepo;
    private final EmailService emailService;
    private final ChatWebSocketHandler wsHandler;
    private final NotificationService notificationService;

    public MessageNotificationService(UserRepository userRepo, EmailService emailService,
                                      @Lazy ChatWebSocketHandler wsHandler,
                                      NotificationService notificationService) {
        this.userRepo = userRepo;
        this.emailService = emailService;
        this.wsHandler = wsHandler;
        this.notificationService = notificationService;
    }

    @Async
    public void notifyNewMessage(UUID recipientId, UUID senderId, String content) {
        // The recipient is connected (any open tab) — they'll see it in real time.
        if (wsHandler.isOnline(recipientId)) return;

        User recipient = userRepo.findById(recipientId).orElse(null);
        User sender    = userRepo.findById(senderId).orElse(null);
        if (recipient == null || sender == null) return;

        // In-app notification for the offline recipient — independent of the
        // email opt-out, which only governs emails.
        notificationService.notify(recipientId, Notification.Type.NEW_MESSAGE, java.util.Map.of(
                "fromId", senderId.toString(),
                "fromUsername", sender.getUsername()));

        if (!recipient.isNotifyOnMessage()) return;   // opted out
        if (!recipient.isEmailVerified()) return;      // unverified / possibly wrong address
        if (recipient.isBlocked()) return;             // blocked account

        emailService.sendNewMessageEmail(
                recipient.getEmail(),
                recipient.getUsername(),
                sender.getUsername(),
                content,
                senderId.toString(),
                recipient.getUnsubscribeToken(),
                recipient.getLanguage());
    }
}
