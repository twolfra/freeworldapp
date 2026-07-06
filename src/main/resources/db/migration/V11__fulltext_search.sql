-- Postgres full-text search (AP 3.5): generated tsvector + GIN index.
ALTER TABLE offers ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('german',
        coalesce(title, '') || ' ' || coalesce(description, '') || ' ' ||
        coalesce(category, '') || ' ' || coalesce(city, '') || ' ' || coalesce(region, ''))) STORED;
CREATE INDEX idx_offers_search ON offers USING GIN (search_vector);

ALTER TABLE requests ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('german',
        coalesce(title, '') || ' ' || coalesce(description, '') || ' ' ||
        coalesce(category, '') || ' ' || coalesce(city, '') || ' ' || coalesce(region, ''))) STORED;
CREATE INDEX idx_requests_search ON requests USING GIN (search_vector);
