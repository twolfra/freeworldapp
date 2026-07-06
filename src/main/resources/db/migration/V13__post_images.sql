-- Multi-image galleries (AP 3.3): up to 5 images per post, ordered.
-- offers/requests.image_url stays as the cover (first image).
CREATE TABLE post_images (
    id          uuid PRIMARY KEY,
    target_type varchar(20)  NOT NULL,
    target_id   uuid         NOT NULL,
    url         varchar(500) NOT NULL,
    thumb_url   varchar(500),
    sort_order  integer      NOT NULL DEFAULT 0
);

CREATE INDEX idx_post_images_target ON post_images (target_type, target_id, sort_order);

-- Existing single images become the first gallery entry (legacy uploads have
-- no thumbnail variant; thumb_url stays NULL there).
INSERT INTO post_images (id, target_type, target_id, url, sort_order)
SELECT gen_random_uuid(), 'OFFER', id, image_url, 0 FROM offers WHERE image_url IS NOT NULL;
INSERT INTO post_images (id, target_type, target_id, url, sort_order)
SELECT gen_random_uuid(), 'REQUEST', id, image_url, 0 FROM requests WHERE image_url IS NOT NULL;
