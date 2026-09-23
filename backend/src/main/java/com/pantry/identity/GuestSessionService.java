package com.pantry.identity;

import com.pantry.identity.internal.GuestSessionOperations;
import com.pantry.shared.PantryProperties;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class GuestSessionService {
    private final GuestSessionOperations operations;
    private final PantryProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    GuestSessionService(GuestSessionOperations operations, PantryProperties properties) {
        this.operations = operations;
        this.properties = properties;
    }

    public IssuedGuestSession create() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        GuestSession session = operations.create(token, properties.guestSessionTtl());
        return new IssuedGuestSession(session, token);
    }

    public GuestSession requireActive(String token) {
        return operations.requireActive(token);
    }

    public void delete(String token) {
        operations.delete(token);
    }

    public record IssuedGuestSession(GuestSession session, String token) {}
}
