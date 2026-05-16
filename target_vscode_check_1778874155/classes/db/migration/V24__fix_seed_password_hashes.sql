-- Fix incorrect seed hashes so documented password works.
-- Password for all listed users: Admin1234!

UPDATE users
SET password_hash = '$2a$12$Tx9OHm3F5EYQU4YtJcFhdOjoqfbHWDtqXaGrCrPH7RYN9VvKxNvpa',
    must_change_password = 0,
    is_active = 1
WHERE username IN (
    'admin',
    'manager',
    'viewer',
    'nova_admin',
    'nova_manager',
    'mid_admin',
    'mid_manager',
    'zero_admin',
    'zero_manager'
);
