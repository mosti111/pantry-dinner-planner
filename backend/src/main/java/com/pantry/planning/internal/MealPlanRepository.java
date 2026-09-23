package com.pantry.planning.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MealPlanRepository extends JpaRepository<MealPlanEntity, UUID> {
    Optional<MealPlanEntity> findByIdAndGuestSessionId(UUID id, UUID guestSessionId);
    Optional<MealPlanEntity> findByGuestSessionIdAndIdempotencyKey(UUID guestSessionId, String idempotencyKey);
}
