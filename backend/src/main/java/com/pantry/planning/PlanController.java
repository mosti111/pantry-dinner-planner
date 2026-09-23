package com.pantry.planning;

import com.pantry.identity.GuestSession;
import com.pantry.identity.GuestSessionService;
import com.pantry.shared.PantryProperties;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class PlanController {
    private static final String GUEST_COOKIE = "PANTRY_GUEST";
    private final GuestSessionService guestSessions;
    private final PlanService plans;
    private final PantryProperties properties;

    PlanController(GuestSessionService guestSessions, PlanService plans, PantryProperties properties) {
        this.guestSessions = guestSessions;
        this.plans = plans;
        this.properties = properties;
    }

    @PostMapping("/meal-plans")
    ResponseEntity<PlanView> create(
            @CookieValue(GUEST_COOKIE) String token,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlanningRequest request) {
        GuestSession guest = guestSessions.requireActive(token);
        PlanView created = plans.create(guest, request, idempotencyKey);
        return ResponseEntity.created(URI.create("/api/v1/meal-plans/" + created.id())).body(created);
    }

    @GetMapping("/meal-plans/{planId}")
    PlanView get(@CookieValue(GUEST_COOKIE) String token, @PathVariable UUID planId) {
        return plans.get(guestSessions.requireActive(token), planId);
    }

    @PutMapping("/meal-plans/{planId}/basket/lines/{lineId}")
    PlanView selectLine(
            @CookieValue(GUEST_COOKIE) String token,
            @PathVariable UUID planId,
            @PathVariable UUID lineId,
            @RequestBody LineSelection request) {
        return plans.selectLine(guestSessions.requireActive(token), planId, lineId, request.selected());
    }

    @PostMapping("/meal-plans/{planId}/basket/refresh")
    PlanView refresh(@CookieValue(GUEST_COOKIE) String token, @PathVariable UUID planId) {
        return plans.refresh(guestSessions.requireActive(token), planId);
    }

    @PutMapping("/meal-plans/{planId}/basket/lines/{lineId}/product")
    PlanView selectProduct(
            @CookieValue(GUEST_COOKIE) String token,
            @PathVariable UUID planId,
            @PathVariable UUID lineId,
            @Valid @RequestBody ProductSelection request) {
        return plans.selectProduct(guestSessions.requireActive(token), planId, lineId, request.sku());
    }

    @PostMapping("/meal-plans/{planId}/meals/{slot}/replace")
    PlanView replaceMeal(
            @CookieValue(GUEST_COOKIE) String token,
            @PathVariable UUID planId,
            @PathVariable String slot) {
        return plans.replaceMeal(guestSessions.requireActive(token), planId, slot);
    }

    @PostMapping("/meal-plans/{planId}/basket/recovery-demo")
    PlanView simulateRecovery(
            @CookieValue(GUEST_COOKIE) String token,
            @PathVariable UUID planId,
            @Valid @RequestBody RecoverySimulation request) {
        if (!properties.demoMode()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return plans.simulateRecovery(guestSessions.requireActive(token), planId, request.scenario());
    }

    @PostMapping("/meal-plans/{planId}/basket/finalize")
    PlanView finalizeCart(
            @CookieValue(GUEST_COOKIE) String token,
            @PathVariable UUID planId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return plans.finalizeCart(guestSessions.requireActive(token), planId, idempotencyKey);
    }

    record LineSelection(boolean selected) {}
    record ProductSelection(@jakarta.validation.constraints.NotBlank String sku) {}
    record RecoverySimulation(@jakarta.validation.constraints.NotBlank String scenario) {}
}
