-- Gift lifecycle: offers ACTIVE -> RESERVED -> GIVEN, requests OPEN -> FULFILLED.
ALTER TABLE offers ADD COLUMN status varchar(16) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE offers ADD COLUMN reserved_for_id uuid;
ALTER TABLE offers ADD CONSTRAINT fk_offer_reserved_for FOREIGN KEY (reserved_for_id) REFERENCES users (id);

ALTER TABLE requests ADD COLUMN status varchar(16) DEFAULT 'OPEN' NOT NULL;
