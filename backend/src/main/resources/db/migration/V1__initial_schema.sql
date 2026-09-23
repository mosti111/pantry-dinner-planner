CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE guest_session (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CLAIMED', 'EXPIRED', 'DELETED')),
    created_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE meal_plan (
    id UUID PRIMARY KEY,
    guest_session_id UUID NOT NULL REFERENCES guest_session(id),
    status VARCHAR(32) NOT NULL,
    request_payload JSONB NOT NULL,
    plan_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX meal_plan_guest_created_idx ON meal_plan (guest_session_id, created_at DESC);

CREATE TABLE ingredient_alias (
    id UUID PRIMARY KEY,
    ingredient_key VARCHAR(100) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    alias VARCHAR(200) NOT NULL,
    normalized_alias VARCHAR(200) NOT NULL,
    confidence NUMERIC(5,4) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    UNIQUE (locale, normalized_alias, ingredient_key)
);

CREATE INDEX ingredient_alias_trgm_idx ON ingredient_alias USING gin (normalized_alias gin_trgm_ops);

CREATE TABLE event_publication (
    id UUID NOT NULL,
    listener_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    completion_date TIMESTAMPTZ,
    PRIMARY KEY (id)
);
CREATE INDEX event_publication_incomplete_idx ON event_publication (publication_date) WHERE completion_date IS NULL;
