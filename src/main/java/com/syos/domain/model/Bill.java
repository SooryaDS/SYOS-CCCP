package com.syos.domain.model;

import com.syos.domain.enums.TransactionType;
import com.syos.service.payment.PaymentResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Bill {
    private int id; // Database primary key
    private String billSerialNumber; //YYYYMMDDNNNN format
    private LocalDateTime billDate;
    private TransactionType transactionType;
    private Integer userId; // Changed from String to Integer for consistency with OnlineUser
    private String employeeId; // Keeping as String for now, adjust if you have an Employee class with int ID
    private List<BillItem> billItems;
    private BigDecimal subTotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalTotalAmount;
    private PaymentResult paymentResult;

    // Default constructor (no-arg constructor for frameworks)
    public Bill() {
        this.billItems = new ArrayList<>();
        this.subTotalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.finalTotalAmount = BigDecimal.ZERO;
    }

    // Constructor for creating a new Bill (e.g., at the start of a transaction)
    public Bill(TransactionType transactionType) {
        this(); // Call default constructor to initialize collections and amounts
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type cannot be null.");
        }
        this.billDate = LocalDateTime.now();
        this.transactionType = transactionType;
    }

    /**
     * Full constructor for Bill, typically used when retrieving from a database.
     * @param id The unique ID of the bill.
     * @param billSerialNumber The generated serial number for the bill.
     * @param billDate The date and time the bill was created.
     * @param transactionType The type of transaction (e.g., SALE, RETURN).
     * @param userId The ID of the online user associated with the bill (can be null for walk-ins).
     * @param employeeId The ID of the employee who processed the bill (can be null if not applicable).
     * @param subTotalAmount The total amount before discounts.
     * @param discountAmount The total discount applied.
     * @param finalTotalAmount The final amount after discounts.
     * @param paymentResult The result of the payment for this bill.
     */
    public Bill(int id, String billSerialNumber, LocalDateTime billDate, TransactionType transactionType,
                Integer userId, String employeeId, BigDecimal subTotalAmount, BigDecimal discountAmount,
                BigDecimal finalTotalAmount, PaymentResult paymentResult) {
        this(); // Call default constructor to initialize collections and amounts
        if (billSerialNumber == null || billSerialNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Bill serial number cannot be null or empty.");
        }
        if (billDate == null) {
            throw new IllegalArgumentException("Bill date cannot be null.");
        }
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type cannot be null.");
        }
        if (subTotalAmount == null || subTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Sub total amount cannot be null or negative.");
        }
        if (finalTotalAmount == null || finalTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Final total amount cannot be null or negative.");
        }

        this.id = id;
        this.billSerialNumber = billSerialNumber;
        this.billDate = billDate;
        this.transactionType = transactionType;
        this.userId = userId;
        this.employeeId = employeeId;
        this.subTotalAmount = subTotalAmount;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.finalTotalAmount = finalTotalAmount;
        this.paymentResult = paymentResult; // Directly assign PaymentResult object
    }

    // REMOVED: The problematic overloaded constructor.
    // public Bill(int id, String billSerialNumber, LocalDateTime billDate, TransactionType transactionType, String userId, String employeeId, BigDecimal subTotalAmount, BigDecimal discountAmount, BigDecimal finalTotalAmount, BigDecimal cashTendered, BigDecimal changeAmount) {
    //     this(id, billSerialNumber, billDate, transactionType,
    //             (userId != null && !userId.isEmpty()) ? Integer.parseInt(userId) : null, // Convert userId String to Integer
    //             employeeId, subTotalAmount, discountAmount, finalTotalAmount,
    //             (cashTendered != null) ? new PaymentResult(true, cashTendered, changeAmount, "Payment recorded from DB.") : null);
    // }


    public void addItem(Item item, int quantity) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");

        Optional<BillItem> existingItem = billItems.stream()
                .filter(bi -> bi.getItem().getItemCode().equals(item.getItemCode()))
                .findFirst();

        if (existingItem.isPresent()) {
            BillItem currentBillItem = existingItem.get();
            currentBillItem.setQuantityPurchased(currentBillItem.getQuantityPurchased() + quantity);
        } else {
            BigDecimal pricePerUnitAtSale = item.getUnitPrice();
            BigDecimal totalPriceForItem = pricePerUnitAtSale.multiply(BigDecimal.valueOf(quantity));
            billItems.add(new BillItem(item, item.getName(), quantity, pricePerUnitAtSale, totalPriceForItem));
        }
        recalculateTotals();
    }

    public void addBillItem(BillItem billItem) {
        if (billItem == null) throw new IllegalArgumentException("Bill item cannot be null.");
        Optional<BillItem> existingItem = billItems.stream()
                .filter(bi -> bi.getItem().getItemCode().equals(billItem.getItem().getItemCode()))
                .findFirst();

        if (existingItem.isPresent()) {
            BillItem currentBillItem = existingItem.get();
            currentBillItem.setQuantityPurchased(currentBillItem.getQuantityPurchased() + billItem.getQuantityPurchased());
        } else {
            this.billItems.add(billItem);
        }
        recalculateTotals();
    }

    public boolean removeBillItem(String itemCode) {
        boolean removed = this.billItems.removeIf(billItem -> billItem.getItem().getItemCode().equals(itemCode));
        if (removed) {
            recalculateTotals();
        }
        return removed;
    }

    public boolean updateBillItemQuantity(String itemCode, int newQuantity) {
        if (newQuantity < 0) throw new IllegalArgumentException("New quantity cannot be negative.");

        if (newQuantity == 0) {
            return removeBillItem(itemCode);
        }

        for (BillItem billItem : billItems) {
            if (billItem.getItem().getItemCode().equals(itemCode)) {
                billItem.setQuantityPurchased(newQuantity);
                recalculateTotals();
                return true;
            }
        }
        return false; // Item not found in the bill
    }


    private void recalculateTotals() {
        this.subTotalAmount = billItems.stream()
                .map(BillItem::getTotalPriceForItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.finalTotalAmount = this.subTotalAmount.subtract(this.discountAmount);
        if (this.finalTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalTotalAmount = BigDecimal.ZERO;
        }
    }

    public void recordPayment(PaymentResult paymentResult) {
        this.paymentResult = paymentResult; // Can be null to clear
    }

    public String generateReceipt() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        sb.append("========================================\n");
        sb.append("           SYOS - OFFICIAL BILL\n");
        sb.append("========================================\n");
        sb.append(String.format("Bill No.: %s\n", billSerialNumber));
        sb.append(String.format("Date    : %s\n", billDate.format(formatter)));
        sb.append(String.format("Type    : %s\n", transactionType));
        if (employeeId != null && !employeeId.isEmpty()) sb.append(String.format("Cashier : %s\n", employeeId));
        // Use userId directly as Integer
        if (userId != null) sb.append(String.format("Customer: %d\n", userId));
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-20s | %3s | %8s | %8s\n", "Item Name", "Qty", "Unit Pr.", "Total Pr."));
        sb.append("----------------------------------------\n");
        for (BillItem bi : billItems) {
            sb.append(String.format("%-20s | %3d | %8.2f | %8.2f\n",
                    bi.getItemNameAtSale(), bi.getQuantityPurchased(), bi.getPricePerUnitAtSale(), bi.getTotalPriceForItem()));
        }
        sb.append("----------------------------------------\n");
        sb.append(String.format("%34s %8.2f\n", "Subtotal:", getSubTotalAmount()));
        if (getDiscountAmount() != null && getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("%34s %8.2f\n", "Discount:", getDiscountAmount().negate()));
        }
        sb.append(String.format("%34s %8.2f\n", "TOTAL DUE:", getFinalTotalAmount()));
        sb.append("----------------------------------------\n");
        if (paymentResult != null && paymentResult.isSuccess()) {
            sb.append(String.format("%34s %8.2f\n", "Cash Tendered:", paymentResult.getAmountTendered()));
            sb.append(String.format("%34s %8.2f\n", "Change:", paymentResult.getChangeGiven()));
        }
        sb.append("========================================\n");
        sb.append("       Thank you! Please come again!\n");
        sb.append("========================================\n");
        return sb.toString();
    }

    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBillSerialNumber() { return billSerialNumber; }
    public void setBillSerialNumber(String billSerialNumber) {
        if (billSerialNumber == null || billSerialNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Bill serial number cannot be null or empty.");
        }
        this.billSerialNumber = billSerialNumber;
    }

    public LocalDateTime getBillDate() { return billDate; }
    public void setBillDate(LocalDateTime billDate) {
        if (billDate == null) {
            throw new IllegalArgumentException("Bill date cannot be null.");
        }
        this.billDate = billDate;
    }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) {
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type cannot be null.");
        }
        this.transactionType = transactionType;
    }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public List<BillItem> getBillItems() { return Collections.unmodifiableList(billItems); }
    public void setBillItems(List<BillItem> billItems) {
        this.billItems = (billItems != null) ? new ArrayList<>(billItems) : new ArrayList<>();
        recalculateTotals();
    }

    public BigDecimal getSubTotalAmount() { return subTotalAmount; }
    public void setSubTotalAmount(BigDecimal subTotalAmount) {
        if (subTotalAmount == null || subTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Sub total amount cannot be null or negative.");
        }
        this.subTotalAmount = subTotalAmount;
        recalculateTotals();
    }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount amount cannot be null or negative.");
        }
        this.discountAmount = discountAmount;
        recalculateTotals();
    }
    public BigDecimal getFinalTotalAmount() { return finalTotalAmount; }
    public void setFinalTotalAmount(BigDecimal finalTotalAmount) {
        if (finalTotalAmount == null || finalTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Final total amount cannot be null or negative.");
        }
        this.finalTotalAmount = finalTotalAmount;
    }

    public BigDecimal getCashTendered() { return (paymentResult != null) ? paymentResult.getAmountTendered() : null; }
    public BigDecimal getChangeAmount() { return (paymentResult != null) ? paymentResult.getChangeGiven() : null; }
    public PaymentResult getPaymentResult() { return paymentResult; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bill bill = (Bill) o;
        return id == bill.id && Objects.equals(billSerialNumber, bill.billSerialNumber); // Include serial number for better equality check
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, billSerialNumber);
    }

    @Override
    public String toString() {
        return "Bill{" +
                "id=" + id +
                ", billSerialNumber='" + billSerialNumber + '\'' +
                ", billDate=" + billDate +
                ", transactionType=" + transactionType +
                ", userId=" + userId + // Changed to print Integer
                ", employeeId='" + employeeId + '\'' +
                ", billItemsCount=" + billItems.size() +
                ", subTotalAmount=" + String.format("%.2f", subTotalAmount) +
                ", discountAmount=" + String.format("%.2f", discountAmount) +
                ", finalTotalAmount=" + String.format("%.2f", finalTotalAmount) +
                '}';
    }
}