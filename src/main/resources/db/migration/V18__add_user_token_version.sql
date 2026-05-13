ALTER TABLE users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 AFTER must_change_password;
