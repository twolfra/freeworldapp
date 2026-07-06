-- Session tokens are no longer stored in plaintext; only their SHA-256 hash.
-- Existing sessions cannot be migrated (we never knew a hash preimage schema),
-- so all users are logged out once (force re-login).
DELETE FROM sessions;

ALTER TABLE sessions DROP COLUMN token;
ALTER TABLE sessions ADD COLUMN token_hash varchar(64) NOT NULL;
ALTER TABLE sessions ADD CONSTRAINT uk_sessions_token_hash UNIQUE (token_hash);
