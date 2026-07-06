package de.freeworldapp.app.subscription;

import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.request.RequestRepository;
import de.freeworldapp.app.subscription.dto.SubscriptionDtos;
import de.freeworldapp.app.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionRepository subRepo;
    private final UserRepository userRepo;
    private final OfferRepository offerRepo;
    private final RequestRepository requestRepo;

    public SubscriptionController(SubscriptionRepository subRepo, UserRepository userRepo,
                                  OfferRepository offerRepo, RequestRepository requestRepo) {
        this.subRepo = subRepo;
        this.userRepo = userRepo;
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
    }

    @PostMapping
    public ResponseEntity<?> subscribe(@Valid @RequestBody SubscriptionDtos.Create in) {
        UUID subscriberId = SecurityContext.authenticatedId();
        UUID subscribedToId;
        try {
            subscribedToId = UUID.fromString(in.subscribedToId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }
        if (subscriberId.equals(subscribedToId))
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot subscribe to yourself."));
        if (subRepo.existsBySubscriber_IdAndSubscribedTo_Id(subscriberId, subscribedToId))
            return ResponseEntity.status(409).body(Map.of("error", "Already subscribed."));

        var subscriber   = userRepo.findById(subscriberId);
        var subscribedTo = userRepo.findById(subscribedToId);
        if (subscriber.isEmpty())   return ResponseEntity.badRequest().body(Map.of("error", "Subscriber not found."));
        if (subscribedTo.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "User not found."));

        Subscription s = new Subscription();
        s.setSubscriber(subscriber.get());
        s.setSubscribedTo(subscribedTo.get());
        Subscription saved = subRepo.save(s);
        return ResponseEntity.created(URI.create("/api/subscriptions/" + saved.getId()))
                .body(toResponse(saved));
    }

    @DeleteMapping
    public ResponseEntity<?> unsubscribe(@RequestParam String subscriberId,
                                         @RequestParam String subscribedToId) {
        UUID sid = SecurityContext.authenticatedId();
        UUID tid;
        try { tid = UUID.fromString(subscribedToId); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }
        return subRepo.findBySubscriber_IdAndSubscribedTo_Id(sid, tid)
                .map(s -> { subRepo.delete(s); return ResponseEntity.noContent().<Void>build(); })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam String subscriberId) {
        UUID sid;
        try { sid = UUID.fromString(subscriberId); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }
        List<SubscriptionDtos.Response> result = subRepo.findBySubscriber_Id(sid).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam String subscriberId,
                                   @RequestParam String subscribedToId) {
        UUID sid, tid;
        try { sid = UUID.fromString(subscriberId); tid = UUID.fromString(subscribedToId); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }
        var resp = new SubscriptionDtos.CheckResponse();
        resp.subscribed = subRepo.existsBySubscriber_IdAndSubscribedTo_Id(sid, tid);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/feed")
    public ResponseEntity<?> feed(@RequestParam String subscriberId) {
        UUID sid;
        try { sid = UUID.fromString(subscriberId); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }

        UUID callerId = SecurityContext.authenticatedId();
        if (!callerId.equals(sid))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied."));

        List<UUID> followedIds = subRepo.findBySubscriber_Id(sid).stream()
                .map(s -> s.getSubscribedTo().getId())
                .collect(Collectors.toList());

        if (followedIds.isEmpty()) return ResponseEntity.ok(List.of());

        List<SubscriptionDtos.FeedItem> items = new ArrayList<>();

        offerRepo.findByOfferedBy_IdIn(followedIds).stream()
                .filter(o -> !o.getOfferedBy().isBlocked()).forEach(o -> {
            var item = new SubscriptionDtos.FeedItem();
            item.type            = "offer";
            item.id              = o.getId().toString();
            item.title           = o.getTitle();
            item.description     = o.getDescription();
            item.region          = o.getRegion();
            item.category        = o.getCategory();
            item.quantity        = o.getQuantity();
            item.authorId        = o.getOfferedBy().getId().toString();
            item.authorUsername  = o.getOfferedBy().getUsername();
            item.createdAt       = DateTimeFormatter.ISO_INSTANT.format(o.getCreatedAt());
            items.add(item);
        });

        requestRepo.findByRequestedBy_IdIn(followedIds).stream()
                .filter(r -> !r.getRequestedBy().isBlocked()).forEach(r -> {
            var item = new SubscriptionDtos.FeedItem();
            item.type            = "request";
            item.id              = r.getId().toString();
            item.title           = r.getTitle();
            item.description     = r.getDescription();
            item.region          = r.getRegion();
            item.category        = r.getCategory();
            item.quantity        = r.getQuantity();
            item.authorId        = r.getRequestedBy().getId().toString();
            item.authorUsername  = r.getRequestedBy().getUsername();
            item.createdAt       = DateTimeFormatter.ISO_INSTANT.format(r.getCreatedAt());
            items.add(item);
        });

        items.sort(Comparator.comparing((SubscriptionDtos.FeedItem i) -> i.createdAt).reversed());
        return ResponseEntity.ok(items);
    }

    private SubscriptionDtos.Response toResponse(Subscription s) {
        var out = new SubscriptionDtos.Response();
        out.id                  = s.getId().toString();
        out.subscribedToId      = s.getSubscribedTo().getId().toString();
        out.subscribedToUsername = s.getSubscribedTo().getUsername();
        out.createdAt           = DateTimeFormatter.ISO_INSTANT.format(s.getCreatedAt());
        return out;
    }
}
