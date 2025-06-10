package com.syos.domain.model;

import com.syos.domain.enums.TransactionType;
import com.syos.service.payment.PaymentResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Bill {
    private int billSerialNumber;
    private LocalDateTime billDate;
    private TransactionType transactionType;
    private String userId;
    private String employeeId;
    private List<BillItem> billItems;
    private BigDecimal subTotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalTotalAmount;
    private PaymentResult paymentResult;

    public Bill(TransactionType transactionType) {
        this.billDate = LocalDateTime.now();
        this.transactionType = transactionType;
        this.billItems = new ArrayList<>();
        this.subTotalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.finalTotalAmount = BigDecimal.ZERO;
        this.paymentResult = null;
    }

    public Bill(int billSerialNumber, LocalDateTime billDate, TransactionType transactionType, String userId, String employeeId, BigDecimal subTotalAmount, BigDecimal discountAmount, BigDecimal finalTotalAmount, BigDecimal cashTendered, BigDecimal changeAmount) {
        this.billSerialNumber = billSerialNumber;
        this.billDate = billDate;
        this.transactionType = transactionType;
        this.userId = userId;
        this.employeeId = employeeId;
        this.subTotalAmount = subTotalAmount;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.finalTotalAmount = finalTotalAmount;
        this.billItems = new ArrayList<>();
        if (cashTendered != null) {
            this.paymentResult = new PaymentResult(true, cashTendered, changeAmount, "Payment recorded from DB.");
        }
    }

    public void addItem(Item item, int quantity) {
        if (item == null || quantity <= 0) return;
        Optional<BillItem> existingItem = billItems.stream()
                .filter(bi -> bi.getItem().getItemCode().equals(item.getItemCode()))
                .findFirst();
        if (existingItem.isPresent()) {
            BillItem currentBillItem = existingItem.get();
            currentBillItem.setQuantityPurchased(currentBillItem.getQuantityPurchased() + quantity);
        } else {
            billItems.add(new BillItem(item, quantity));
        }
        recalculateTotals();
    }

    public void addBillItem(BillItem billItem) {
        if (billItem == null) throw new IllegalArgumentException("Bill item cannot be null.");
        this.billItems.add(billItem);
        recalculateTotals();
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
        if (paymentResult != null && paymentResult.isSuccess()) {
            this.paymentResult = paymentResult;
        }
    }

    public String generateReceipt() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        sb.append("========================================\n");
        sb.append("           SYOS - OFFICIAL BILL\n");
        sb.append("========================================\n");
        sb.append(String.format("Bill No.: %d\n", billSerialNumber));
        sb.append(String.format("Date    : %s\n", billDate.format(formatter)));
        sb.append(String.format("Type    : %s\n", transactionType));
        if (employeeId != null && !employeeId.isEmpty()) sb.append(String.format("Cashier : %s\n", employeeId));
        if (userId != null && !userId.isEmpty()) sb.append(String.format("Customer: %s\n", userId));
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
    public int getBillSerialNumber() { return billSerialNumber; }
    public void setBillSerialNumber(int billSerialNumber) { this.billSerialNumber = billSerialNumber; }
    public LocalDateTime getBillDate() { return billDate; }
    public void setBillDate(LocalDateTime billDate) { this.billDate = billDate; }
    public TransactionType getTransactionType() { return transactionType; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public List<BillItem> getBillItems() { return new ArrayList<>(billItems); }
    public void setBillItems(List<BillItem> billItems) {
        this.billItems = (billItems != null) ? billItems : new ArrayList<>();
        recalculateTotals();
    }
    public BigDecimal getSubTotalAmount() { return subTotalAmount; }
    public void setSubTotalAmount(BigDecimal subTotalAmount) { this.subTotalAmount = subTotalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = (discountAmount == null) ? BigDecimal.ZERO : discountAmount;
        recalculateTotals();
    }
    public BigDecimal getFinalTotalAmount() { return finalTotalAmount; }
    public void setFinalTotalAmount(BigDecimal finalTotalAmount) { this.finalTotalAmount = finalTotalAmount; }
    public BigDecimal getCashTendered() { return (paymentResult != null) ? paymentResult.getAmountTendered() : null; }
    public BigDecimal getChangeAmount() { return (paymentResult != null) ? paymentResult.getChangeGiven() : null; }
    public PaymentResult getPaymentResult() { return paymentResult; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bill bill = (Bill) o;
        return billSerialNumber == bill.billSerialNumber && Objects.equals(billDate.toLocalDate(), bill.billDate.toLocalDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(billSerialNumber, billDate.toLocalDate());
    }
}
