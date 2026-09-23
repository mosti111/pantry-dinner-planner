package com.pantry.identity.internal;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GuestSessionRepository extends JpaRepository<GuestSessionEntity, UUID> {
    Optional<GuestSessionEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from GuestSessionEntity session where session.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
