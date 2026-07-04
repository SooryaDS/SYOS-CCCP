package com.syos.service.payment;

import com.syos.domain.exception.PaymentException;
import java.math.BigDecimal;

public interface PaymentMethod {
    PaymentResult processPayment(BigDecimal amountToPay, BigDecimal amountTendered) throws PaymentException;
}
