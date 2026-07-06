package de.freeworldapp.app.postimage;

import de.freeworldapp.app.image.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gallery management shared by offer/request controllers. The first image is
 * the cover and is mirrored into the post's legacy imageUrl column so every
 * existing consumer (lists, OG tags, e-mails) keeps working.
 */
@Service
public class PostImageService {

    public static final int MAX_IMAGES = 5;

    private final PostImageRepository repo;
    private final StorageService storage;

    public PostImageService(PostImageRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    public List<Map<String, Object>> list(PostImage.TargetType type, UUID targetId) {
        return repo.findByTargetTypeAndTargetIdOrderBySortOrder(type, targetId).stream()
                .map(i -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
                    m.put("url", i.getUrl());
                    m.put("thumbUrl", i.getThumbUrl());
                    m.put("sortOrder", i.getSortOrder());
                    return m;
                })
                .toList();
    }

    /**
     * Replaces the gallery with the given ordered urls (max 5). Stored files
     * that are no longer referenced are deleted. Returns the new cover url
     * (or null when the gallery is now empty).
     */
    @Transactional
    public String replace(PostImage.TargetType type, UUID targetId, List<Map<String, String>> images) {
        List<PostImage> existing = repo.findByTargetTypeAndTargetIdOrderBySortOrder(type, targetId);
        List<String> keptUrls = images == null ? List.of()
                : images.stream().map(i -> i.get("url")).filter(u -> u != null && !u.isBlank()).toList();

        // delete files dropped from the gallery
        for (PostImage old : existing) {
            if (!keptUrls.contains(old.getUrl())) storage.delete(old.getUrl());
        }
        repo.deleteByTargetTypeAndTargetId(type, targetId);

        List<PostImage> fresh = new ArrayList<>();
        int order = 0;
        if (images != null) {
            for (Map<String, String> img : images) {
                String url = img.get("url");
                if (url == null || url.isBlank()) continue;
                if (order >= MAX_IMAGES) break;
                PostImage p = new PostImage();
                p.setTargetType(type);
                p.setTargetId(targetId);
                p.setUrl(url);
                p.setThumbUrl(img.get("thumbUrl"));
                p.setSortOrder(order++);
                fresh.add(p);
            }
        }
        repo.saveAll(fresh);
        return fresh.isEmpty() ? null : fresh.get(0).getUrl();
    }

    /** Full cleanup on post deletion — removes rows and stored files. */
    @Transactional
    public void deleteAll(PostImage.TargetType type, UUID targetId) {
        repo.findByTargetTypeAndTargetIdOrderBySortOrder(type, targetId)
                .forEach(i -> storage.delete(i.getUrl()));
        repo.deleteByTargetTypeAndTargetId(type, targetId);
    }
}
