package com.catalyst.common.error;

import org.springframework.http.HttpStatus;

/** Base class for errors that map directly onto an HTTP status. */
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
