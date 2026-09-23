CREATE TABLE account (
    id UUID PRIMARY KEY,
    identity_provider VARCHAR(80) NOT NULL,
    external_subject_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'LOCKED', 'DELETED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (identity_provider, external_subject_hash)
);

ALTER TABLE guest_session ADD COLUMN claimed_by_account_id UUID REFERENCES account(id);

CREATE TABLE ingredient (
    id UUID PRIMARY KEY,
    ingredient_key VARCHAR(100) NOT NULL UNIQUE,
    canonical_name VARCHAR(200) NOT NULL,
    measurement_unit VARCHAR(20) NOT NULL CHECK (measurement_unit IN ('GRAM', 'MILLILITER', 'EACH')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE recipe (
    id UUID PRIMARY KEY,
    recipe_key VARCHAR(120) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE recipe_version (
    id UUID PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipe(id),
    version INTEGER NOT NULL CHECK (version > 0),
    title VARCHAR(240) NOT NULL,
    localized_title VARCHAR(240) NOT NULL,
    description TEXT NOT NULL,
    base_servings INTEGER NOT NULL CHECK (base_servings > 0),
    cooking_minutes INTEGER NOT NULL CHECK (cooking_minutes > 0),
    dietary_label VARCHAR(80) NOT NULL,
    allergen_codes JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (recipe_id, version)
);

CREATE TABLE recipe_ingredient (
    recipe_version_id UUID NOT NULL REFERENCES recipe_version(id) ON DELETE CASCADE,
    sequence INTEGER NOT NULL CHECK (sequence > 0),
    ingredient_id UUID NOT NULL REFERENCES ingredient(id),
    quantity NUMERIC(14,4) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(20) NOT NULL CHECK (unit IN ('GRAM', 'MILLILITER', 'EACH')),
    pantry_staple BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (recipe_version_id, sequence)
);

CREATE TABLE recipe_step (
    recipe_version_id UUID NOT NULL REFERENCES recipe_version(id) ON DELETE CASCADE,
    sequence INTEGER NOT NULL CHECK (sequence > 0),
    instruction TEXT NOT NULL,
    PRIMARY KEY (recipe_version_id, sequence)
);

CREATE TABLE retailer (
    id UUID PRIMARY KEY,
    retailer_key VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    capabilities JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE product (
    id UUID PRIMARY KEY,
    retailer_id UUID NOT NULL REFERENCES retailer(id),
    external_sku VARCHAR(160) NOT NULL,
    brand VARCHAR(160) NOT NULL,
    title VARCHAR(300) NOT NULL,
    package_quantity NUMERIC(14,4) NOT NULL CHECK (package_quantity > 0),
    package_unit VARCHAR(20) NOT NULL CHECK (package_unit IN ('GRAM', 'MILLILITER', 'EACH')),
    weighted BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (retailer_id, external_sku)
);

CREATE TABLE product_ingredient_mapping (
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    ingredient_id UUID NOT NULL REFERENCES ingredient(id),
    confidence NUMERIC(5,4) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    approval_status VARCHAR(20) NOT NULL CHECK (approval_status IN ('APPROVED', 'REVIEW', 'REJECTED')),
    provenance VARCHAR(80) NOT NULL,
    PRIMARY KEY (product_id, ingredient_id)
);

CREATE TABLE offer_snapshot (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(id),
    store_context_key VARCHAR(160) NOT NULL,
    price NUMERIC(14,2) NOT NULL CHECK (price >= 0),
    currency CHAR(3) NOT NULL,
    availability VARCHAR(20) NOT NULL CHECK (availability IN ('AVAILABLE', 'LOW_STOCK', 'UNAVAILABLE', 'UNKNOWN')),
    source_revision VARCHAR(160) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX offer_snapshot_current_idx ON offer_snapshot (product_id, store_context_key, observed_at DESC);

CREATE TABLE plan_transition (
    id UUID PRIMARY KEY,
    meal_plan_id UUID NOT NULL REFERENCES meal_plan(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(160) NOT NULL,
    plan_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX plan_transition_plan_time_idx ON plan_transition (meal_plan_id, occurred_at);

CREATE TABLE ai_proposal (
    id UUID PRIMARY KEY,
    meal_plan_id UUID REFERENCES meal_plan(id) ON DELETE CASCADE,
    schema_version VARCHAR(20) NOT NULL,
    source VARCHAR(120) NOT NULL,
    structured_payload JSONB NOT NULL,
    parse_status VARCHAR(20) NOT NULL,
    response_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE integration_call (
    id UUID PRIMARY KEY,
    provider_key VARCHAR(100) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    duration_millis BIGINT NOT NULL CHECK (duration_millis >= 0),
    sanitized_metadata JSONB NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL
);
