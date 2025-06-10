package com.syos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class OnlineOrderItem {
    private int orderItemId;
    private String orderId;
    private Item item;
    private int quantity;
    private BigDecimal priceAtAddition;
    private BigDecimal lineTotal;

    public OnlineOrderItem(Item item, int quantity) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null for an order item.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        this.item = item;
        this.quantity = quantity;
        this.priceAtAddition = item.getUnitPrice();
        this.lineTotal = this.priceAtAddition.multiply(new BigDecimal(quantity));
    }

    public OnlineOrderItem(int orderItemId, String orderId, Item item, int quantity, BigDecimal priceAtAddition) {
        this(item, quantity);
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.priceAtAddition = priceAtAddition;
        this.lineTotal = this.priceAtAddition.multiply(new BigDecimal(quantity));
    }

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int orderItemId) { this.orderItemId = orderItemId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Item getItem() { return item; }
    public String getItemCode() { return item.getItemCode(); }
    public String getItemName() { return item.getName(); }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        this.quantity = quantity;
        this.lineTotal = this.priceAtAddition.multiply(new BigDecimal(this.quantity));
    }
    public BigDecimal getPriceAtAddition() { return priceAtAddition; }
    public BigDecimal getLineTotal() { return lineTotal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OnlineOrderItem that = (OnlineOrderItem) o;
        if (orderItemId != 0 && that.orderItemId != 0) return orderItemId == that.orderItemId;
        return Objects.equals(item, that.item) && Objects.equals(orderId, that.orderId);
    }

    @Override
    public int hashCode() {
        if (orderItemId != 0) return Objects.hash(orderItemId);
        return Objects.hash(orderId, item);
    }

    @Override
    public String toString() {
        return String.format("  Item: %s (%s), Qty: %d, Price: %.2f, Line Total: %.2f", item.getName(), item.getItemCode(), quantity, priceAtAddition, lineTotal);
    }
}

