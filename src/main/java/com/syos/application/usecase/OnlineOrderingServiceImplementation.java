package com.syos.application.usecase;

import com.syos.application.port.OnlineOrderingService;
import com.syos.application.port.StockManagementService;
import com.syos.domain.model.*;
import com.syos.domain.enums.OrderStatus;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.exception.*;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.*;
import com.syos.adapter.out.util.DatabaseConnection;

import com.syos.service.payment.PaymentMethod;
import com.syos.service.payment.PaymentResult;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class OnlineOrderingServiceImplementation implements OnlineOrderingService {

    private final OnlineOrderRepository onlineOrderRepository;
    private final ItemRepository itemRepository;
    private final WebsiteStockRepository websiteStockRepository;
    private final BillRepository billRepository;
    private final StockManagementService stockManagementService;
    private final PaymentMethod onlinePaymentStrategy;
    private final OnlineUserRepository onlineUserRepository;

    public OnlineOrderingServiceImplementation(OnlineOrderRepository onlineOrderRepository,
                                               ItemRepository itemRepository,
                                               WebsiteStockRepository websiteStockRepository,
                                               BillRepository billRepository,
                                               StockManagementService stockManagementService,
                                               PaymentMethod onlinePaymentStrategy,
                                               OnlineUserRepository onlineUserRepository) {
        this.onlineOrderRepository = onlineOrderRepository;
        this.itemRepository = itemRepository;
        this.websiteStockRepository = websiteStockRepository;
        this.billRepository = billRepository;
        this.stockManagementService = stockManagementService;
        this.onlinePaymentStrategy = onlinePaymentStrategy;
        this.onlineUserRepository = onlineUserRepository;
    }

    @Override
    public OnlineOrder getActiveOrderForUser(int onlineUserId) throws DatabaseOperationException {
        List<OnlineOrder> pendingOrders = onlineOrderRepository.findByUserIdAndStatus(onlineUserId, OrderStatus.PENDING);
        if (!pendingOrders.isEmpty()) return pendingOrders.get(0);

        OnlineUser user = onlineUserRepository.findById(onlineUserId)
                .orElseThrow(() -> new DatabaseOperationException("User not found for ID: " + onlineUserId));

        OnlineOrder newOrder = new OnlineOrder(onlineUserId);
        newOrder.setShippingAddress(user.getAddress());

        return onlineOrderRepository.save(newOrder);
    }

    @Override
    public OnlineOrder addItemToActiveOrder(int onlineUserId, String itemCode, int quantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {

        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");

        OnlineOrder order = getActiveOrderForUser(onlineUserId);
        if (order.getStatus() != OrderStatus.PENDING)
            throw new IllegalStateException("Can only add items to a PENDING order.");

        Item item = itemRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new ItemNotFoundException("Item with code '" + itemCode + "' not found."));

        WebsiteStock stock = websiteStockRepository.findByItem(item)
                .orElseThrow(() -> new InsufficientStockException("Stock info not found for item: " + item.getName()));

        int alreadyInCart = order.getItems().stream()
                .filter(oi -> oi.getItem().getItemCode().equals(itemCode))
                .mapToInt(OnlineOrderItem::getQuantity)
                .sum();

        if (stock.getQuantityAvailableOnline() < quantity + alreadyInCart)
            throw new InsufficientStockException("Not enough stock for item: " + item.getName());

        order.addItem(item, quantity);
        return onlineOrderRepository.update(order);
    }

    @Override
    public OnlineOrder updateOrderItemQuantity(int onlineUserId, String itemCode, int newQuantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {

        OnlineOrder order = getActiveOrderForUser(onlineUserId);
        if (order.getStatus() != OrderStatus.PENDING)
            throw new IllegalStateException("Can only update items in a PENDING order.");

        if (newQuantity < 0) throw new IllegalArgumentException("New quantity cannot be negative.");

        if (newQuantity > 0) {
            Item item = itemRepository.findByItemCode(itemCode)
                    .orElseThrow(() -> new ItemNotFoundException("Item with code '" + itemCode + "' not found."));

            WebsiteStock stock = websiteStockRepository.findByItem(item)
                    .orElseThrow(() -> new InsufficientStockException("Stock info not found for item: " + item.getName()));

            if (newQuantity > stock.getQuantityAvailableOnline())
                throw new InsufficientStockException("Not enough stock for item: " + item.getName());
        }

        boolean updated = order.updateItemQuantity(itemCode, newQuantity);
        if (!updated && newQuantity > 0)
            throw new ItemNotFoundException("Item '" + itemCode + "' not found in current order.");

        return onlineOrderRepository.update(order);
    }

    @Override
    public OnlineOrder removeItemFromActiveOrder(int onlineUserId, String itemCode)
            throws ItemNotFoundException, DatabaseOperationException {

        OnlineOrder order = getActiveOrderForUser(onlineUserId);
        if (order.getStatus() != OrderStatus.PENDING)
            throw new IllegalStateException("Can only remove items from a PENDING order.");

        boolean removed = order.removeItem(itemCode);
        if (!removed) throw new ItemNotFoundException("Item '" + itemCode + "' not found in current order.");

        return onlineOrderRepository.update(order);
    }

    @Override
    public Bill checkoutOrder(int onlineUserId, String shippingAddress)
            throws OrderProcessingException, DatabaseOperationException, InsufficientStockException, ItemNotFoundException {

        Connection conn = null;
        try {
            // 1️ Open connection and start transaction
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 2️ Fetch active order
            OnlineOrder order = getActiveOrderForUser(onlineUserId);
            validateOrderForCheckout(order);

            // 3️ Update shipping address and status
            order.setShippingAddress(shippingAddress);
            order.setStatus(OrderStatus.PROCESSING);
            onlineOrderRepository.update(order);

            // 4️ Prepare bill items (checks stock and locks rows)
            List<BillItem> billItems = prepareBillItems(order, conn);

            // 5️ Process payment
            BigDecimal amountToPay = calculateFinalAmount(order);
            PaymentResult paymentResult = processPayment(amountToPay);

            // 6️ Create and save bill
            Bill bill = createAndSaveBill(order, billItems, amountToPay, paymentResult, conn);

            // 7️ Reduce stock
            reduceStock(billItems, conn);

            // 8️ Update order status to SUBMITTED
            order.setStatus(OrderStatus.SUBMITTED);
            onlineOrderRepository.update(order);

            // 9️ Commit transaction
            conn.commit();
            System.out.println("Order " + order.getOrderId() + " checked out successfully. Bill #" + bill.getBillSerialNumber());
            return bill;

        } catch (Exception e) {
            rollback(conn);
            rethrowCheckoutExceptions(e);
            return null; // unreachable
        } finally {
            closeConnection(conn);
        }
    }

// ======= Helper Methods =======

    private void validateOrderForCheckout(OnlineOrder order) throws OrderProcessingException {
        if (order.getItems().isEmpty())
            throw new OrderProcessingException("Cannot checkout an empty order.");
        if (order.getStatus() != OrderStatus.PENDING)
            throw new OrderProcessingException("Order is not in PENDING state. Current status: " + order.getStatus());
    }

    private List<BillItem> prepareBillItems(OnlineOrder order, Connection conn)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {

        List<BillItem> billItems = new ArrayList<>();
        for (OnlineOrderItem oi : order.getItems()) {
            Item item = itemRepository.findByItemCode(oi.getItem().getItemCode())
                    .orElseThrow(() -> new ItemNotFoundException("Item missing: " + oi.getItem().getItemCode()));

            WebsiteStock stock = websiteStockRepository.findByItemForUpdate(item, conn)
                    .orElseThrow(() -> new InsufficientStockException("Stock info missing for item: " + item.getName()));

            if (stock.getQuantityAvailableOnline() < oi.getQuantity())
                throw new InsufficientStockException("Insufficient stock for item: " + item.getName());

            billItems.add(new BillItem(item, item.getName(), oi.getQuantity(),
                    oi.getPriceAtAddition(), oi.getLineTotal()));
        }
        return billItems;
    }

    private BigDecimal calculateFinalAmount(OnlineOrder order) {
        BigDecimal total = Optional.ofNullable(order.getCalculatedTotalAmount()).orElse(BigDecimal.ZERO);
        BigDecimal discount = Optional.ofNullable(order.getDiscountAmount()).orElse(BigDecimal.ZERO);
        return total.subtract(discount);
    }

    private PaymentResult processPayment(BigDecimal amountToPay) throws OrderProcessingException {
        try {
            PaymentResult result = onlinePaymentStrategy.processPayment(amountToPay, amountToPay);
            if (!result.isSuccess())
                throw new OrderProcessingException("Payment failed: " + result.getMessage());
            return result;
        } catch (Exception e) {
            throw new OrderProcessingException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    private Bill createAndSaveBill(OnlineOrder order, List<BillItem> billItems, BigDecimal finalAmount,
                                   PaymentResult paymentResult, Connection conn) throws DatabaseOperationException {
        Bill bill = new Bill(TransactionType.ONLINE_SALE);
        bill.setUserId(order.getOnlineUserId());
        bill.setBillDate(LocalDateTime.now());
        bill.setBillItems(billItems);
        bill.setSubTotalAmount(order.getCalculatedTotalAmount());
        bill.setDiscountAmount(order.getDiscountAmount());
        bill.setFinalTotalAmount(finalAmount);
        bill.recordPayment(paymentResult);

        String serialNumber = billRepository.findNextBillSerialNumberForToday();
        bill.setBillSerialNumber(serialNumber);

        return billRepository.save(bill, conn);
    }

    private void reduceStock(List<BillItem> billItems, Connection conn) throws DatabaseOperationException {
        for (BillItem bi : billItems) {
            WebsiteStock stock = websiteStockRepository.findByItemForUpdate(bi.getItem(), conn)
                    .orElseThrow(() -> new DatabaseOperationException("Stock missing during reduction for item: " + bi.getItem().getName()));
            stock.setQuantityAvailableOnline(stock.getQuantityAvailableOnline() - bi.getQuantityPurchased());
            websiteStockRepository.update(stock, conn);
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    private void rethrowCheckoutExceptions(Exception e) throws OrderProcessingException, DatabaseOperationException,
            InsufficientStockException, ItemNotFoundException {
        if (e instanceof OrderProcessingException) throw (OrderProcessingException) e;
        if (e instanceof DatabaseOperationException) throw (DatabaseOperationException) e;
        if (e instanceof InsufficientStockException) throw (InsufficientStockException) e;
        if (e instanceof ItemNotFoundException) throw (ItemNotFoundException) e;
        throw new OrderProcessingException("Checkout failed: " + e.getMessage(), e);
    }


    @Override
    public Optional<OnlineOrder> findOnlineOrderById(String orderId) throws DatabaseOperationException {
        return onlineOrderRepository.findById(orderId);
    }

    @Override
    public OnlineOrder updateShippingAddress(int onlineUserId, String newAddress)
            throws DatabaseOperationException {
        if (newAddress == null || newAddress.trim().isEmpty())
            throw new IllegalArgumentException("Shipping address cannot be null or empty.");

        OnlineOrder order = getActiveOrderForUser(onlineUserId);
        if (order.getStatus() != OrderStatus.PENDING)
            throw new IllegalStateException("Cannot update shipping address for an order not in PENDING status.");

        order.setShippingAddress(newAddress);
        return onlineOrderRepository.update(order);
    }
}
