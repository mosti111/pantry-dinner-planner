ALTER TABLE meal_plan ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE meal_plan ADD COLUMN request_fingerprint VARCHAR(64);

UPDATE meal_plan
SET idempotency_key = id::text,
    request_fingerprint = repeat('0', 64);

ALTER TABLE meal_plan ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE meal_plan ALTER COLUMN request_fingerprint SET NOT NULL;
ALTER TABLE meal_plan ADD CONSTRAINT meal_plan_guest_idempotency_key UNIQUE (guest_session_id, idempotency_key);
