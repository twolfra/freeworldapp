package de.freeworldapp.app.postimage;

import jakarta.persistence.*;

import java.util.UUID;

/** One gallery image of an offer/request; the first (sortOrder 0) is the cover. */
@Entity
@Table(name = "post_images")
public class PostImage {

    public enum TargetType { OFFER, REQUEST }

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "thumb_url", length = 500)
    private String thumbUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public UUID getId() { return id; }
    public TargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public String getUrl() { return url; }
    public String getThumbUrl() { return thumbUrl; }
    public int getSortOrder() { return sortOrder; }

    public void setTargetType(TargetType targetType) { this.targetType = targetType; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public void setUrl(String url) { this.url = url; }
    public void setThumbUrl(String thumbUrl) { this.thumbUrl = thumbUrl; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
