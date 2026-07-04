package com.syos.application.usecase;

import com.syos.application.port.BillingService;
import com.syos.application.port.StockManagementService;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.model.Bill;
import com.syos.domain.model.BillItem;
import com.syos.domain.model.OnlineOrder;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.exception.PaymentException;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.BillRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.service.payment.PaymentMethod;
import com.syos.service.payment.PaymentResult;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BillingServiceImplementation implements BillingService {

    private final ItemRepository itemRepository;
    private final BillRepository billRepository;

    public BillingServiceImplementation(ItemRepository itemRepository, BillRepository billRepository) {
        this.itemRepository = itemRepository;
        this.billRepository = billRepository;
    }

    @Override
    public Bill startNewBill(TransactionType transactionType) {
        if (transactionType == null) throw new IllegalArgumentException("Transaction type cannot be null.");
        return new Bill(transactionType);
    }

    @Override
    public void addItemToBill(Bill bill, String itemCode, int quantity) throws ItemNotFoundException, DatabaseOperationException {
        if (bill == null) throw new IllegalArgumentException("Bill cannot be null.");
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be null.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");

        var itemOpt = itemRepository.findByItemCode(itemCode);
        if (itemOpt.isEmpty()) throw new ItemNotFoundException("Item not found: " + itemCode);

        bill.addItem(itemOpt.get(), quantity);
    }

    @Override
    public Bill finalizeBill(Bill bill,
                             PaymentMethod paymentStrategy,
                             StockManagementService stockManagementService,
                             BigDecimal amountTendered)
            throws DatabaseOperationException, IllegalStateException, InsufficientStockException, ItemNotFoundException, PaymentException {

        if (bill == null || paymentStrategy == null || stockManagementService == null)
            throw new IllegalArgumentException("Arguments cannot be null.");
        if (bill.getBillItems().isEmpty()) throw new IllegalStateException("Cannot finalize empty bill.");
        if (amountTendered == null || amountTendered.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount tendered cannot be null or negative.");

        // 1️ Process payment
        PaymentResult paymentResult = paymentStrategy.processPayment(bill.getFinalTotalAmount(), amountTendered);
        if (!paymentResult.isSuccess()) throw new PaymentException("Payment failed: " + paymentResult.getMessage());
        bill.recordPayment(paymentResult);

        // 2️ Validate items exist
        for (BillItem billItem : bill.getBillItems()) {
            if (itemRepository.findByItemCode(billItem.getItem().getItemCode()).isEmpty())
                throw new ItemNotFoundException("Item missing: " + billItem.getItem().getItemCode());
        }

        // 3️ Save bill in a transaction + reduce stock
        try (Connection conn = com.syos.adapter.out.util.DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Generate next serial number
                LocalDate today = LocalDate.now();
                Optional<Bill> lastBill = billRepository.findTopByBillDateOrderBySerialNumberDesc(today);
                bill.setBillSerialNumber(generateNextSerial(lastBill.map(Bill::getBillSerialNumber).orElse(null), today));

                // Save bill
                Bill savedBill = billRepository.save(bill, conn);

                // Reduce stock
                stockManagementService.reduceShelfStockAfterPOSSale(savedBill.getBillItems());

                conn.commit();
                return savedBill;

            } catch (Exception e) {
                conn.rollback();
                throw new DatabaseOperationException("Finalize bill failed: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database connection failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void createBill(OnlineOrder onlineOrder, BigDecimal cashTendered, TransactionType transactionType) throws DatabaseOperationException {
        List<BillItem> billItems = onlineOrder.getItems().stream()
                .map(o -> new BillItem(o.getItem(), o.getItem().getName(), o.getQuantity(), o.getPriceAtAddition(), o.getLineTotal()))
                .collect(Collectors.toList());

        Bill newBill = new Bill(transactionType);
        newBill.setBillDate(onlineOrder.getCreationDate());
        newBill.setUserId(onlineOrder.getOnlineUserId());
        newBill.setEmployeeId(null);
        newBill.setSubTotalAmount(onlineOrder.getCalculatedTotalAmount());
        newBill.setDiscountAmount(onlineOrder.getDiscountAmount());
        newBill.setFinalTotalAmount(onlineOrder.getCalculatedTotalAmount().subtract(onlineOrder.getDiscountAmount()));
        newBill.recordPayment(new PaymentResult(true, cashTendered, cashTendered.subtract(newBill.getFinalTotalAmount()), "Online payment"));

        // Generate serial
        LocalDate today = LocalDate.now();
        Optional<Bill> lastBill = billRepository.findTopByBillDateOrderBySerialNumberDesc(today);
        newBill.setBillSerialNumber(generateNextSerial(lastBill.map(Bill::getBillSerialNumber).orElse(null), today));

        newBill.setBillItems(billItems);
        billRepository.save(newBill);
    }

    @Override
    public Optional<Bill> getBillDetails(String billSerialNumber, LocalDate billDate) throws DatabaseOperationException {
        if (billSerialNumber == null || billSerialNumber.trim().isEmpty()) throw new IllegalArgumentException("Bill serial number cannot be null.");
        if (billDate == null) throw new IllegalArgumentException("Bill date cannot be null.");
        return billRepository.findBySerialNumberAndDate(billSerialNumber, billDate);
    }

    // ======= Helper =======
    String generateNextSerial(String lastSerial, LocalDate today) {
        // Example: YYYYMMDDNNNN
        String datePart = today.toString().replaceAll("-", ""); // YYYYMMDD
        int nextNumber = 1;
        if (lastSerial != null && lastSerial.startsWith(datePart)) {
            String numPart = lastSerial.substring(8);
            nextNumber = Integer.parseInt(numPart) + 1;
        }
        return datePart + String.format("%04d", nextNumber);
    }
}
