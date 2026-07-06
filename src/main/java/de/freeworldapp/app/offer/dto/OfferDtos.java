package de.freeworldapp.app.offer.dto;

import jakarta.validation.constraints.*;

public class OfferDtos {

    public static class Create {
        @NotBlank @Size(max = 140)
        public String title;
        @NotBlank @Size(max = 4000)
        public String description;
        @NotBlank @Size(max = 140)
        public String region;
        @NotBlank @Size(max = 140)
        public String category;
        @NotNull @Min(1)
        public Integer quantity;
        public String imageUrl;
        @Size(max = 10)
        public String postalCode; // optional; resolved against plz_geo
    }

    public static class Update {
        @NotBlank @Size(max = 140)
        public String title;
        @NotBlank @Size(max = 4000)
        public String description;
        @NotBlank @Size(max = 140)
        public String region;
        @NotBlank @Size(max = 140)
        public String category;
        @NotNull @Min(1)
        public Integer quantity;
        public String imageUrl;
        @Size(max = 10)
        public String postalCode; // optional; resolved against plz_geo
    }

    public static class StatusUpdate {
        @NotBlank
        public String status;            // ACTIVE / RESERVED / GIVEN
        public String reservedForId;     // optional, only meaningful with RESERVED
    }

    public static class Response {
        public String id;
        public String title;
        public String description;
        public String region;
        public String category;
        public Integer quantity;
        public String offeredById;
        public String offeredByUsername;
        public String imageUrl;
        public String createdAt;
        public Double lat;
        public Double lon;
        public String postalCode;
        public String city;
        public Double distanceKm; // only set by /api/search with a location
        public java.util.List<java.util.Map<String, Object>> images; // gallery, only on detail
        public String status;
        public String reservedForId;
        public String reservedForUsername;
    }
}
