
package com.syos.domain.exception;

public class OrderProcessingException extends Exception {

    public OrderProcessingException() {
        super();
    }

    public OrderProcessingException(String message) {
        super(message);
    }

    public OrderProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrderProcessingException(Throwable cause) {
        super(cause);
    }
}
