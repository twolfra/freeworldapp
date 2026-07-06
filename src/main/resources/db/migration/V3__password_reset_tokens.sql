CREATE TABLE password_reset_tokens (
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL,
    token_hash varchar(64) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    used_at    timestamp(6) with time zone,
    CONSTRAINT uk_prt_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_prt_user ON password_reset_tokens (user_id);
