package com.pantry.identity.internal;

import com.pantry.identity.AccountClaimResult;
import com.pantry.identity.GuestSessionNotFoundException;
import com.pantry.identity.IdentityProviderGateway.VerifiedIdentity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountClaimOperations {
    private final AccountRepository accounts;
    private final GuestSessionRepository guestSessions;
    private final Clock clock;

    @Autowired
    AccountClaimOperations(AccountRepository accounts, GuestSessionRepository guestSessions) {
        this(accounts, guestSessions, Clock.systemUTC());
    }

    AccountClaimOperations(AccountRepository accounts, GuestSessionRepository guestSessions, Clock clock) {
        this.accounts = accounts;
        this.guestSessions = guestSessions;
        this.clock = clock;
    }

    @Transactional
    public AccountClaimResult claimGuest(String guestToken, VerifiedIdentity identity) {
        Instant now = clock.instant();
        GuestSessionEntity guest = guestSessions.findByTokenHash(GuestSessionOperations.hash(guestToken))
                .filter(candidate -> candidate.activeAt(now))
                .orElseThrow(() -> new GuestSessionNotFoundException("Guest session is missing or expired"));
        String provider = identity.provider().trim().toLowerCase(java.util.Locale.ROOT);
        String subjectHash = hash(provider + "\u0000" + identity.subject());
        AccountEntity account = accounts.findByIdentityProviderAndExternalSubjectHash(provider, subjectHash).orElse(null);
        boolean created = account == null;
        if (created) {
            account = accounts.save(new AccountEntity(UUID.randomUUID(), provider, subjectHash, now));
        } else if (!account.active()) {
            throw new com.pantry.identity.AccountClaimConflictException("Account is not active");
        }
        guest.claim(account.id);
        guest.touch(now);
        return new AccountClaimResult(account.id, guest.id, created);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
