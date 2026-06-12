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

    public static class Response {
        public String id;
        public String username;
        public String email;
        public String createdAt;
    }
}
