package com.syos.application.port;

import com.syos.domain.model.Bill;
import com.syos.domain.model.OnlineOrder;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.exception.PaymentException;
import com.syos.service.payment.PaymentStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;


public interface BillingService {
    Bill startNewBill(TransactionType transactionType) throws DatabaseOperationException;

    void addItemToBill(Bill bill, String itemCode, int quantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException;

    /**
     * Finalizes the bill using a given payment strategy.
     * @param bill The bill to finalize.
     * @param paymentStrategy The payment method to use.
     * @param stockManagementService The service to handle stock reduction.
     * @param amountTendered The amount of money tendered by the customer.
     * @return The finalized and saved Bill.
     * @throws DatabaseOperationException if saving fails.
     * @throws IllegalStateException if bill cannot be finalized.
     * @throws PaymentException if the payment strategy fails.
     */
    Bill finalizeBill(Bill bill, PaymentStrategy paymentStrategy, StockManagementService stockManagementService, BigDecimal amountTendered) // <--- ADDED amountTendered
            throws DatabaseOperationException, IllegalStateException, InsufficientStockException, ItemNotFoundException, PaymentException;

    void createBill(OnlineOrder onlineOrder, BigDecimal cashTendered, TransactionType transactionType) throws DatabaseOperationException;

    Optional<Bill> getBillDetails(int billSerialNumber, LocalDate billDate) throws DatabaseOperationException;
}