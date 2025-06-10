package com.syos.application.usecase;

import com.syos.application.port.BillingService;
import com.syos.application.port.StockManagementService;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.model.Bill;
import com.syos.domain.model.BillItem;
import com.syos.domain.model.Item;
import com.syos.domain.model.OnlineOrder;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.exception.PaymentException;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.BillRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.service.payment.PaymentResult;
import com.syos.service.payment.PaymentStrategy;

import java.math.BigDecimal;
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
    public Bill startNewBill(TransactionType transactionType) throws DatabaseOperationException {
        Bill bill = new Bill(transactionType);
        int nextSerialNumber = billRepository.findNextBillSerialNumberForToday();
        bill.setBillSerialNumber(nextSerialNumber);
        return bill;
    }

    @Override
    public void addItemToBill(Bill bill, String itemCode, int quantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {
        if (bill == null) throw new IllegalArgumentException("Bill cannot be null.");
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be empty.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        Optional<Item> itemOptional = itemRepository.findByItemCode(itemCode);
        if (!itemOptional.isPresent()) throw new ItemNotFoundException("Item with code '" + itemCode + "' not found.");
        Item item = itemOptional.get();
        bill.addItem(item, quantity);
        System.out.println("Item '" + item.getName() + "' (Qty: " + quantity + ") added to bill #" + bill.getBillSerialNumber());
    }

    @Override
    public Bill finalizeBill(Bill bill, PaymentStrategy paymentStrategy, StockManagementService stockManagementService, BigDecimal amountTendered) // <--- ADDED amountTendered
            throws DatabaseOperationException, IllegalStateException, InsufficientStockException, ItemNotFoundException, PaymentException {
        if (bill == null) throw new IllegalArgumentException("Bill cannot be null.");
        if (bill.getBillItems().isEmpty()) throw new IllegalStateException("Cannot finalize an empty bill.");
        if (paymentStrategy == null) throw new IllegalArgumentException("Payment strategy cannot be null.");
        if (amountTendered == null || amountTendered.compareTo(BigDecimal.ZERO) < 0) { // Added validation for amountTendered
            throw new IllegalArgumentException("Amount tendered cannot be null or negative.");
        }

        // 1. Execute payment using the provided strategy
        // Now calling processPayment with amountToPay and amountTendered
        PaymentResult paymentResult = paymentStrategy.processPayment(bill.getFinalTotalAmount(), amountTendered); // <--- CORRECTED CALL

        if (!paymentResult.isSuccess()) {
            throw new PaymentException("Payment failed: " + paymentResult.getMessage());
        }

        // 2. Record the successful payment result on the bill
        bill.recordPayment(paymentResult);

        // 3. Save the bill to the database
        Bill savedBill = billRepository.save(bill);

        // 4. Reduce stock
        if (stockManagementService == null) throw new IllegalStateException("StockManagementService not provided.");
        stockManagementService.reduceStockAfterSale(bill.getBillItems());

        System.out.println("Bill #" + savedBill.getBillSerialNumber() + " finalized, stock updated, and bill saved.");
        return savedBill;
    }

    @Override
    public void createBill(OnlineOrder onlineOrder, BigDecimal cashTendered, TransactionType transactionType) throws DatabaseOperationException {
        // Convert OnlineOrder's items into BillItems for the Bill object
        List<BillItem> billItems = onlineOrder.getItems().stream()
                .map(onlineOrderItem -> {
                    return new BillItem(
                            onlineOrderItem.getItem(),
                            onlineOrderItem.getItemName(),
                            onlineOrderItem.getQuantity(),
                            onlineOrderItem.getPriceAtAddition(),
                            onlineOrderItem.getLineTotal()
                    );
                })
                .collect(Collectors.toList());

        int billSerialNumber = billRepository.findNextBillSerialNumberForToday();

        BigDecimal subTotalFromOnlineOrder = onlineOrder.getCalculatedTotalAmount();
        BigDecimal discountFromOnlineOrder = BigDecimal.ZERO; // Adjust if onlineOrder has a discount field
        BigDecimal finalTotalFromOnlineOrder = onlineOrder.getCalculatedTotalAmount(); // Assuming this is net total

        // Calculate change amount separately to avoid potential compiler confusion
        BigDecimal changeAmount = cashTendered.subtract(finalTotalFromOnlineOrder);

        // Create a new Bill instance from the online order details
        Bill newBill = new Bill(
                billSerialNumber,                           // Use newly generated bill serial number
                onlineOrder.getCreationDate(),              // Order creation date (LocalDateTime)
                transactionType,                            // Transaction type (e.g., ONLINE_SALE)
                String.valueOf(onlineOrder.getOnlineUserId()), // Convert int onlineUserId to String
                null,                                       // employeeId is null for online orders
                subTotalFromOnlineOrder,                    // Sub total amount
                discountFromOnlineOrder,                    // Discount amount
                finalTotalFromOnlineOrder,                  // Final total amount
                cashTendered,                               // Cash tendered
                changeAmount                                // Explicitly use the calculated changeAmount
        );
        newBill.setBillItems(billItems);

        // Save the new bill to the repository
        Bill savedBill = billRepository.save(newBill);
        System.out.println("Online order " + onlineOrder.getOrderId() + " processed and saved as bill #" + savedBill.getBillSerialNumber());
    }

    @Override
    public Optional<Bill> getBillDetails(int billSerialNumber, LocalDate billDate) throws DatabaseOperationException {
        return billRepository.findBySerialNumberAndDate(billSerialNumber, billDate);
    }
}