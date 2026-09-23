package com.pantry.identity;

public class GuestSessionNotFoundException extends RuntimeException {
    public GuestSessionNotFoundException(String message) {
        super(message);
    }
}
