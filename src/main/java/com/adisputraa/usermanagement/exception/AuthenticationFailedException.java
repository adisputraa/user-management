package com.adisputraa.usermanagement.exception;

/** Keycloak rejected the credentials or the refresh token. Mapped to HTTP 401. */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
