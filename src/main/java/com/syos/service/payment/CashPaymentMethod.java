package com.syos.service.payment;

import com.syos.domain.exception.PaymentException;

import java.math.BigDecimal;

public class CashPaymentMethod implements PaymentMethod {

    // This class is now stateless. The 'cashTendered' field and its constructor are removed.
    // Payment details are passed directly to the processPayment method.

    @Override
    public PaymentResult processPayment(BigDecimal amountToPay, BigDecimal amountTendered) throws PaymentException {
        if (amountTendered == null || amountTendered.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount tendered cannot be null or negative.");
        }
        if (amountToPay == null || amountToPay.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount to pay cannot be null or negative.");
        }

        if (amountTendered.compareTo(amountToPay) < 0) {
            // Payment is insufficient
            throw new PaymentException("Insufficient cash tendered. Amount due: " + amountToPay + ", Tendered: " + amountTendered);
        }

        BigDecimal change = amountTendered.subtract(amountToPay);
        return new PaymentResult(true, amountTendered, change, "Payment successful.");
    }
}
