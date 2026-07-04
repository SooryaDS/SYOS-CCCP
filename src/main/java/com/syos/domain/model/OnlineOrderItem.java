package com.syos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a single item in an OnlineOrder.
 * Stores the snapshot of item price at the time of addition.
 */
public class OnlineOrderItem {

    private int orderItemId;
    private String orderId; // Linked OnlineOrder ID
    private Item item;
    private int quantity;
    private BigDecimal priceAtAddition;
    private BigDecimal lineTotal;

    // -------------------- Constructors --------------------

    /**
     * No-arg constructor for frameworks / serialization.
     */
    public OnlineOrderItem() { }

    /**
     * Creates a new OnlineOrderItem with the specified item and quantity.
     * Captures the current price of the item and calculates the line total.
     *
     * @param item     The item being added.
     * @param quantity Quantity of the item.
     */
    public OnlineOrderItem(Item item, int quantity) {
        validateItemAndQuantity(item, quantity);

        this.orderItemId = 0;
        this.orderId = null;
        this.item = item;
        this.quantity = quantity;
        this.priceAtAddition = item.getUnitPrice();
        recalculateLineTotal();
    }

    /**
     * Full constructor for existing order items (e.g., loaded from database).
     */
    public OnlineOrderItem(int orderItemId, String orderId, Item item, int quantity,
                           BigDecimal priceAtAddition, BigDecimal lineTotal) {
        validateItemAndQuantity(item, quantity);
        validatePrice(priceAtAddition, "Price at addition");
        validatePrice(lineTotal, "Line total");

        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.item = item;
        this.quantity = quantity;
        this.priceAtAddition = priceAtAddition;
        this.lineTotal = lineTotal;
    }

    // -------------------- Business Methods --------------------

    private void validateItemAndQuantity(Item item, int quantity) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null.");
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        if (item.getUnitPrice() == null) throw new IllegalArgumentException("Item unit price cannot be null.");
    }

    private void validatePrice(BigDecimal price, String fieldName) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException(fieldName + " cannot be null or negative.");
    }

    private void recalculateLineTotal() {
        this.lineTotal = priceAtAddition.multiply(BigDecimal.valueOf(quantity));
    }

    // -------------------- Getters & Setters --------------------

    public int getOrderItemId() { return orderItemId; }
    public String getOrderId() { return orderId; }
    public Item getItem() { return item; }
    public String getItemCode() { return item != null ? item.getItemCode() : null; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPriceAtAddition() { return priceAtAddition; }
    public BigDecimal getLineTotal() { return lineTotal; }

    public void setOrderItemId(int orderItemId) { this.orderItemId = orderItemId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public void setItem(Item item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null.");
        this.item = item;
        if (priceAtAddition == null) this.priceAtAddition = item.getUnitPrice();
        recalculateLineTotal();
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        this.quantity = quantity;
        recalculateLineTotal();
    }

    public void setPriceAtAddition(BigDecimal priceAtAddition) {
        validatePrice(priceAtAddition, "Price at addition");
        this.priceAtAddition = priceAtAddition;
        recalculateLineTotal();
    }

    public void setLineTotal(BigDecimal lineTotal) {
        validatePrice(lineTotal, "Line total");
        this.lineTotal = lineTotal;
    }

    // -------------------- Object Overrides --------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OnlineOrderItem that = (OnlineOrderItem) o;

        if (orderItemId != 0 && that.orderItemId != 0)
            return orderItemId == that.orderItemId;

        return quantity == that.quantity &&
                Objects.equals(orderId, that.orderId) &&
                Objects.equals(item != null ? item.getItemCode() : null,
                        that.item != null ? that.item.getItemCode() : null) &&
                Objects.equals(priceAtAddition, that.priceAtAddition);
    }

    @Override
    public int hashCode() {
        if (orderItemId != 0) return Objects.hash(orderItemId);
        return Objects.hash(orderId, item != null ? item.getItemCode() : null, quantity, priceAtAddition);
    }

    @Override
    public String toString() {
        return "OnlineOrderItem{" +
                "orderItemId=" + orderItemId +
                ", orderId='" + orderId + '\'' +
                ", itemCode='" + (item != null ? item.getItemCode() : "null") + '\'' +
                ", quantity=" + quantity +
                ", priceAtAddition=" + String.format("%.2f", priceAtAddition) +
                ", lineTotal=" + String.format("%.2f", lineTotal) +
                '}';
    }
}
