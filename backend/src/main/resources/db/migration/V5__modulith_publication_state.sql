ALTER TABLE event_publication ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING';
ALTER TABLE event_publication ADD COLUMN last_resubmission_date TIMESTAMPTZ;
ALTER TABLE event_publication ADD COLUMN completion_attempts INTEGER NOT NULL DEFAULT 0;
