package de.freeworldapp.app.request;

import de.freeworldapp.app.auth.AdminGuard;
import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.image.StorageService;
import de.freeworldapp.app.like.Like;
import de.freeworldapp.app.like.LikeRepository;
import de.freeworldapp.app.request.dto.RequestDtos;
import de.freeworldapp.app.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestRepository requestRepo;
    private final UserRepository userRepo;
    private final StorageService storage;
    private final LikeRepository likeRepo;
    private final AdminGuard adminGuard;

    public RequestController(RequestRepository requestRepo, UserRepository userRepo, StorageService storage,
                             LikeRepository likeRepo, AdminGuard adminGuard) {
        this.requestRepo = requestRepo;
        this.userRepo = userRepo;
        this.storage = storage;
        this.likeRepo = likeRepo;
        this.adminGuard = adminGuard;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RequestDtos.Create in) {
        UUID userId = SecurityContext.authenticatedId();
        return userRepo.findById(userId)
                .map(user -> {
                    Request r = new Request();
                    r.setTitle(in.title);
                    r.setDescription(in.description);
                    r.setRegion(in.region);
                    r.setCategory(in.category);
                    r.setQuantity(in.quantity);
                    r.setImageUrl(in.imageUrl);
                    r.setRequestedBy(user);
                    Request saved = requestRepo.save(r);
                    return ResponseEntity
                            .created(URI.create("/api/requests/" + saved.getId()))
                            .body((Object) toResponse(saved));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "User not found.")));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String requestedBy,
                                  @RequestParam(defaultValue = "false") boolean includeCompleted) {
        if (requestedBy != null) {
            UUID uid;
            try { uid = UUID.fromString(requestedBy); }
            catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
            }
            // Per-user views (profile, own management) always show every status.
            return ResponseEntity.ok(requestRepo.findByRequestedBy_Id(uid).stream().map(this::toResponse).toList());
        }
        return ResponseEntity.ok(requestRepo.findAll().stream()
                .filter(r -> !r.getRequestedBy().isBlocked())
                .filter(r -> includeCompleted || r.getStatus() != Request.Status.FULFILLED)
                .map(this::toResponse).toList());
    }

    /** Lifecycle: owner (or admin) moves a request between OPEN / FULFILLED. */
    @PostMapping("{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable UUID id, @Valid @RequestBody RequestDtos.StatusUpdate in) {
        UUID callerId = SecurityContext.authenticatedId();
        return requestRepo.findById(id)
                .<ResponseEntity<?>>map(r -> {
                    if (!r.getRequestedBy().getId().equals(callerId) && !adminGuard.isAdmin())
                        return ResponseEntity.status(403).body(Map.of("error", "Not your request."));
                    Request.Status newStatus;
                    try { newStatus = Request.Status.valueOf(in.status.toUpperCase()); }
                    catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid status."));
                    }
                    r.setStatus(newStatus);
                    return ResponseEntity.ok(toResponse(requestRepo.save(r)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("{id}")
    public ResponseEntity<RequestDtos.Response> get(@PathVariable UUID id) {
        return requestRepo.findById(id)
                .map(r -> ResponseEntity.ok(toResponse(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody RequestDtos.Update in) {
        UUID callerId = SecurityContext.authenticatedId();
        return requestRepo.findById(id)
                .map(r -> {
                    if (!r.getRequestedBy().getId().equals(callerId))
                        return ResponseEntity.status(403).body((Object) Map.of("error", "Not your request."));
                    String oldImage = r.getImageUrl();
                    r.setTitle(in.title);
                    r.setDescription(in.description);
                    r.setRegion(in.region);
                    r.setCategory(in.category);
                    r.setQuantity(in.quantity);
                    r.setImageUrl(in.imageUrl);
                    Request saved = requestRepo.save(r);
                    if (oldImage != null && !oldImage.equals(in.imageUrl)) storage.delete(oldImage);
                    return ResponseEntity.ok((Object) toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        UUID callerId = SecurityContext.authenticatedId();
        return requestRepo.findById(id)
                .map(r -> {
                    if (!r.getRequestedBy().getId().equals(callerId))
                        return ResponseEntity.status(403).<Void>build();
                    String imageUrl = r.getImageUrl();
                    likeRepo.deleteAllByTargetTypeAndTargetId(Like.TargetType.REQUEST, id);
                    requestRepo.delete(r);
                    storage.delete(imageUrl);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private RequestDtos.Response toResponse(Request r) {
        var out = new RequestDtos.Response();
        out.id = r.getId().toString();
        out.title = r.getTitle();
        out.description = r.getDescription();
        out.region = r.getRegion();
        out.category = r.getCategory();
        out.quantity = r.getQuantity();
        out.requestedById = r.getRequestedBy().getId().toString();
        out.requestedByUsername = r.getRequestedBy().getUsername();
        out.imageUrl = r.getImageUrl();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(r.getCreatedAt());
        out.status = r.getStatus().name();
        return out;
    }
}
