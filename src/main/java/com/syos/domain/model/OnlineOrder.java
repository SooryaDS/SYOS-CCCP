package com.syos.domain.model;

import com.syos.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class OnlineOrder {
    private String orderId;
    private int onlineUserId;
    private List<OnlineOrderItem> items;
    private OrderStatus status;
    private LocalDateTime creationDate;
    private LocalDateTime lastModifiedDate;
    private String shippingAddress;
    private BigDecimal calculatedTotalAmount;
    private BigDecimal discountAmount;

    public OnlineOrder(int onlineUserId) {
        this.orderId = UUID.randomUUID().toString();
        this.onlineUserId = onlineUserId;
        this.items = new ArrayList<>();
        this.status = OrderStatus.PENDING;
        this.creationDate = LocalDateTime.now();
        this.lastModifiedDate = LocalDateTime.now();
        this.calculatedTotalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
    }

    public OnlineOrder(String orderId, int onlineUserId, OrderStatus status, LocalDateTime creationDate, LocalDateTime lastModifiedDate, String shippingAddress, BigDecimal calculatedTotalAmount, BigDecimal discountAmount) {
        this.orderId = orderId;
        this.onlineUserId = onlineUserId;
        this.status = status;
        this.creationDate = creationDate;
        this.lastModifiedDate = lastModifiedDate;
        this.shippingAddress = shippingAddress;
        this.calculatedTotalAmount = calculatedTotalAmount;
        this.discountAmount = discountAmount;
        this.items = new ArrayList<>();
    }

    public void addItem(Item item, int quantity) {
        for (OnlineOrderItem orderItem : items) {
            if (orderItem.getItem().getItemCode().equals(item.getItemCode())) {
                orderItem.setQuantity(orderItem.getQuantity() + quantity);
                recalculateTotalAmount();
                this.lastModifiedDate = LocalDateTime.now();
                return;
            }
        }
        items.add(new OnlineOrderItem(item, quantity));
        recalculateTotalAmount();
        this.lastModifiedDate = LocalDateTime.now();
    }

    public boolean removeItem(String itemCode) {
        boolean removed = this.items.removeIf(orderItem -> orderItem.getItem().getItemCode().equals(itemCode));
        if (removed) {
            recalculateTotalAmount();
            this.lastModifiedDate = LocalDateTime.now();
        }
        return removed;
    }

    public boolean updateItemQuantity(String itemCode, int newQuantity) {
        if (newQuantity < 0) throw new IllegalArgumentException("New quantity cannot be negative.");
        if (newQuantity == 0) return removeItem(itemCode);
        for (OnlineOrderItem orderItem : items) {
            if (orderItem.getItem().getItemCode().equals(itemCode)) {
                orderItem.setQuantity(newQuantity);
                recalculateTotalAmount();
                this.lastModifiedDate = LocalDateTime.now();
                return true;
            }
        }
        return false;
    }

    private void recalculateTotalAmount() {
        this.calculatedTotalAmount = items.stream().map(OnlineOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getOrderId() { return orderId; }
    public int getOnlineUserId() { return onlineUserId; }
    public List<OnlineOrderItem> getItems() { return new ArrayList<>(items); }
    public void setItems(List<OnlineOrderItem> items) { this.items = new ArrayList<>(items); recalculateTotalAmount(); }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; this.lastModifiedDate = LocalDateTime.now(); }
    public LocalDateTime getCreationDate() { return creationDate; }
    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; this.lastModifiedDate = LocalDateTime.now(); }
    public BigDecimal getCalculatedTotalAmount() { return calculatedTotalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Discount amount cannot be null or negative."); this.discountAmount = discountAmount; this.lastModifiedDate = LocalDateTime.now(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OnlineOrder that = (OnlineOrder) o;
        return Objects.equals(orderId, that.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}