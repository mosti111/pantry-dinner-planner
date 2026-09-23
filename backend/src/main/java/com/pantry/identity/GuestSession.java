package com.pantry.identity;

import java.time.Instant;
import java.util.UUID;

public record GuestSession(UUID id, Instant expiresAt) {
}
