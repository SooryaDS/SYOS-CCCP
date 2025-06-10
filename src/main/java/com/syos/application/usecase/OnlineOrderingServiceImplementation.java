package com.syos.application.usecase;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.BillRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.OnlineOrderRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.WebsiteStockRepository;
import com.syos.application.port.OnlineOrderingService;
import com.syos.application.port.StockManagementService;
import com.syos.domain.enums.OrderStatus;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.exception.*;
import com.syos.domain.model.*;

import com.syos.service.payment.PaymentResult;
import com.syos.service.payment.PaymentStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OnlineOrderingServiceImplementation implements OnlineOrderingService {

    private final OnlineOrderRepository onlineOrderRepository;
    private final ItemRepository itemRepository;
    private final WebsiteStockRepository websiteStockRepository;
    private final BillRepository billRepository;
    private final StockManagementService stockManagementService;
    private final PaymentStrategy onlinePaymentStrategy;

    public OnlineOrderingServiceImplementation(OnlineOrderRepository onlineOrderRepository,
                                               ItemRepository itemRepository,
                                               WebsiteStockRepository websiteStockRepository,
                                               BillRepository billRepository,
                                               StockManagementService stockManagementService,
                                               PaymentStrategy onlinePaymentStrategy) {
        this.onlineOrderRepository = onlineOrderRepository;
        this.itemRepository = itemRepository;
        this.websiteStockRepository = websiteStockRepository;
        this.billRepository = billRepository;
        this.stockManagementService = stockManagementService;
        this.onlinePaymentStrategy = onlinePaymentStrategy;
    }

    @Override
    public OnlineOrder getActiveOrderForUser(int onlineUserId) throws DatabaseOperationException {
        List<OnlineOrder> pendingOrders = onlineOrderRepository.findByUserIdAndStatus(onlineUserId, OrderStatus.PENDING);
        if (!pendingOrders.isEmpty()) {
            return pendingOrders.get(0);
        } else {
            OnlineOrder newOrder = new OnlineOrder(onlineUserId);
            return onlineOrderRepository.save(newOrder);
        }
    }

    @Override
    public OnlineOrder addItemToActiveOrder(int onlineUserId, String itemCode, int quantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");

        OnlineOrder order = getActiveOrderForUser(onlineUserId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only add items to a PENDING order.");
        }

        Optional<Item> itemOptional = itemRepository.findByItemCode(itemCode);
        if (!itemOptional.isPresent()) {
            throw new ItemNotFoundException("Item with code '" + itemCode + "' not found.");
        }
        Item item = itemOptional.get();

        List<WebsiteStock> websiteStocks = websiteStockRepository.findByItemCode(itemCode);
        int totalWebsiteStock = websiteStocks.stream().mapToInt(WebsiteStock::getQuantityAvailableOnline).sum();

        int quantityAlreadyInCart = order.getItems().stream()
                .filter(oi -> oi.getItemCode().equals(itemCode))
                .mapToInt(OnlineOrderItem::getQuantity)
                .sum();

        if (totalWebsiteStock < (quantityAlreadyInCart + quantity)) {
            throw new InsufficientStockException("Not enough stock for item: " + item.getName() +
                    ". Available: " + (totalWebsiteStock - quantityAlreadyInCart));
        }

        order.addItem(item, quantity);
        return onlineOrderRepository.update(order);
    }

    @Override
    public OnlineOrder updateOrderItemQuantity(int onlineUserId, String itemCode, int newQuantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {
        OnlineOrder order = getActiveOrderForUser(onlineUserId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only update items in a PENDING order.");
        }

        if (newQuantity < 0) throw new IllegalArgumentException("New quantity cannot be negative.");

        if (newQuantity > 0) {
            Optional<Item> itemOptional = itemRepository.findByItemCode(itemCode);
            if (!itemOptional.isPresent()) {
                throw new ItemNotFoundException("Item with code '" + itemCode + "' not found.");
            }

            List<WebsiteStock> websiteStocks = websiteStockRepository.findByItemCode(itemCode);
            int totalWebsiteStock = websiteStocks.stream().mapToInt(WebsiteStock::getQuantityAvailableOnline).sum();

            if (newQuantity > totalWebsiteStock) {
                throw new InsufficientStockException("Not enough stock to update quantity for item: " + itemOptional.get().getName() +
                        ". Available: " + totalWebsiteStock + ", Desired: " + newQuantity);
            }
        }

        boolean updated = order.updateItemQuantity(itemCode, newQuantity);
        if (!updated && newQuantity > 0) {
            throw new ItemNotFoundException("Item '" + itemCode + "' not found in current order.");
        }
        return onlineOrderRepository.update(order);
    }

    @Override
    public OnlineOrder removeItemFromActiveOrder(int onlineUserId, String itemCode)
            throws ItemNotFoundException, DatabaseOperationException {
        OnlineOrder order = getActiveOrderForUser(onlineUserId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only remove items from a PENDING order.");
        }
        boolean removed = order.removeItem(itemCode);
        if (!removed) {
            throw new ItemNotFoundException("Item '" + itemCode + "' not found in current order.");
        }
        return onlineOrderRepository.update(order);
    }

    @Override
    public Bill checkoutOrder(int onlineUserId, String shippingAddress)
            throws OrderProcessingException, DatabaseOperationException, InsufficientStockException, ItemNotFoundException {
        OnlineOrder order = getActiveOrderForUser(onlineUserId);
        if (order.getItems().isEmpty()) {
            throw new OrderProcessingException("Cannot checkout an empty order.");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderProcessingException("Order is not in PENDING state. Current status: " + order.getStatus());
        }

        for (OnlineOrderItem oi : order.getItems()) {
            List<WebsiteStock> websiteStocks = websiteStockRepository.findByItemCode(oi.getItemCode());
            int totalWebsiteStock = websiteStocks.stream().mapToInt(WebsiteStock::getQuantityAvailableOnline).sum();
            if (totalWebsiteStock < oi.getQuantity()) {
                throw new InsufficientStockException("Stock for item " + oi.getItemName() +
                        " became insufficient. Available: " + totalWebsiteStock + ", Ordered: " + oi.getQuantity());
            }
        }

        order.setShippingAddress(shippingAddress);
        order.setStatus(OrderStatus.PROCESSING);
        onlineOrderRepository.update(order);

        Bill bill = new Bill(TransactionType.ONLINE_SALE);
        bill.setUserId(String.valueOf(order.getOnlineUserId()));
        bill.setBillDate(LocalDateTime.now());

        List<BillItem> billItemsForStockReduction = new ArrayList<>();
        for (OnlineOrderItem oi : order.getItems()) {
            Item currentItemDetails = itemRepository.findByItemCode(oi.getItemCode())
                    .orElseThrow(() -> new ItemNotFoundException("Item " + oi.getItemCode() + " definition missing during checkout."));

            BillItem newBillItem = new BillItem(
                    currentItemDetails,
                    oi.getItemName(),
                    oi.getQuantity(),
                    oi.getPriceAtAddition(),
                    oi.getLineTotal()
            );
            bill.addBillItem(newBillItem);
            billItemsForStockReduction.add(newBillItem);
        }

        bill.setSubTotalAmount(order.getCalculatedTotalAmount());
        bill.setDiscountAmount(order.getDiscountAmount());
        bill.setFinalTotalAmount(order.getCalculatedTotalAmount().subtract(order.getDiscountAmount()));

        BigDecimal amountToPay = bill.getFinalTotalAmount();
        BigDecimal amountTendered = amountToPay;

        PaymentResult paymentResult;
        try {
            paymentResult = onlinePaymentStrategy.processPayment(amountToPay, amountTendered);
        } catch (PaymentException e) {
            throw new OrderProcessingException("Payment failed during online checkout: " + e.getMessage(), e);
        }

        if (!paymentResult.isSuccess()) {
            throw new OrderProcessingException("Payment failed: " + paymentResult.getMessage());
        }

        bill.recordPayment(paymentResult);
        Bill savedBill = billRepository.save(bill);

        stockManagementService.reduceWebsiteStockAfterOnlineSale(billItemsForStockReduction);

        order.setStatus(OrderStatus.SUBMITTED);
        onlineOrderRepository.update(order);

        System.out.println("Order " + order.getOrderId() + " checked out. Bill #" + savedBill.getBillSerialNumber() + " created.");
        return savedBill;
    }
}
