package com.pantry.planning.internal;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "meal_plan")
class MealPlanEntity {
    @Id
    UUID id;
    UUID guestSessionId;
    String idempotencyKey;
    String requestFingerprint;
    String status;
    @JdbcTypeCode(SqlTypes.JSON)
    JsonNode requestPayload;
    @JdbcTypeCode(SqlTypes.JSON)
    JsonNode planPayload;
    Instant createdAt;
    Instant updatedAt;
    @Version
    long version;

    protected MealPlanEntity() {}

    MealPlanEntity(UUID id, UUID guestSessionId, String idempotencyKey, String requestFingerprint,
                   JsonNode requestPayload, JsonNode planPayload, Instant now) {
        this.id = id;
        this.guestSessionId = guestSessionId;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.status = "READY_FOR_REVIEW";
        this.requestPayload = requestPayload;
        this.planPayload = planPayload;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void update(String status, JsonNode planPayload, Instant now) {
        this.status = status;
        this.planPayload = planPayload;
        this.updatedAt = now;
    }
}
