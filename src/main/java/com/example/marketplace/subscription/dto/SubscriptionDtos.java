package com.example.marketplace.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public class SubscriptionDtos {

    public static class Create {
        @NotBlank public String subscribedToId;
    }

    public static class Response {
        public String id;
        public String subscribedToId;
        public String subscribedToUsername;
        public String createdAt;
    }

    public static class CheckResponse {
        public boolean subscribed;
    }

    public static class FeedItem {
        public String type; // "offer" or "request"
        public String id;
        public String title;
        public String description;
        public String region;
        public String category;
        public Integer quantity;
        public String authorId;
        public String authorUsername;
        public String createdAt;
    }
}
