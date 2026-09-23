package com.pantry.planning.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pantry.ingredient.IngredientNormalizer;
import com.pantry.recipe.internal.ReferenceRecipeCatalog;
import com.pantry.retailer.internal.ReferenceRetailerGateway;
import com.pantry.planning.IdempotencyConflictException;
import com.pantry.planning.PlanView;
import com.pantry.planning.PlanningRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class PlanOperationsTest {
    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final CartAttemptRepository attempts = mock(CartAttemptRepository.class);
    private final PlanTransitionRepository transitions = mock(PlanTransitionRepository.class);
    private final AtomicReference<MealPlanEntity> stored = new AtomicReference<>();
    private final AtomicReference<CartAttemptEntity> storedAttempt = new AtomicReference<>();
    private final PlanOperations operations;

    PlanOperationsTest() {
        when(plans.save(any())).thenAnswer(invocation -> {
            MealPlanEntity entity = invocation.getArgument(0);
            stored.set(entity);
            return entity;
        });
        when(plans.findByGuestSessionIdAndIdempotencyKey(any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get()).filter(entity ->
                        entity.guestSessionId.equals(invocation.getArgument(0))
                                && entity.idempotencyKey.equals(invocation.getArgument(1))));
        when(plans.findByIdAndGuestSessionId(any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get()).filter(entity ->
                        entity.id.equals(invocation.getArgument(0))
                                && entity.guestSessionId.equals(invocation.getArgument(1))));
        when(attempts.save(any())).thenAnswer(invocation -> {
            CartAttemptEntity attempt = invocation.getArgument(0);
            storedAttempt.set(attempt);
            return attempt;
        });
        when(attempts.findByMealPlanIdAndIdempotencyKey(any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(storedAttempt.get()).filter(attempt ->
                        attempt.mealPlanId.equals(invocation.getArgument(0))
                                && attempt.idempotencyKey.equals(invocation.getArgument(1))));
        operations = new PlanOperations(plans, attempts, transitions,
                new CuratedPlanningEngine(new IngredientNormalizer(), new ReferenceRecipeCatalog(),
                        new ReferenceRetailerGateway()), new ObjectMapper(), new PlanningMetrics(new SimpleMeterRegistry()),
                Clock.fixed(Instant.parse("2026-09-21T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void repeatsSamePlanningRequestWithoutCreatingAnotherPlan() {
        UUID guest = UUID.randomUUID();
        PlanView first = operations.create(guest, request("1500.00"), "plan-1");
        PlanView replay = operations.create(guest, request("1500.00"), "plan-1");

        assertThat(replay.id()).isEqualTo(first.id());
    }

    @Test
    void rejectsKeyReuseForDifferentPlanningRequest() {
        UUID guest = UUID.randomUUID();
        operations.create(guest, request("1500.00"), "plan-1");

        assertThatThrownBy(() -> operations.create(guest, request("1600.00"), "plan-1"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void rejectsUnsafeIdempotencyKeys() {
        assertThatThrownBy(() -> operations.create(UUID.randomUUID(), request("1500.00"), "contains spaces"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaysTheSameCartRequestWithoutCallingTheRetailerTwice() {
        UUID guest = UUID.randomUUID();
        PlanView plan = operations.create(guest, request("1500.00"), "plan-1");

        PlanView first = operations.finalizeCart(guest, plan.id(), "cart-1");
        PlanView replay = operations.finalizeCart(guest, plan.id(), "cart-1");

        assertThat(replay.basket().cartReference()).isEqualTo(first.basket().cartReference());
        assertThat(storedAttempt.get().status).isEqualTo("CREATED");
    }

    @Test
    void rejectsCartKeyReuseAfterTheSelectedLinesChange() {
        UUID guest = UUID.randomUUID();
        PlanView plan = operations.create(guest, request("1500.00"), "plan-1");
        operations.finalizeCart(guest, plan.id(), "cart-1");
        UUID lineId = plan.basket().lines().getFirst().id();
        operations.selectLine(guest, plan.id(), lineId, false);

        assertThatThrownBy(() -> operations.finalizeCart(guest, plan.id(), "cart-1"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void neverReturnsAnotherGuestsPlan() {
        UUID owner = UUID.randomUUID();
        PlanView plan = operations.create(owner, request("1500.00"), "plan-1");

        assertThatThrownBy(() -> operations.get(UUID.randomUUID(), plan.id()))
                .isInstanceOf(com.pantry.planning.PlanNotFoundException.class);
    }

    private PlanningRequest request(String budget) {
        return new PlanningRequest(4, 3, new PlanningRequest.MoneyInput(budget, "TRY"), null,
                PlanningRequest.Preference.BALANCED, Set.of(), Set.of(), Set.of(), true);
    }
}
