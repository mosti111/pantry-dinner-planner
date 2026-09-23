package com.pantry.identity.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByIdentityProviderAndExternalSubjectHash(String identityProvider, String externalSubjectHash);
}
