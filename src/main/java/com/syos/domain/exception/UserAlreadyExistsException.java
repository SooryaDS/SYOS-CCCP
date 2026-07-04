package com.syos.domain.exception;

public class UserAlreadyExistsException extends Exception {
    public UserAlreadyExistsException(String message) {
        super(message);
    }

    // FIX: Added constructor to accept a message and a cause (Throwable)
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
