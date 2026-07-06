-- In-app notification centre (AP 3.4).
CREATE TABLE notifications (
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL,
    type       varchar(30) NOT NULL,
    payload    text,
    read_at    timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
