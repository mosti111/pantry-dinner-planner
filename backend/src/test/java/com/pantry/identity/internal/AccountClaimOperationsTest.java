package com.pantry.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pantry.identity.AccountClaimConflictException;
import com.pantry.identity.IdentityProviderGateway.VerifiedIdentity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AccountClaimOperationsTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-22T09:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsAnAccountWithoutPersistingTheExternalSubject() {
        AccountRepository accounts = mock(AccountRepository.class);
        GuestSessionRepository sessions = mock(GuestSessionRepository.class);
        GuestSessionEntity guest = guest("guest-token");
        AtomicReference<AccountEntity> saved = new AtomicReference<>();
        when(sessions.findByTokenHash(GuestSessionOperations.hash("guest-token"))).thenReturn(Optional.of(guest));
        when(accounts.findByIdentityProviderAndExternalSubjectHash(anyString(), anyString())).thenReturn(Optional.empty());
        when(accounts.save(any())).thenAnswer(invocation -> {
            AccountEntity account = invocation.getArgument(0);
            saved.set(account);
            return account;
        });

        var result = new AccountClaimOperations(accounts, sessions, CLOCK)
                .claimGuest("guest-token", new VerifiedIdentity("OIDC", "provider-secret-subject"));

        assertThat(result.accountCreated()).isTrue();
        assertThat(guest.claimedByAccountId).isEqualTo(result.accountId());
        assertThat(saved.get().identityProvider).isEqualTo("oidc");
        assertThat(saved.get().externalSubjectHash).hasSize(64).doesNotContain("provider-secret-subject");
    }

    @Test
    void rejectsClaimingTheSameGuestForAnotherAccount() {
        AccountRepository accounts = mock(AccountRepository.class);
        GuestSessionRepository sessions = mock(GuestSessionRepository.class);
        GuestSessionEntity guest = guest("guest-token");
        guest.claim(UUID.randomUUID());
        AccountEntity other = new AccountEntity(UUID.randomUUID(), "oidc", "a".repeat(64), CLOCK.instant());
        when(sessions.findByTokenHash(anyString())).thenReturn(Optional.of(guest));
        when(accounts.findByIdentityProviderAndExternalSubjectHash(anyString(), anyString()))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> new AccountClaimOperations(accounts, sessions, CLOCK)
                .claimGuest("guest-token", new VerifiedIdentity("oidc", "another-subject")))
                .isInstanceOf(AccountClaimConflictException.class);
    }

    @Test
    void replayingAClaimForTheSameVerifiedAccountIsIdempotent() {
        AccountRepository accounts = mock(AccountRepository.class);
        GuestSessionRepository sessions = mock(GuestSessionRepository.class);
        GuestSessionEntity guest = guest("guest-token");
        AccountEntity existing = new AccountEntity(UUID.randomUUID(), "oidc", "a".repeat(64), CLOCK.instant());
        when(sessions.findByTokenHash(anyString())).thenReturn(Optional.of(guest));
        when(accounts.findByIdentityProviderAndExternalSubjectHash(anyString(), anyString()))
                .thenReturn(Optional.of(existing));
        AccountClaimOperations operations = new AccountClaimOperations(accounts, sessions, CLOCK);

        var first = operations.claimGuest("guest-token", new VerifiedIdentity("oidc", "same-subject"));
        var replay = operations.claimGuest("guest-token", new VerifiedIdentity("oidc", "same-subject"));

        assertThat(first.accountCreated()).isFalse();
        assertThat(replay.accountId()).isEqualTo(first.accountId());
    }

    private GuestSessionEntity guest(String token) {
        return new GuestSessionEntity(UUID.randomUUID(), GuestSessionOperations.hash(token), CLOCK.instant(),
                CLOCK.instant().plusSeconds(3600));
    }
}
