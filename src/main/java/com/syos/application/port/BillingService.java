package com.syos.application.port;

import com.syos.domain.model.Bill;
import com.syos.domain.model.OnlineOrder;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.exception.PaymentException;
import com.syos.service.payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface BillingService {

    Bill startNewBill(TransactionType transactionType);

    void addItemToBill(Bill bill, String itemCode, int quantity)
            throws ItemNotFoundException, DatabaseOperationException;

    Bill finalizeBill(Bill bill, PaymentMethod paymentStrategy, StockManagementService stockManagementService, BigDecimal amountTendered)
            throws DatabaseOperationException, IllegalStateException, InsufficientStockException, ItemNotFoundException, PaymentException;

    void createBill(OnlineOrder onlineOrder, BigDecimal cashTendered, TransactionType transactionType) throws DatabaseOperationException;

    Optional<Bill> getBillDetails(String billSerialNumber, LocalDate billDate) throws DatabaseOperationException;
}