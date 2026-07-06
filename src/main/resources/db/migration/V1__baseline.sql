-- Baseline schema for FreeWorld (matches the entity model as of Phase 0).
-- Existing databases created by ddl-auto:update are baselined via
-- spring.flyway.baseline-on-migrate=true and never run this script.

CREATE TABLE users (
    id                            uuid PRIMARY KEY,
    username                      varchar(32)  NOT NULL,
    email                         varchar(255) NOT NULL,
    password_hash                 varchar(60)  NOT NULL,
    created_at                    timestamp(6) with time zone NOT NULL,
    email_verified                boolean DEFAULT false NOT NULL,
    verification_token            varchar(36),
    verification_token_expires_at timestamp(6) with time zone,
    role                          varchar(16) DEFAULT 'USER' NOT NULL,
    blocked                       boolean DEFAULT false NOT NULL,
    blocked_at                    timestamp(6) with time zone,
    notify_on_message             boolean DEFAULT true NOT NULL,
    unsubscribe_token             varchar(36),
    language                      varchar(8) DEFAULT 'en' NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE sessions (
    id         uuid PRIMARY KEY,
    token      varchar(36) NOT NULL,
    user_id    uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone,
    CONSTRAINT uk_sessions_token UNIQUE (token),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE offers (
    id            uuid PRIMARY KEY,
    title         varchar(140)  NOT NULL,
    description   varchar(4000) NOT NULL,
    region        varchar(140)  NOT NULL,
    category      varchar(140)  NOT NULL,
    quantity      integer       NOT NULL,
    image_url     varchar(500),
    offered_by_id uuid          NOT NULL,
    created_at    timestamp(6) with time zone NOT NULL,
    CONSTRAINT fk_offer_offered_by FOREIGN KEY (offered_by_id) REFERENCES users (id)
);

CREATE TABLE requests (
    id              uuid PRIMARY KEY,
    title           varchar(140)  NOT NULL,
    description     varchar(4000) NOT NULL,
    region          varchar(140)  NOT NULL,
    category        varchar(140)  NOT NULL,
    quantity        integer       NOT NULL,
    image_url       varchar(500),
    requested_by_id uuid          NOT NULL,
    created_at      timestamp(6) with time zone NOT NULL,
    CONSTRAINT fk_request_requested_by FOREIGN KEY (requested_by_id) REFERENCES users (id)
);

CREATE TABLE messages (
    id           uuid PRIMARY KEY,
    sender_id    uuid          NOT NULL,
    recipient_id uuid          NOT NULL,
    content      varchar(2000) NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    read_at      timestamp(6) with time zone,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT fk_message_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
);

CREATE TABLE subscriptions (
    id               uuid PRIMARY KEY,
    subscriber_id    uuid NOT NULL,
    subscribed_to_id uuid NOT NULL,
    created_at       timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_subscription UNIQUE (subscriber_id, subscribed_to_id),
    CONSTRAINT fk_subscription_subscriber FOREIGN KEY (subscriber_id) REFERENCES users (id),
    CONSTRAINT fk_subscription_subscribed_to FOREIGN KEY (subscribed_to_id) REFERENCES users (id)
);

CREATE TABLE likes (
    id          uuid PRIMARY KEY,
    user_id     uuid        NOT NULL,
    target_type varchar(20) NOT NULL,
    target_id   uuid        NOT NULL,
    created_at  timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_like_user_target UNIQUE (user_id, target_type, target_id),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE reports (
    id          uuid PRIMARY KEY,
    reporter_id uuid          NOT NULL,
    target_type varchar(20)   NOT NULL,
    target_id   uuid          NOT NULL,
    reason      varchar(20)   NOT NULL,
    note        varchar(1000),
    status      varchar(20)   NOT NULL,
    created_at  timestamp(6) with time zone NOT NULL,
    resolved_by uuid,
    resolved_at timestamp(6) with time zone,
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES users (id)
);

-- Postgres does not index foreign keys automatically.
CREATE INDEX idx_sessions_user ON sessions (user_id);
CREATE INDEX idx_offers_offered_by ON offers (offered_by_id);
CREATE INDEX idx_requests_requested_by ON requests (requested_by_id);
CREATE INDEX idx_messages_sender ON messages (sender_id);
CREATE INDEX idx_messages_recipient ON messages (recipient_id);
CREATE INDEX idx_subscriptions_subscriber ON subscriptions (subscriber_id);
CREATE INDEX idx_likes_target ON likes (target_type, target_id);
CREATE INDEX idx_reports_status ON reports (status);
