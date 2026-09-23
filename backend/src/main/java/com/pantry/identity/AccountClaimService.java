package com.pantry.identity;

import com.pantry.identity.IdentityProviderGateway.VerifiedIdentity;
import com.pantry.identity.internal.AccountClaimOperations;
import org.springframework.stereotype.Service;

/**
 * Account-ready seam kept deliberately off HTTP until a real identity provider
 * and its token validation policy have been selected.
 */
@Service
public class AccountClaimService {
    private final AccountClaimOperations operations;

    AccountClaimService(AccountClaimOperations operations) {
        this.operations = operations;
    }

    public AccountClaimResult claimGuest(String guestToken, VerifiedIdentity verifiedIdentity) {
        return operations.claimGuest(guestToken, verifiedIdentity);
    }
}
