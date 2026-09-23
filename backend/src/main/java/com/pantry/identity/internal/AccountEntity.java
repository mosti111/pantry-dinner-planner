package com.pantry.identity.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account")
class AccountEntity {
    @Id
    UUID id;
    @Column(length = 80, nullable = false)
    String identityProvider;
    @Column(length = 64, nullable = false)
    String externalSubjectHash;
    @Column(length = 20, nullable = false)
    String status;
    Instant createdAt;
    Instant updatedAt;

    protected AccountEntity() {}

    AccountEntity(UUID id, String identityProvider, String externalSubjectHash, Instant now) {
        this.id = id;
        this.identityProvider = identityProvider;
        this.externalSubjectHash = externalSubjectHash;
        this.status = "ACTIVE";
        this.createdAt = now;
        this.updatedAt = now;
    }

    boolean active() {
        return "ACTIVE".equals(status);
    }
}
