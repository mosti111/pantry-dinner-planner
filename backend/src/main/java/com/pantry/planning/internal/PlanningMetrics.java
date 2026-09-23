package com.pantry.planning.internal;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
class PlanningMetrics {
    private final Counter plansCreated;
    private final Counter planningReplays;
    private final Counter cartsCreated;
    private final Counter cartReplays;
    private final Counter idempotencyConflicts;

    PlanningMetrics(MeterRegistry registry) {
        plansCreated = registry.counter("pantry.plans.created");
        planningReplays = registry.counter("pantry.plans.idempotency.replays");
        cartsCreated = registry.counter("pantry.carts.created");
        cartReplays = registry.counter("pantry.carts.idempotency.replays");
        idempotencyConflicts = registry.counter("pantry.idempotency.conflicts");
    }

    void planCreated() { plansCreated.increment(); }
    void planningReplay() { planningReplays.increment(); }
    void cartCreated() { cartsCreated.increment(); }
    void cartReplay() { cartReplays.increment(); }
    void idempotencyConflict() { idempotencyConflicts.increment(); }
}
