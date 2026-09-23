package com.pantry.planning.internal;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pantry.planning.PlanView;
import com.pantry.planning.PlanNotFoundException;
import com.pantry.planning.IdempotencyConflictException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import com.pantry.planning.PlanningRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanOperations {
    private final MealPlanRepository repository;
    private final CartAttemptRepository cartAttempts;
    private final PlanTransitionRepository transitions;
    private final CuratedPlanningEngine engine;
    private final ObjectMapper objectMapper;
    private final PlanningMetrics metrics;
    private final Clock clock;

    @Autowired
    PlanOperations(MealPlanRepository repository, CartAttemptRepository cartAttempts,
            PlanTransitionRepository transitions, CuratedPlanningEngine engine, ObjectMapper objectMapper,
            PlanningMetrics metrics) {
        this(repository, cartAttempts, transitions, engine, objectMapper, metrics, Clock.systemUTC());
    }

    PlanOperations(MealPlanRepository repository, CartAttemptRepository cartAttempts,
            PlanTransitionRepository transitions, CuratedPlanningEngine engine, ObjectMapper objectMapper,
            PlanningMetrics metrics, Clock clock) {
        this.repository = repository;
        this.cartAttempts = cartAttempts;
        this.transitions = transitions;
        this.engine = engine;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public PlanView create(UUID guestSessionId, PlanningRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        JsonNode requestTree = tree(request);
        String fingerprint = fingerprint(requestTree);
        MealPlanEntity replay = repository.findByGuestSessionIdAndIdempotencyKey(guestSessionId, idempotencyKey).orElse(null);
        if (replay != null) {
            if (!replay.requestFingerprint.equals(fingerprint)) {
                metrics.idempotencyConflict();
                throw new IdempotencyConflictException("Idempotency-Key was already used for a different request");
            }
            metrics.planningReplay();
            return view(replay.planPayload);
        }
        UUID planId = UUID.randomUUID();
        Instant now = clock.instant();
        PlanView view = engine.create(planId, request, now);
        repository.save(new MealPlanEntity(planId, guestSessionId, idempotencyKey, fingerprint, requestTree, tree(view), now));
        transitions.save(new PlanTransitionEntity(planId, null, view.status(), "PLAN_CREATED", view.version(), now));
        metrics.planCreated();
        return view;
    }

    @Transactional(readOnly = true)
    public PlanView get(UUID guestSessionId, UUID planId) {
        return view(requireOwned(guestSessionId, planId).planPayload);
    }

    @Transactional
    public PlanView selectLine(UUID guestSessionId, UUID planId, UUID lineId, boolean selected) {
        MealPlanEntity entity = requireOwned(guestSessionId, planId);
        PlanView updated = engine.updateLineSelection(view(entity.planPayload), lineId, selected, clock.instant());
        update(entity, updated, "PANTRY_SELECTION_CHANGED");
        return updated;
    }

    @Transactional
    public PlanView refresh(UUID guestSessionId, UUID planId) {
        MealPlanEntity entity = requireOwned(guestSessionId, planId);
        PlanView updated = engine.refresh(view(entity.planPayload), clock.instant());
        update(entity, updated, "OFFERS_REFRESHED");
        return updated;
    }

    @Transactional
    public PlanView selectProduct(UUID guestSessionId, UUID planId, UUID lineId, String sku) {
        MealPlanEntity entity = requireOwned(guestSessionId, planId);
        PlanView updated = engine.selectProduct(view(entity.planPayload), lineId, sku, clock.instant());
        update(entity, updated, "PRODUCT_CHANGED");
        return updated;
    }

    @Transactional
    public PlanView replaceMeal(UUID guestSessionId, UUID planId, String slot) {
        MealPlanEntity entity = requireOwned(guestSessionId, planId);
        PlanningRequest request = objectMapper.convertValue(entity.requestPayload, PlanningRequest.class);
        PlanView updated = engine.replaceMeal(view(entity.planPayload), request, slot, clock.instant());
        update(entity, updated, "MEAL_REPLACED");
        return updated;
    }

    @Transactional
    public PlanView simulateRecovery(UUID guestSessionId, UUID planId, String scenario) {
        MealPlanEntity entity = requireOwned(guestSessionId, planId);
        PlanningRequest request = objectMapper.convertValue(entity.requestPayload, PlanningRequest.class);
        PlanView updated = engine.simulateRecovery(view(entity.planPayload), request, scenario, clock.instant());
        update(entity, updated, "RECOVERY_SIMULATED");
        return updated;
    }

    @Transactional
    public PlanView finalizeCart(UUID guestSessionId, UUID planId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        MealPlanEntity entity = requireOwned(guestSessionId, planId);
        PlanView current = view(entity.planPayload);
        String requestFingerprint = cartFingerprint(current);
        CartAttemptEntity replay = cartAttempts.findByMealPlanIdAndIdempotencyKey(planId, idempotencyKey).orElse(null);
        if (replay != null) {
            if (!replay.requestFingerprint.equals(requestFingerprint)) {
                metrics.idempotencyConflict();
                throw new IdempotencyConflictException("Idempotency-Key was already used for a different cart request");
            }
            metrics.cartReplay();
            return view(replay.responsePayload);
        }
        PlanView result = "CART_READY".equals(current.status())
                ? current
                : engine.finalizeCart(engine.refresh(current, clock.instant()), idempotencyKey, clock.instant());
        if (!"CART_READY".equals(current.status())) {
            update(entity, result, "CART_CREATED");
        }
        cartAttempts.save(new CartAttemptEntity(
                UUID.randomUUID(), planId, idempotencyKey, requestFingerprint,
                result.basket().status(), result.basket().cartReference(), tree(result), clock.instant()));
        metrics.cartCreated();
        return result;
    }

    private String cartFingerprint(PlanView plan) {
        List<CartRequestLine> lines = plan.basket().lines().stream()
                .filter(PlanView.BasketLineView::selected)
                .map(line -> new CartRequestLine(line.sku(), line.packageCount()))
                .sorted(Comparator.comparing(CartRequestLine::sku))
                .toList();
        return fingerprint(tree(lines));
    }

    private void validateIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 128 || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("Idempotency-Key must be 1-128 URL-safe characters");
        }
    }

    private String fingerprint(JsonNode value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private MealPlanEntity requireOwned(UUID guestSessionId, UUID planId) {
        return repository.findByIdAndGuestSessionId(planId, guestSessionId)
                .orElseThrow(() -> new PlanNotFoundException("Plan was not found"));
    }

    private void update(MealPlanEntity entity, PlanView updated, String reason) {
        String previous = entity.status;
        entity.update(updated.status(), tree(updated), updated.updatedAt());
        transitions.save(new PlanTransitionEntity(
                entity.id, previous, updated.status(), reason, updated.version(), updated.updatedAt()));
    }

    private JsonNode tree(Object value) {
        return objectMapper.valueToTree(value);
    }

    private PlanView view(JsonNode node) {
        return objectMapper.convertValue(node, PlanView.class);
    }

    private record CartRequestLine(String sku, int packageCount) {}
}
