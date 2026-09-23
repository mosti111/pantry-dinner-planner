package com.pantry.identity;

public class AccountClaimConflictException extends RuntimeException {
    public AccountClaimConflictException(String message) {
        super(message);
    }
}
