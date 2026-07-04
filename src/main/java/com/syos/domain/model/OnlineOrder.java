package com.syos.domain.model;

import com.syos.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents an Online Order placed by a customer.
 * This is a core domain entity containing business logic related to an online order.
 */
public class OnlineOrder {

    private String orderId; // now a String for UUID
    private int onlineUserId;
    private List<OnlineOrderItem> items;
    private OrderStatus status;
    private LocalDateTime creationDate;
    private LocalDateTime lastModifiedDate;
    private String shippingAddress;
    private BigDecimal calculatedTotalAmount;
    private BigDecimal discountAmount;

    // -------------------- Constructors --------------------

    /**
     * No-arg constructor for frameworks / serialization.
     */
    public OnlineOrder() {
        this.items = new ArrayList<>();
        this.calculatedTotalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.status = OrderStatus.PENDING;
        this.creationDate = LocalDateTime.now();
        this.lastModifiedDate = LocalDateTime.now();
        this.shippingAddress = "";
    }

    /**
     * Convenience constructor for creating a new online order with just a user ID.
     * Automatically generates UUID and sets default values.
     *
     * @param onlineUserId the ID of the user placing the order
     */
    public OnlineOrder(int onlineUserId) {
        this();
        this.orderId = UUID.randomUUID().toString();
        this.onlineUserId = onlineUserId;
    }

    /**
     * Full constructor for loading an existing order from a database.
     *
     * @param orderId             Order ID
     * @param onlineUserId        User ID
     * @param status              Order status
     * @param creationDate        Creation date
     * @param lastModifiedDate    Last modified date
     * @param shippingAddress     Shipping address
     * @param calculatedTotalAmount Calculated total amount
     * @param discountAmount      Discount amount
     */
    public OnlineOrder(String orderId, int onlineUserId, OrderStatus status, LocalDateTime creationDate,
                       LocalDateTime lastModifiedDate, String shippingAddress, BigDecimal calculatedTotalAmount,
                       BigDecimal discountAmount) {
        this();
        this.orderId = orderId;
        this.onlineUserId = onlineUserId;
        this.status = status != null ? status : OrderStatus.PENDING;
        this.creationDate = creationDate != null ? creationDate : LocalDateTime.now();
        this.lastModifiedDate = lastModifiedDate != null ? lastModifiedDate : LocalDateTime.now();
        this.shippingAddress = shippingAddress != null ? shippingAddress : "";
        this.calculatedTotalAmount = calculatedTotalAmount != null ? calculatedTotalAmount : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    /**
     * Full constructor including a list of items.
     *
     * @param orderId             Order ID
     * @param onlineUserId        User ID
     * @param status              Order status
     * @param creationDate        Creation date
     * @param lastModifiedDate    Last modified date
     * @param shippingAddress     Shipping address
     * @param calculatedTotalAmount Calculated total amount
     * @param discountAmount      Discount amount
     * @param items               List of order items
     */
    public OnlineOrder(String orderId, int onlineUserId, OrderStatus status, LocalDateTime creationDate,
                       LocalDateTime lastModifiedDate, String shippingAddress, BigDecimal calculatedTotalAmount,
                       BigDecimal discountAmount, List<OnlineOrderItem> items) {
        this(orderId, onlineUserId, status, creationDate, lastModifiedDate, shippingAddress, calculatedTotalAmount, discountAmount);
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        recalculateTotalAmount();
    }

    // -------------------- Business Methods --------------------

    public void addItem(Item item, int quantity) {
        if (item == null) throw new IllegalArgumentException("Cannot add null item to order.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");

        Optional<OnlineOrderItem> existingItemOpt = items.stream()
                .filter(orderItem -> orderItem.getItem().getItemCode().equals(item.getItemCode()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            OnlineOrderItem orderItem = existingItemOpt.get();
            orderItem.setQuantity(orderItem.getQuantity() + quantity);
        } else {
            items.add(new OnlineOrderItem(item, quantity));
        }

        recalculateTotalAmount();
        updateLastModifiedDate();
    }

    public boolean removeItem(String itemCode) {
        boolean removed = items.removeIf(orderItem -> orderItem.getItem().getItemCode().equals(itemCode));
        if (removed) {
            recalculateTotalAmount();
            updateLastModifiedDate();
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
                updateLastModifiedDate();
                return true;
            }
        }
        return false;
    }

    private void recalculateTotalAmount() {
        this.calculatedTotalAmount = items.stream()
                .map(OnlineOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateLastModifiedDate() {
        this.lastModifiedDate = LocalDateTime.now();
    }

    // -------------------- Getters & Setters --------------------

    public String getOrderId() { return orderId; }
    public int getOnlineUserId() { return onlineUserId; }
    public List<OnlineOrderItem> getItems() { return Collections.unmodifiableList(items); }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public String getShippingAddress() { return shippingAddress; }
    public BigDecimal getCalculatedTotalAmount() { return calculatedTotalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setItems(List<OnlineOrderItem> items) {
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        recalculateTotalAmount();
        updateLastModifiedDate();
    }

    public void setStatus(OrderStatus status) {
        if (status == null) throw new IllegalArgumentException("Status cannot be null.");
        this.status = status;
        updateLastModifiedDate();
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress != null ? shippingAddress : "";
        updateLastModifiedDate();
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Discount amount cannot be null or negative.");
        this.discountAmount = discountAmount;
        updateLastModifiedDate();
    }

    // -------------------- Object Overrides --------------------

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

    @Override
    public String toString() {
        return "OnlineOrder{" +
                "orderId='" + orderId + '\'' +
                ", onlineUserId=" + onlineUserId +
                ", status=" + status +
                ", creationDate=" + creationDate +
                ", lastModifiedDate=" + lastModifiedDate +
                ", calculatedTotalAmount=" + String.format("%.2f", calculatedTotalAmount) +
                ", discountAmount=" + String.format("%.2f", discountAmount) +
                ", shippingAddress='" + shippingAddress + '\'' +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                '}';
    }
}
