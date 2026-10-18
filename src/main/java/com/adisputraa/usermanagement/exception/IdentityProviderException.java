package com.adisputraa.usermanagement.exception;

/** Keycloak is unreachable or returned an unexpected error. Mapped to HTTP 502. */
public class IdentityProviderException extends RuntimeException {

    public IdentityProviderException(String message) {
        super(message);
    }

    public IdentityProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
