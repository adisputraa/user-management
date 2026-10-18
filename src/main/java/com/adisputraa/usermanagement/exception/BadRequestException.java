package com.adisputraa.usermanagement.exception;

/** The request is syntactically valid but asks for something unsupported. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
