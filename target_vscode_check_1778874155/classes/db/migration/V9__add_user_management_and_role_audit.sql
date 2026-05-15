ALTER TABLE users
    ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER role;

UPDATE users
SET is_active = 1
WHERE is_active IS NULL;

CREATE TABLE user_role_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    changed_by BIGINT NULL,
    old_role ENUM('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_VIEWER') NOT NULL,
    new_role ENUM('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_VIEWER') NOT NULL,
    reason VARCHAR(255),
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_role_audit_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_role_audit_changed_by
        FOREIGN KEY (changed_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_user_role_audit_user (user_id),
    INDEX idx_user_role_audit_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
