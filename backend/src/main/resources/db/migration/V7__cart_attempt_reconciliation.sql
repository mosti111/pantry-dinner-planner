ALTER TABLE cart_attempt ADD COLUMN request_fingerprint VARCHAR(64);
ALTER TABLE cart_attempt ADD COLUMN status VARCHAR(30);
ALTER TABLE cart_attempt ADD COLUMN external_reference VARCHAR(160);

UPDATE cart_attempt
SET request_fingerprint = repeat('0', 64),
    status = 'CREATED';

ALTER TABLE cart_attempt ALTER COLUMN request_fingerprint SET NOT NULL;
ALTER TABLE cart_attempt ALTER COLUMN status SET NOT NULL;
