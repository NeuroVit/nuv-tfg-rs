ALTER TABLE users
    ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0 AFTER avatar_url;

UPDATE users
SET must_change_password = 0
WHERE must_change_password IS NULL;
