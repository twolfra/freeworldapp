-- "Danke" system: one qualitative thank-you per completed gift (no scores).
-- offer_id/offer_title are denormalized (no FK) so a thanks survives offer deletion.
CREATE TABLE thanks (
    id           uuid PRIMARY KEY,
    from_user_id uuid         NOT NULL,
    to_user_id   uuid         NOT NULL,
    offer_id     uuid         NOT NULL,
    offer_title  varchar(140) NOT NULL,
    text         varchar(280),
    created_at   timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_thanks_offer UNIQUE (offer_id),
    CONSTRAINT fk_thanks_from FOREIGN KEY (from_user_id) REFERENCES users (id),
    CONSTRAINT fk_thanks_to FOREIGN KEY (to_user_id) REFERENCES users (id)
);

CREATE INDEX idx_thanks_to_user ON thanks (to_user_id);
