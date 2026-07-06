package de.freeworldapp.app.offer;

import de.freeworldapp.app.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 140)
    private String region;

    @Column(nullable = false, length = 140)
    private String category;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String imageUrl;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_by_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_offer_offered_by"))
    private User offeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, columnDefinition = "varchar(16) DEFAULT 'ACTIVE'")
    private Status status = Status.ACTIVE;

    // Optional: who the item is currently reserved for (only when status = RESERVED).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_for_id",
            foreignKey = @ForeignKey(name = "fk_offer_reserved_for"))
    private User reservedFor;

    // Geo (AP 3.1): PLZ-centroid coordinates, never an exact address.
    @Column
    private Double lat;

    @Column
    private Double lon;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(length = 100)
    private String city;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRegion() { return region; }
    public String getCategory() { return category; }
    public Integer getQuantity() { return quantity; }
    public String getImageUrl() { return imageUrl; }
    public User getOfferedBy() { return offeredBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Double getLat() { return lat; }
    public Double getLon() { return lon; }
    public String getPostalCode() { return postalCode; }
    public String getCity() { return city; }
    public void setLat(Double lat) { this.lat = lat; }
    public void setLon(Double lon) { this.lon = lon; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setCity(String city) { this.city = city; }
    public Status getStatus() { return status; }
    public User getReservedFor() { return reservedFor; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setRegion(String region) { this.region = region; }
    public void setCategory(String category) { this.category = category; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setOfferedBy(User offeredBy) { this.offeredBy = offeredBy; }
    public void setStatus(Status status) { this.status = status; }
    public void setReservedFor(User reservedFor) { this.reservedFor = reservedFor; }

    public enum Status { ACTIVE, RESERVED, GIVEN }
}
