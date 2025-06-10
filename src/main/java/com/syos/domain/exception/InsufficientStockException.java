package com.syos.domain.exception;

public class InsufficientStockException extends Exception {

    public InsufficientStockException() {
        super();
    }

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }

    public InsufficientStockException(Throwable cause) {
        super(cause);
    }
}
