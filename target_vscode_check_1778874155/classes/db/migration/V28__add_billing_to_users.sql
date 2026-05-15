ALTER TABLE users
  ADD COLUMN stripe_customer_id  VARCHAR(255) NULL,
  ADD COLUMN subscription_id     VARCHAR(255) NULL,
  ADD COLUMN subscription_status VARCHAR(20)  NOT NULL DEFAULT 'INACTIVE';
