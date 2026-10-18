package com.adisputraa.usermanagement.exception;

/** The request conflicts with the current state, e.g. a duplicate username. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
