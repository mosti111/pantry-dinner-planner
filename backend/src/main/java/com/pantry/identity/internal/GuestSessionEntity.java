package com.pantry.identity.internal;

import com.pantry.identity.GuestSession;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guest_session")
class GuestSessionEntity {
    @Id
    UUID id;
    @Column(length = 64, nullable = false)
    String tokenHash;
    @Column(length = 20, nullable = false)
    String status;
    Instant createdAt;
    Instant lastSeenAt;
    Instant expiresAt;
    UUID claimedByAccountId;
    @Version
    long version;

    protected GuestSessionEntity() {}

    GuestSessionEntity(UUID id, String tokenHash, Instant now, Instant expiresAt) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.status = "ACTIVE";
        this.createdAt = now;
        this.lastSeenAt = now;
        this.expiresAt = expiresAt;
    }

    GuestSession toPublic() {
        return new GuestSession(id, expiresAt);
    }

    boolean activeAt(Instant now) {
        return "ACTIVE".equals(status) && expiresAt.isAfter(now);
    }

    void touch(Instant now) {
        lastSeenAt = now;
    }

    void claim(UUID accountId) {
        if (claimedByAccountId != null && !claimedByAccountId.equals(accountId)) {
            throw new com.pantry.identity.AccountClaimConflictException(
                    "Guest session has already been claimed by another account");
        }
        claimedByAccountId = accountId;
    }

}
