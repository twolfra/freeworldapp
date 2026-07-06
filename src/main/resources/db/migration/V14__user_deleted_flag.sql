-- DSGVO deletion (AP 4.4): self-deleted accounts are anonymized, not removed,
-- so the other side of a conversation keeps its context ("Deleted account").
ALTER TABLE users ADD COLUMN deleted boolean DEFAULT false NOT NULL;
