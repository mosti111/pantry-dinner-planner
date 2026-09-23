CREATE TABLE cart_attempt (
    id UUID PRIMARY KEY,
    meal_plan_id UUID NOT NULL REFERENCES meal_plan(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL,
    response_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (meal_plan_id, idempotency_key)
);

CREATE INDEX cart_attempt_plan_created_idx ON cart_attempt (meal_plan_id, created_at DESC);
