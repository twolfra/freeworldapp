CREATE TABLE admin_audit_log (
    id             uuid PRIMARY KEY,
    admin_id       uuid        NOT NULL,
    admin_username varchar(32) NOT NULL,
    action         varchar(40) NOT NULL,
    target_type    varchar(20) NOT NULL,
    target_id      uuid,
    created_at     timestamp(6) with time zone NOT NULL
);

CREATE INDEX idx_audit_created ON admin_audit_log (created_at);
