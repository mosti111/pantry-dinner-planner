package com.pantry.planning.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "cart_attempt", uniqueConstraints = @UniqueConstraint(columnNames = {"meal_plan_id", "idempotency_key"}))
class CartAttemptEntity {
    @Id
    UUID id;
    UUID mealPlanId;
    @Column(length = 128, nullable = false)
    String idempotencyKey;
    @Column(length = 64, nullable = false)
    String requestFingerprint;
    @Column(length = 30, nullable = false)
    String status;
    @Column(length = 160)
    String externalReference;
    @JdbcTypeCode(SqlTypes.JSON)
    JsonNode responsePayload;
    Instant createdAt;

    protected CartAttemptEntity() {}

    CartAttemptEntity(UUID id, UUID mealPlanId, String idempotencyKey, String requestFingerprint,
            String status, String externalReference, JsonNode responsePayload, Instant createdAt) {
        this.id = id;
        this.mealPlanId = mealPlanId;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.status = status;
        this.externalReference = externalReference;
        this.responsePayload = responsePayload;
        this.createdAt = createdAt;
    }
}
