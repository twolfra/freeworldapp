-- Seed script for load/perf testing the search endpoint (AP 3.5).
-- Creates one seed user plus 10.000 offers and 2.000 requests spread over
-- random German postal codes. Run against a DEV database only:
--   psql "$DB_URL" -f scripts/seed-demo-data.sql

WITH seed_user AS (
    INSERT INTO users (id, username, email, password_hash, created_at, email_verified,
                       role, blocked, notify_on_message, language)
    VALUES (gen_random_uuid(), 'seed_user',
            'seed@example.invalid',
            '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5B0aM1Wp5g4M0K7yPqfSufaMLxK6W', -- "password"
            now(), true, 'USER', false, false, 'de')
    ON CONFLICT (username) DO UPDATE SET email = EXCLUDED.email
    RETURNING id
),
plz AS (
    SELECT plz, city, lat, lon, row_number() OVER (ORDER BY random()) AS rn
    FROM plz_geo
)
INSERT INTO offers (id, title, description, region, category, quantity, offered_by_id,
                    created_at, status, postal_code, city, lat, lon)
SELECT gen_random_uuid(),
       'Seed offer #' || g || ' — ' || (ARRAY['Fahrrad','Bücherkiste','Sofa','Werkzeug','Kinderwagen',
                                              'Pflanzen','Laptop','Winterjacke','Geschirr','Lampen'])[1 + g % 10],
       'Automatically generated demo item number ' || g || ' for search load testing.',
       p.plz || ' ' || p.city,
       (ARRAY['Food & Drink','Clothing','Books & Media','Tools & Equipment','Furniture','Electronics',
              'Skills & Services','Plants & Seeds','Childcare','Transport','Other'])[1 + g % 11],
       1 + g % 5,
       (SELECT id FROM seed_user),
       now() - (g || ' minutes')::interval,
       CASE WHEN g % 10 = 0 THEN 'GIVEN' ELSE 'ACTIVE' END,
       p.plz, p.city, p.lat, p.lon
FROM generate_series(1, 10000) AS g
JOIN plz p ON p.rn = 1 + (g % (SELECT count(*) FROM plz_geo));

WITH seed_user AS (SELECT id FROM users WHERE username = 'seed_user'),
plz AS (
    SELECT plz, city, lat, lon, row_number() OVER (ORDER BY random()) AS rn
    FROM plz_geo
)
INSERT INTO requests (id, title, description, region, category, quantity, requested_by_id,
                      created_at, status, postal_code, city, lat, lon)
SELECT gen_random_uuid(),
       'Seed request #' || g,
       'Automatically generated demo request number ' || g || '.',
       p.plz || ' ' || p.city,
       (ARRAY['Food & Drink','Clothing','Books & Media','Tools & Equipment','Furniture','Electronics',
              'Skills & Services','Plants & Seeds','Childcare','Transport','Other'])[1 + g % 11],
       1,
       (SELECT id FROM seed_user),
       now() - (g || ' minutes')::interval,
       CASE WHEN g % 10 = 0 THEN 'FULFILLED' ELSE 'OPEN' END,
       p.plz, p.city, p.lat, p.lon
FROM generate_series(1, 2000) AS g
JOIN plz p ON p.rn = 1 + (g % (SELECT count(*) FROM plz_geo));
