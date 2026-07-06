package de.freeworldapp.app.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReportDtos {

    /** Filed by any authenticated user against a post or another user. */
    public static class Create {
        @NotBlank
        public String targetType;   // OFFER | REQUEST | USER
        @NotBlank
        public String targetId;     // UUID of the offer/request/user
        @NotBlank
        public String reason;       // SPAM | INAPPROPRIATE | SCAM | HARASSMENT | OTHER
        @Size(max = 1000)
        public String note;         // optional free text
    }

    /** Admin moderation-queue view of a report, enriched with target details. */
    public static class AdminResponse {
        public String id;
        public String reporterId;
        public String reporterUsername;
        public String targetType;
        public String targetId;
        public String reason;
        public String note;
        public String status;
        public String createdAt;
        public String resolvedBy;
        public String resolvedAt;
        // Enriched target context (null if the target no longer exists)
        public String targetTitle;       // post title, or username for USER reports
        public String targetAuthorId;    // owner of the reported post, or the reported user's id
        public String targetAuthorUsername;
        public boolean targetExists;
        public boolean targetAuthorBlocked;
    }
}
