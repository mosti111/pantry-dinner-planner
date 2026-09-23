package com.pantry.planning.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plan_transition")
class PlanTransitionEntity {
    @Id UUID id;
    UUID mealPlanId;
    @Column(length = 32)
    String fromStatus;
    @Column(length = 32, nullable = false)
    String toStatus;
    @Column(length = 160, nullable = false)
    String reason;
    long planVersion;
    Instant occurredAt;

    protected PlanTransitionEntity() {}

    PlanTransitionEntity(UUID mealPlanId, String fromStatus, String toStatus, String reason,
            long planVersion, Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.mealPlanId = mealPlanId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.planVersion = planVersion;
        this.occurredAt = occurredAt;
    }
}
