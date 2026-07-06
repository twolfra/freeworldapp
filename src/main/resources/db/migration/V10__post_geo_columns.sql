-- Geo columns for radius search (AP 3.1). Existing free-text region is kept
-- as display text; legacy rows copy it into city.
ALTER TABLE offers ADD COLUMN lat double precision;
ALTER TABLE offers ADD COLUMN lon double precision;
ALTER TABLE offers ADD COLUMN postal_code varchar(10);
ALTER TABLE offers ADD COLUMN city varchar(100);
UPDATE offers SET city = region WHERE city IS NULL;
CREATE INDEX idx_offers_lat_lon ON offers (lat, lon);

ALTER TABLE requests ADD COLUMN lat double precision;
ALTER TABLE requests ADD COLUMN lon double precision;
ALTER TABLE requests ADD COLUMN postal_code varchar(10);
ALTER TABLE requests ADD COLUMN city varchar(100);
UPDATE requests SET city = region WHERE city IS NULL;
CREATE INDEX idx_requests_lat_lon ON requests (lat, lon);
