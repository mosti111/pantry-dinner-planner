package com.pantry.planning;

import com.pantry.identity.GuestSession;
import com.pantry.planning.internal.PlanOperations;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PlanService {
    private final PlanOperations operations;

    PlanService(PlanOperations operations) {
        this.operations = operations;
    }

    public PlanView create(GuestSession guest, PlanningRequest request, String idempotencyKey) {
        return operations.create(guest.id(), request, idempotencyKey);
    }

    public PlanView get(GuestSession guest, UUID planId) {
        return operations.get(guest.id(), planId);
    }

    public PlanView selectLine(GuestSession guest, UUID planId, UUID lineId, boolean selected) {
        return operations.selectLine(guest.id(), planId, lineId, selected);
    }

    public PlanView refresh(GuestSession guest, UUID planId) {
        return operations.refresh(guest.id(), planId);
    }

    public PlanView selectProduct(GuestSession guest, UUID planId, UUID lineId, String sku) {
        return operations.selectProduct(guest.id(), planId, lineId, sku);
    }

    public PlanView replaceMeal(GuestSession guest, UUID planId, String slot) {
        return operations.replaceMeal(guest.id(), planId, slot);
    }

    public PlanView simulateRecovery(GuestSession guest, UUID planId, String scenario) {
        return operations.simulateRecovery(guest.id(), planId, scenario);
    }

    public PlanView finalizeCart(GuestSession guest, UUID planId, String idempotencyKey) {
        return operations.finalizeCart(guest.id(), planId, idempotencyKey);
    }
}
