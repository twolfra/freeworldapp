package com.example.marketplace.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDtos {

    public static class Create {
        @NotBlank @Size(min = 3, max = 32)
        public String username;
        @NotBlank @Email
        public String email;
        @NotBlank @Size(min = 6, max = 72)
        public String password;
    }

    public static class Update {
        @NotBlank @Size(min = 3, max = 32)
        public String username;
        @NotBlank @Email
        public String email;
    }

    public static class Login {
        @NotBlank
        public String username;
        @NotBlank
        public String password;
    }

    public static class Response {
        public String id;
        public String username;
        public String email;
        public String createdAt;
        public String role;  // USER or ADMIN
        public String token; // only populated on login
    }

    /** Returned by public GET endpoints — no email exposed. */
    public static class PublicResponse {
        public String id;
        public String username;
        public String createdAt;
    }

    /** Admin-only view of a user — includes email, role, block state and post counts. */
    public static class AdminResponse {
        public String id;
        public String username;
        public String email;
        public String createdAt;
        public String role;
        public boolean blocked;
        public String blockedAt;
        public long offerCount;
        public long requestCount;
    }
}
