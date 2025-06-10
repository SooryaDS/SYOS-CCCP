package com.syos.service.payment;

import java.math.BigDecimal;

/**
 * A Data Transfer Object to hold the results of a payment operation.
 */
public class PaymentResult {
    private final boolean success;
    private final BigDecimal amountTendered;
    private final BigDecimal changeGiven;
    private final String message;

    public PaymentResult(boolean success, BigDecimal amountTendered, BigDecimal changeGiven, String message) {
        this.success = success;
        this.amountTendered = amountTendered;
        this.changeGiven = changeGiven;
        this.message = message;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public BigDecimal getAmountTendered() {
        return amountTendered;
    }

    public BigDecimal getChangeGiven() {
        return changeGiven;
    }

    public String getMessage() {
        return message;
    }
}