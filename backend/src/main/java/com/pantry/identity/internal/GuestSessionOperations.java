package com.pantry.identity.internal;

import com.pantry.identity.GuestSession;
import com.pantry.identity.GuestSessionNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GuestSessionOperations {
    private final GuestSessionRepository repository;
    private final Clock clock;

    @Autowired
    GuestSessionOperations(GuestSessionRepository repository) {
        this(repository, Clock.systemUTC());
    }

    GuestSessionOperations(GuestSessionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public GuestSession create(String token, Duration ttl) {
        Instant now = clock.instant();
        GuestSessionEntity entity = new GuestSessionEntity(UUID.randomUUID(), hash(token), now, now.plus(ttl));
        return repository.save(entity).toPublic();
    }

    @Transactional
    public GuestSession requireActive(String token) {
        Instant now = clock.instant();
        GuestSessionEntity entity = repository.findByTokenHash(hash(token))
                .filter(candidate -> candidate.activeAt(now))
                .orElseThrow(() -> new GuestSessionNotFoundException("Guest session is missing or expired"));
        entity.touch(now);
        return entity.toPublic();
    }

    @Transactional
    public void delete(String token) {
        Instant now = clock.instant();
        GuestSessionEntity entity = repository.findByTokenHash(hash(token))
                .filter(candidate -> candidate.activeAt(now))
                .orElseThrow(() -> new GuestSessionNotFoundException("Guest session is missing or expired"));
        repository.delete(entity);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
