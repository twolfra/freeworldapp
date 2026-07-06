-- Optional profile fields (AP 2.6). postal_code is never exposed publicly.
ALTER TABLE users ADD COLUMN display_name varchar(60);
ALTER TABLE users ADD COLUMN bio varchar(500);
ALTER TABLE users ADD COLUMN avatar_url varchar(500);
ALTER TABLE users ADD COLUMN postal_code varchar(10);
ALTER TABLE users ADD COLUMN city varchar(100);
