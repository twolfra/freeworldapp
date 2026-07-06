package de.freeworldapp.app.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MessageDtos {

    public static class Send {
        @NotBlank
        public String recipientId;
        @NotBlank
        @Size(max = 2000)
        public String content;
    }

    public static class Response {
        public String id;
        public String senderId;
        public String senderUsername;
        public String recipientId;
        public String recipientUsername;
        public String content;
        public String createdAt;
        public String readAt; // null if unread
        public String contextType; // OFFER/REQUEST or null
        public String contextId;
    }

    public static class ConversationSummary {
        public String userId;
        public String username;
        public String lastMessage;
        public String lastMessageAt;
        public long unreadCount;
    }
}
