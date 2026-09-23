package com.pantry.identity;

import java.util.UUID;

public record AccountClaimResult(UUID accountId, UUID guestSessionId, boolean accountCreated) {}
