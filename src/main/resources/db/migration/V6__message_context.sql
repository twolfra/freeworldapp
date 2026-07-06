-- Optional post reference on messages ("I'm interested in: X" flow).
ALTER TABLE messages ADD COLUMN context_type varchar(20);
ALTER TABLE messages ADD COLUMN context_id uuid;
