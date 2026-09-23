package com.pantry.planning.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartAttemptRepository extends JpaRepository<CartAttemptEntity, UUID> {
    Optional<CartAttemptEntity> findByMealPlanIdAndIdempotencyKey(UUID mealPlanId, String idempotencyKey);
}
