package com.shopease.exception;

/** Requested resource does not exist (or is not visible to the current user) -> HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
