package de.freeworldapp.app.like;

import de.freeworldapp.app.auth.Session;
import de.freeworldapp.app.auth.SessionRepository;
import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.request.RequestRepository;
import de.freeworldapp.app.user.User;
import de.freeworldapp.app.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @PostMapping
    public ResponseEntity<Map<String, String>> like(
            @RequestParam String targetType,
            @RequestParam UUID targetId,
            @RequestHeader("X-Session-Token") String token) {
        var sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty() || (sessionOpt.get().getExpiresAt() != null && sessionOpt.get().getExpiresAt().isBefore(Instant.now())))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UUID userId = sessionOpt.get().getUser().getId();

        try {
            Like.TargetType type = Like.TargetType.valueOf(targetType.toUpperCase());

            // Verify the target exists
            if (type == Like.TargetType.OFFER) {
                if (!offerRepository.existsById(targetId)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Offer not found"));
                }
            } else {
                if (!requestRepository.existsById(targetId)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Request not found"));
                }
            }

            // Check if already liked
            if (likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, type, targetId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Already liked"));
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Like like = new Like();
            like.setUser(user);
            like.setTargetType(type);
            like.setTargetId(targetId);
            likeRepository.save(like);

            return ResponseEntity.ok(Map.of("message", "Liked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid target type"));
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> unlike(
            @RequestParam String targetType,
            @RequestParam UUID targetId,
            @RequestHeader("X-Session-Token") String token) {
        var sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty() || (sessionOpt.get().getExpiresAt() != null && sessionOpt.get().getExpiresAt().isBefore(Instant.now())))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UUID userId = sessionOpt.get().getUser().getId();

        try {
            Like.TargetType type = Like.TargetType.valueOf(targetType.toUpperCase());
            Optional<Like> like = likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId);

            if (like.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Like not found"));
            }

            likeRepository.deleteById(like.get().getId());
            return ResponseEntity.ok(Map.of("message", "Unliked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid target type"));
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> check(
            @RequestParam String targetType,
            @RequestParam UUID targetId,
            @RequestHeader("X-Session-Token") String token) {
        var sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty() || (sessionOpt.get().getExpiresAt() != null && sessionOpt.get().getExpiresAt().isBefore(Instant.now())))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UUID userId = sessionOpt.get().getUser().getId();

        try {
            Like.TargetType type = Like.TargetType.valueOf(targetType.toUpperCase());
            boolean liked = likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, type, targetId);
            long count = likeRepository.countByTargetTypeAndTargetId(type, targetId);
            return ResponseEntity.ok(Map.of("liked", liked, "count", count));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid target type"));
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUserLikes(
            @RequestParam UUID userId,
            @RequestParam(required = false) String targetType,
            @RequestHeader("X-Session-Token") String token) {
        var sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty() || (sessionOpt.get().getExpiresAt() != null && sessionOpt.get().getExpiresAt().isBefore(Instant.now())))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UUID sessionUserId = sessionOpt.get().getUser().getId();

        // Users can only fetch their own likes
        if (!sessionUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Like> likes;
        if (targetType != null) {
            try {
                Like.TargetType type = Like.TargetType.valueOf(targetType.toUpperCase());
                likes = likeRepository.findByUserIdAndTargetType(userId, type);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            likes = likeRepository.findAll().stream()
                    .filter(l -> l.getUser().getId().equals(userId))
                    .toList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Like like : likes) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", like.getTargetId());
            item.put("type", like.getTargetType().toString().toLowerCase());
            item.put("likedAt", like.getCreatedAt());

            // Fetch the actual post data
            if (like.getTargetType() == Like.TargetType.OFFER) {
                offerRepository.findById(like.getTargetId()).ifPresent(offer -> {
                    item.put("title", offer.getTitle());
                    item.put("description", offer.getDescription());
                    item.put("region", offer.getRegion());
                    item.put("category", offer.getCategory());
                    item.put("quantity", offer.getQuantity());
                    item.put("imageUrl", offer.getImageUrl());
                    item.put("createdAt", offer.getCreatedAt());
                    item.put("authorId", offer.getOfferedBy().getId());
                    item.put("authorUsername", offer.getOfferedBy().getUsername());
                });
            } else {
                requestRepository.findById(like.getTargetId()).ifPresent(req -> {
                    item.put("title", req.getTitle());
                    item.put("description", req.getDescription());
                    item.put("region", req.getRegion());
                    item.put("category", req.getCategory());
                    item.put("quantity", req.getQuantity());
                    item.put("imageUrl", req.getImageUrl());
                    item.put("createdAt", req.getCreatedAt());
                    item.put("authorId", req.getRequestedBy().getId());
                    item.put("authorUsername", req.getRequestedBy().getUsername());
                });
            }

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }
}
