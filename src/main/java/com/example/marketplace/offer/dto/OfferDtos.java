package com.example.marketplace.offer.dto;

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
        @NotBlank
        public String offeredById;
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
    }

    public static class Response {
        public String id;
        public String title;
        public String description;
        public String region;
        public String category;
        public Integer quantity;
        public String offeredById;
        public String createdAt;
    }
}
