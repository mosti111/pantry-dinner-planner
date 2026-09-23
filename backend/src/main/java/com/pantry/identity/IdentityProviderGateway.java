package com.pantry.identity;

/**
 * Boundary for a future OIDC provider adapter. Only an adapter that has verified
 * the provider credential may create the value passed to the account-claim flow.
 */
public interface IdentityProviderGateway {
    VerifiedIdentity verify(String providerCredential);

    record VerifiedIdentity(String provider, String subject) {
        public VerifiedIdentity {
            if (provider == null || provider.isBlank() || provider.length() > 80) {
                throw new IllegalArgumentException("Identity provider is invalid");
            }
            if (subject == null || subject.isBlank() || subject.length() > 512) {
                throw new IllegalArgumentException("Identity subject is invalid");
            }
        }
    }
}
