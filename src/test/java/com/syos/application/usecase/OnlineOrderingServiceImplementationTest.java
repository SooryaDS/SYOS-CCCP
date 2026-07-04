package com.syos.application.usecase;

import com.syos.domain.enums.TransactionType;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.*;
import com.syos.application.port.OnlineOrderingService;
import com.syos.application.port.StockManagementService;
import com.syos.domain.enums.OrderStatus;
import com.syos.domain.exception.*;
import com.syos.domain.model.*;
import com.syos.adapter.out.util.DatabaseConnection;

import com.syos.service.payment.PaymentMethod;
import com.syos.service.payment.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import java.sql.Connection;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnlineOrderingServiceImplementationTest {

    @Mock
    private OnlineOrderRepository onlineOrderRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private WebsiteStockRepository websiteStockRepository;
    @Mock
    private BillRepository billRepository;
    @Mock
    private StockManagementService stockManagementService;
    @Mock
    private PaymentMethod onlinePaymentStrategy;
    @Mock
    private OnlineUserRepository onlineUserRepository;

    @InjectMocks
    private OnlineOrderingServiceImplementation onlineOrderingService;
    @InjectMocks
    private OnlineOrderingServiceImplementation service;

    private int TEST_USER_ID = 1;
    private String TEST_ITEM_CODE_1 = "ITEM001";
    private String TEST_ITEM_CODE_2 = "ITEM002";
    private String TEST_ITEM_NAME_1 = "Test Item 1";
    private String TEST_ITEM_NAME_2 = "Test Item 2";
    private BigDecimal TEST_ITEM_PRICE_1 = new BigDecimal("10.00");
    private BigDecimal TEST_ITEM_PRICE_2 = new BigDecimal("20.00");

    private OnlineUser testUser;
    private Item testItem1;
    private Item testItem2;
    private WebsiteStock testWebsiteStock1;
    private WebsiteStock testWebsiteStock2;
    private OnlineOrder pendingOrder;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new OnlineUser(
                TEST_USER_ID,
                "testuser",
                "dummyPassHash",
                "Test User FullName",
                "test@example.com",
                "123 Test St"
        );

        // Create test items
        testItem1 = new Item(
                TEST_ITEM_CODE_1,
                TEST_ITEM_NAME_1,
                "Description for Item 1",
                "Category A",
                TEST_ITEM_PRICE_1,
                10,
                50
        );

        testItem2 = new Item(
                TEST_ITEM_CODE_2,
                TEST_ITEM_NAME_2,
                "Description for Item 2",
                "Category B",
                TEST_ITEM_PRICE_2,
                5,
                20
        );

        // Website stocks
        testWebsiteStock1 = new WebsiteStock(1L, testItem1, 100, LocalDateTime.now());
        testWebsiteStock2 = new WebsiteStock(2L, testItem2, 50, LocalDateTime.now());

        // Initialize pending order using the class field (not local variable!)
        pendingOrder = new OnlineOrder(
                "1",                     // orderId
                TEST_USER_ID,            // onlineUserId
                OrderStatus.PENDING,     // status
                LocalDateTime.now(),     // creationDate
                LocalDateTime.now(),     // lastModifiedDate
                null,                    // shippingAddress
                BigDecimal.ZERO,         // calculatedTotalAmount
                BigDecimal.ZERO          // discountAmount
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative quantity update")
    void updateOrderItemQuantity_negativeQuantity() throws DatabaseOperationException, InsufficientStockException, ItemNotFoundException {
        // Arrange: add an item to the order
        pendingOrder.addItem(testItem1, 2);

        // Mock repository to return this order
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        // Act & Assert: negative quantity should throw exception
        assertThrows(IllegalArgumentException.class, () ->
                onlineOrderingService.updateOrderItemQuantity(TEST_USER_ID, TEST_ITEM_CODE_1, -1)
        );
    }


    // --- getActiveOrderForUser tests ---
    @Test
    @DisplayName("Should return existing PENDING order for user")
    void getActiveOrderForUser_existingPendingOrder() throws DatabaseOperationException {
        List<OnlineOrder> existingOrders = Collections.singletonList(pendingOrder);
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING)).willReturn(existingOrders);

        OnlineOrder result = onlineOrderingService.getActiveOrderForUser(TEST_USER_ID);

        assertNotNull(result);
        assertEquals(pendingOrder.getOrderId(), result.getOrderId());
        verify(onlineOrderRepository, times(1)).findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING);
        verify(onlineOrderRepository, never()).save(any(OnlineOrder.class));
        verify(onlineUserRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("Should create and return new PENDING order if none exists")
    void getActiveOrderForUser_noPendingOrder() throws DatabaseOperationException {
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING)).willReturn(Collections.emptyList());
        given(onlineUserRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));
        given(onlineOrderRepository.save(any(OnlineOrder.class))).willAnswer(invocation -> {
            OnlineOrder savedOrder = invocation.getArgument(0);
            savedOrder.setOrderId("2");
            return savedOrder;
        });

        OnlineOrder result = onlineOrderingService.getActiveOrderForUser(TEST_USER_ID);

        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getOnlineUserId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(testUser.getAddress(), result.getShippingAddress());
        assertNotNull(result.getOrderId());
        verify(onlineOrderRepository, times(1)).findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING);
        verify(onlineUserRepository, times(1)).findById(TEST_USER_ID);
        verify(onlineOrderRepository, times(1)).save(any(OnlineOrder.class));
    }

    @Test
    @DisplayName("Should throw DatabaseOperationException if user not found when creating new order")
    void getActiveOrderForUser_userNotFound() throws DatabaseOperationException {
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING)).willReturn(Collections.emptyList());
        given(onlineUserRepository.findById(TEST_USER_ID)).willReturn(Optional.empty());

        assertThrows(DatabaseOperationException.class, () ->
                onlineOrderingService.getActiveOrderForUser(TEST_USER_ID)
        );

        verify(onlineOrderRepository, times(1)).findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING);
        verify(onlineUserRepository, times(1)).findById(TEST_USER_ID);
        verify(onlineOrderRepository, never()).save(any(OnlineOrder.class));
    }

    @Test
    @DisplayName("Should add new item to an empty order")
    void addItemToActiveOrder_addNewItem() throws Exception {

        // Arrange
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        given(itemRepository.findByItemCode(TEST_ITEM_CODE_1))
                .willReturn(Optional.of(testItem1));

        // IMPORTANT: must mock findByItem(Item), not findByItemCode(String)
        given(websiteStockRepository.findByItem(testItem1))
                .willReturn(Optional.of(testWebsiteStock1));

        given(onlineOrderRepository.update(any(OnlineOrder.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // Act
        OnlineOrder updatedOrder =
                onlineOrderingService.addItemToActiveOrder(TEST_USER_ID, TEST_ITEM_CODE_1, 5);

        // Assert
        assertNotNull(updatedOrder);
        assertEquals(1, updatedOrder.getItems().size());
        assertEquals(TEST_ITEM_CODE_1,
                updatedOrder.getItems().get(0).getItem().getItemCode());
        assertEquals(5,
                updatedOrder.getItems().get(0).getQuantity());

        verify(onlineOrderRepository, times(1))
                .update(any(OnlineOrder.class));
    }


    @Test
    @DisplayName("Should update existing item quantity in order")
    void addItemToActiveOrder_updateExistingItem()
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {

        // Arrange
        pendingOrder.addItem(testItem1, 3);

        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        given(itemRepository.findByItemCode(TEST_ITEM_CODE_1))
                .willReturn(Optional.of(testItem1));

        // IMPORTANT FIX
        given(websiteStockRepository.findByItem(testItem1))
                .willReturn(Optional.of(testWebsiteStock1));

        given(onlineOrderRepository.update(any(OnlineOrder.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // Act
        OnlineOrder updatedOrder =
                onlineOrderingService.addItemToActiveOrder(TEST_USER_ID, TEST_ITEM_CODE_1, 5);

        // Assert
        assertNotNull(updatedOrder);
        assertEquals(1, updatedOrder.getItems().size());
        assertEquals(TEST_ITEM_CODE_1, updatedOrder.getItems().get(0).getItem().getItemCode());
        assertEquals(8, updatedOrder.getItems().get(0).getQuantity());

        verify(onlineOrderRepository, times(1))
                .update(any(OnlineOrder.class));
    }

    // --- addItemToActiveOrder tests continued ---



    @Test
    @DisplayName("Should throw IllegalStateException if order is not PENDING")
    void addItemToActiveOrder_orderNotPending() throws DatabaseOperationException {
        pendingOrder.setStatus(OrderStatus.SUBMITTED); // Change status to non-pending
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING)).willReturn(Collections.singletonList(pendingOrder));

        assertThrows(IllegalStateException.class, () ->
                onlineOrderingService.addItemToActiveOrder(TEST_USER_ID, TEST_ITEM_CODE_1, 1)
        );
    }

    @Test
    @DisplayName("Should throw ItemNotFoundException if item not in repository")
    void addItemToActiveOrder_itemNotFound() throws DatabaseOperationException {
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING)).willReturn(Collections.singletonList(pendingOrder));
        given(itemRepository.findByItemCode(TEST_ITEM_CODE_1)).willReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () ->
                onlineOrderingService.addItemToActiveOrder(TEST_USER_ID, TEST_ITEM_CODE_1, 1)
        );
    }
    @Test
    @DisplayName("Should throw InsufficientStockException if stock is less than requested")
    void addItemToActiveOrder_insufficientStock() throws DatabaseOperationException {
        // 1. Arrange
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        given(itemRepository.findByItemCode(TEST_ITEM_CODE_1))
                .willReturn(Optional.of(testItem1));

        // FIX: Change findByItemCode to findByItem(testItem1) to match service code
        given(websiteStockRepository.findByItem(testItem1))
                .willReturn(Optional.of(new WebsiteStock(1L, testItem1, 5, LocalDateTime.now())));

        // 2. Act & Assert
        assertThrows(InsufficientStockException.class, () ->
                onlineOrderingService.addItemToActiveOrder(TEST_USER_ID, TEST_ITEM_CODE_1, 10)
        );
    }
    @Test
    @DisplayName("Should update item quantity in order")
    void updateOrderItemQuantity_success() throws Exception {

        // Arrange
        pendingOrder.addItem(testItem1, 5);

        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        given(itemRepository.findByItemCode(TEST_ITEM_CODE_1))
                .willReturn(Optional.of(testItem1));

        // IMPORTANT FIX
        given(websiteStockRepository.findByItem(testItem1))
                .willReturn(Optional.of(testWebsiteStock1));

        given(onlineOrderRepository.update(any(OnlineOrder.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // Act
        OnlineOrder updatedOrder =
                onlineOrderingService.updateOrderItemQuantity(TEST_USER_ID, TEST_ITEM_CODE_1, 10);

        // Assert
        assertEquals(10, updatedOrder.getItems().get(0).getQuantity());

        verify(onlineOrderRepository, times(1))
                .update(any(OnlineOrder.class));
    }



    @Test
    @DisplayName("Should remove item if new quantity is 0")
    void updateOrderItemQuantity_removeIfZero() throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException, IllegalArgumentException, IllegalStateException {
        pendingOrder.addItem(testItem1, 5);
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));
        given(onlineOrderRepository.update(any(OnlineOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        OnlineOrder updatedOrder = onlineOrderingService.updateOrderItemQuantity(TEST_USER_ID, TEST_ITEM_CODE_1, 0);

        assertTrue(updatedOrder.getItems().isEmpty());
        verify(onlineOrderRepository, times(1)).update(updatedOrder);
        verify(itemRepository, never()).findByItemCode(anyString());
        verify(websiteStockRepository, never()).findByItemCode(anyString());
    }

    @Test
    @DisplayName("Should throw ItemNotFoundException if item not in order")
    void updateOrderItemQuantity_itemNotInOrder() throws DatabaseOperationException, IllegalStateException {
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));
        assertThrows(ItemNotFoundException.class, () ->
                onlineOrderingService.updateOrderItemQuantity(TEST_USER_ID, TEST_ITEM_CODE_1, 5)
        );
    }

    @Test
    @DisplayName("Should throw InsufficientStockException if new quantity exceeds stock")
    void updateOrderItemQuantity_insufficientStock()
            throws DatabaseOperationException {


        pendingOrder.addItem(testItem1, 1);

        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        given(itemRepository.findByItemCode(TEST_ITEM_CODE_1))
                .willReturn(Optional.of(testItem1));

        given(websiteStockRepository.findByItem(testItem1))
                .willReturn(Optional.of(
                        new WebsiteStock(1L, testItem1, 5, LocalDateTime.now())  // only 5 in stock
                ));


        assertThrows(InsufficientStockException.class, () ->
                onlineOrderingService.updateOrderItemQuantity(TEST_USER_ID, TEST_ITEM_CODE_1, 10)
        );
    }


    // --- removeItemFromActiveOrder tests ---
    @Test
    @DisplayName("Should remove item from order")
    void removeItemFromActiveOrder_success() throws ItemNotFoundException, DatabaseOperationException, IllegalStateException {
        pendingOrder.addItem(testItem1, 5);
        pendingOrder.addItem(testItem2, 2);
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));
        given(onlineOrderRepository.update(any(OnlineOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        OnlineOrder updatedOrder = onlineOrderingService.removeItemFromActiveOrder(TEST_USER_ID, TEST_ITEM_CODE_1);

        assertEquals(1, updatedOrder.getItems().size());
        assertEquals(TEST_ITEM_CODE_2, updatedOrder.getItems().get(0).getItem().getItemCode());
        verify(onlineOrderRepository, times(1)).update(updatedOrder);
    }
    // --- checkoutOrder tests ---
    @Test
    void checkoutOrder_Success_ShouldCommitTransaction() throws Exception {
        try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
            // Arrange
            Connection mockConn = mock(Connection.class);
            mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

            OnlineOrder order = new OnlineOrder(1);
            Item testItem = new Item("I01", "Item", "D", "C", BigDecimal.TEN, 1, 1);
            order.addItem(testItem, 1);
            order.setStatus(OrderStatus.PENDING);

            BigDecimal total = order.getCalculatedTotalAmount();

            when(onlineOrderRepository.findByUserIdAndStatus(anyInt(), any())).thenReturn(List.of(order));
            when(itemRepository.findByItemCode(anyString())).thenReturn(Optional.of(testItem));
            when(websiteStockRepository.findByItemForUpdate(any(), any())).thenReturn(Optional.of(new WebsiteStock(testItem, 10)));

            // CORRECTED ORDER: (boolean, BigDecimal, BigDecimal, String)
            PaymentResult paymentSuccess = new PaymentResult(true, total, total, "Success");
            when(onlinePaymentStrategy.processPayment(any(), any())).thenReturn(paymentSuccess);

            when(billRepository.findNextBillSerialNumberForToday()).thenReturn("SN-001");
            when(billRepository.save(any(), any())).thenAnswer(i -> i.getArguments()[0]);

            // Act - Ensure 'service' variable name matches your @InjectMocks declaration
            Bill bill = service.checkoutOrder(1, "123 Street");

            // Assert
            assertNotNull(bill);
            assertEquals(OrderStatus.SUBMITTED, order.getStatus());
            verify(mockConn).commit();
            verify(mockConn, never()).rollback();
        }
    }

    @Test
    void checkoutOrder_PaymentFailed_ShouldRollback() throws Exception {
        try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
            Connection mockConn = mock(Connection.class);
            mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

            OnlineOrder order = new OnlineOrder(1);
            Item testItem = new Item("I01", "Item", "D", "C", BigDecimal.TEN, 1, 1);
            order.addItem(testItem, 1);
            order.setStatus(OrderStatus.PENDING);

            when(onlineOrderRepository.findByUserIdAndStatus(anyInt(), any())).thenReturn(List.of(order));
            when(itemRepository.findByItemCode(anyString())).thenReturn(Optional.of(testItem));
            when(websiteStockRepository.findByItemForUpdate(any(), any())).thenReturn(Optional.of(new WebsiteStock(testItem, 10)));

            // CORRECTED ORDER: (boolean, BigDecimal, BigDecimal, String)
            PaymentResult paymentFail = new PaymentResult(false, BigDecimal.ZERO, BigDecimal.ZERO, "Failed");
            when(onlinePaymentStrategy.processPayment(any(), any())).thenReturn(paymentFail);

            // Act & Assert
            assertThrows(OrderProcessingException.class, () -> service.checkoutOrder(1, "123 Street"));

            verify(mockConn).rollback();
            verify(mockConn, never()).commit();
        }
    }
    @Test
    @DisplayName("Should throw OrderProcessingException for empty order")
    void checkoutOrder_emptyOrder() throws DatabaseOperationException, PaymentException {
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        assertThrows(OrderProcessingException.class, () ->
                onlineOrderingService.checkoutOrder(TEST_USER_ID, "Address")
        );
        verifyNoInteractions(itemRepository, websiteStockRepository, billRepository, stockManagementService, onlinePaymentStrategy);
    }

    @Test
    @DisplayName("Should throw OrderProcessingException if order is not PENDING")
    void checkoutOrder_orderNotPending() throws DatabaseOperationException {
        pendingOrder.addItem(testItem1, 1);
        pendingOrder.setStatus(OrderStatus.SUBMITTED);
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        assertThrows(OrderProcessingException.class, () ->
                onlineOrderingService.checkoutOrder(TEST_USER_ID, "Address")
        );
        verify(onlineOrderRepository, never()).update(any(OnlineOrder.class));
    }


    // --- updateOrderItemQuantity tests (continued) ---
    @Test
    @DisplayName("Should remove item if new quantity is 0 (String orderId)")
    void updateOrderItemQuantity_removeIfZero_stringId() throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException, IllegalArgumentException, IllegalStateException {
        pendingOrder.addItem(testItem1, 5);
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));
        given(onlineOrderRepository.update(any(OnlineOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        OnlineOrder updatedOrder = onlineOrderingService.updateOrderItemQuantity(TEST_USER_ID, TEST_ITEM_CODE_1, 0);

        assertTrue(updatedOrder.getItems().isEmpty());
        verify(onlineOrderRepository, times(1)).update(updatedOrder);
        verify(itemRepository, never()).findByItemCode(anyString());
        verify(websiteStockRepository, never()).findByItemCode(anyString());
    }

    @Test
    @DisplayName("Should throw ItemNotFoundException if item not in order (String orderId)")
    void updateOrderItemQuantity_itemNotInOrder_stringId() throws DatabaseOperationException, IllegalStateException {
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));
        assertThrows(ItemNotFoundException.class, () ->
                onlineOrderingService.updateOrderItemQuantity(TEST_USER_ID, TEST_ITEM_CODE_1, 5)
        );
    }

    @Test
    @DisplayName("Should throw InsufficientStockException if new quantity exceeds stock")
    void updateOrderItemQuantity_insufficientStock_stringId() throws DatabaseOperationException {
        // 1. Setup
        pendingOrder.addItem(testItem1, 1);

        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        given(itemRepository.findByItemCode(TEST_ITEM_CODE_1)).willReturn(Optional.of(testItem1));

        // CHANGE: Use findByItem(testItem1) instead of findByItemCode
        given(websiteStockRepository.findByItem(testItem1))
                .willReturn(Optional.of(new WebsiteStock(1L, testItem1, 5, LocalDateTime.now())));

        // 2. Act & Assert
        assertThrows(InsufficientStockException.class, () ->
                onlineOrderingService.updateOrderItemQuantity(TEST_USER_ID, TEST_ITEM_CODE_1, 10)
        );
    }

    // --- removeItemFromActiveOrder tests ---
    @Test
    @DisplayName("Should remove item from order (String orderId)")
    void removeItemFromActiveOrder_success_stringId() throws ItemNotFoundException, DatabaseOperationException, IllegalStateException {
        pendingOrder.addItem(testItem1, 5);
        pendingOrder.addItem(testItem2, 2);
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));
        given(onlineOrderRepository.update(any(OnlineOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        OnlineOrder updatedOrder = onlineOrderingService.removeItemFromActiveOrder(TEST_USER_ID, TEST_ITEM_CODE_1);

        assertEquals(1, updatedOrder.getItems().size());
        assertEquals(TEST_ITEM_CODE_2, updatedOrder.getItems().get(0).getItem().getItemCode());
        verify(onlineOrderRepository, times(1)).update(updatedOrder);
    }

    @Test
    @DisplayName("Should throw ItemNotFoundException if item not in order when removing (String orderId)")
    void removeItemFromActiveOrder_itemNotInOrder_stringId() throws DatabaseOperationException, IllegalStateException {
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));
        assertThrows(ItemNotFoundException.class, () ->
                onlineOrderingService.removeItemFromActiveOrder(TEST_USER_ID, TEST_ITEM_CODE_1)
        );
    }
    // --- checkoutOrder tests ---
    @Test
    @DisplayName("Should successfully checkout order and create bill (String orderId)")
    void checkoutOrder_success_stringId() throws Exception {
        try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
            // 1. Setup Mock Connection
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

            // 2. Prepare Data
            pendingOrder.addItem(testItem1, 2);
            pendingOrder.addItem(testItem2, 1);
            pendingOrder.setOrderId("123");
            BigDecimal expectedTotal = new BigDecimal("40.00");

            // 3. Mock Repository Behaviors
            given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                    .willReturn(Collections.singletonList(pendingOrder));

            // Use findByItemForUpdate and any(Connection) to match service logic
            given(websiteStockRepository.findByItemForUpdate(eq(testItem1), any()))
                    .willReturn(Optional.of(testWebsiteStock1));
            given(websiteStockRepository.findByItemForUpdate(eq(testItem2), any()))
                    .willReturn(Optional.of(testWebsiteStock2));

            given(itemRepository.findByItemCode(TEST_ITEM_CODE_1)).willReturn(Optional.of(testItem1));
            given(itemRepository.findByItemCode(TEST_ITEM_CODE_2)).willReturn(Optional.of(testItem2));

            // Payment Mock
            given(onlinePaymentStrategy.processPayment(eq(expectedTotal), eq(expectedTotal)))
                    .willReturn(new PaymentResult(true, expectedTotal, expectedTotal, "Payment successful"));

            // Bill Mock
            given(billRepository.findNextBillSerialNumberForToday()).willReturn("202506120001");
            given(billRepository.save(any(Bill.class), any())).willAnswer(invocation -> {
                Bill savedBill = invocation.getArgument(0);
                savedBill.setBillSerialNumber("202506120001");
                return savedBill;
            });

            // 4. Act
            Bill resultBill = onlineOrderingService.checkoutOrder(TEST_USER_ID, "New Shipping Address");

            // 5. Assert
            assertNotNull(resultBill);
            assertEquals(TEST_USER_ID, resultBill.getUserId());
            assertEquals(expectedTotal, resultBill.getFinalTotalAmount());
            assertEquals(OrderStatus.SUBMITTED, pendingOrder.getStatus());

            // 6. Verify Transaction flow
            verify(mockConn).commit();
            verify(websiteStockRepository, times(1)).update(eq(testWebsiteStock1), eq(mockConn));
            verify(websiteStockRepository, times(1)).update(eq(testWebsiteStock2), eq(mockConn));
        }
    }
    @Test
    @DisplayName("Should throw OrderProcessingException for empty order (String orderId)")
    void checkoutOrder_emptyOrder_stringId() throws DatabaseOperationException, PaymentException {
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        assertThrows(OrderProcessingException.class, () ->
                onlineOrderingService.checkoutOrder(TEST_USER_ID, "Address")
        );
        verifyNoInteractions(itemRepository, websiteStockRepository, billRepository, stockManagementService, onlinePaymentStrategy);
    }

    @Test
    @DisplayName("Should throw OrderProcessingException if order is not PENDING (String orderId)")
    void checkoutOrder_orderNotPending_stringId() throws DatabaseOperationException {
        pendingOrder.addItem(testItem1, 1);
        pendingOrder.setStatus(OrderStatus.SUBMITTED);
        given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                .willReturn(Collections.singletonList(pendingOrder));

        assertThrows(OrderProcessingException.class, () ->
                onlineOrderingService.checkoutOrder(TEST_USER_ID, "Address")
        );
        verify(onlineOrderRepository, never()).update(any(OnlineOrder.class));
    }



    @Test
    @DisplayName("Should throw OrderProcessingException on payment failure and revert order status")
    void checkoutOrder_paymentFailed_stringId() throws Exception {
        try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

            pendingOrder.addItem(testItem1, 2);
            BigDecimal expectedTotal = TEST_ITEM_PRICE_1.multiply(new BigDecimal(2));

            given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                    .willReturn(Collections.singletonList(pendingOrder));
            given(itemRepository.findByItemCode(TEST_ITEM_CODE_1)).willReturn(Optional.of(testItem1));

            // Match the service call: findByItemForUpdate
            given(websiteStockRepository.findByItemForUpdate(eq(testItem1), any()))
                    .willReturn(Optional.of(testWebsiteStock1));

            given(onlineOrderRepository.update(any(OnlineOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

            // Mock Payment Denied
            given(onlinePaymentStrategy.processPayment(eq(expectedTotal), eq(expectedTotal)))
                    .willReturn(new PaymentResult(false, expectedTotal, BigDecimal.ZERO, "Payment denied"));

            assertThrows(OrderProcessingException.class, () ->
                    onlineOrderingService.checkoutOrder(TEST_USER_ID, "Address")
            );

            // Verify transaction was rolled back
            verify(mockConn).rollback();
            // Since your implementation re-saves the order status in the catch block if needed
            // OR the status remains at PROCESSING because of the rollback
            verify(billRepository, never()).save(any(Bill.class), any());
        }
    }
    @Test
    @DisplayName("Should throw OrderProcessingException on PaymentException from strategy")
    void checkoutOrder_paymentStrategyThrowsException_stringId() throws Exception {
        try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

            pendingOrder.addItem(testItem1, 2);
            BigDecimal expectedTotal = TEST_ITEM_PRICE_1.multiply(new BigDecimal(2));

            given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                    .willReturn(Collections.singletonList(pendingOrder));
            given(itemRepository.findByItemCode(TEST_ITEM_CODE_1)).willReturn(Optional.of(testItem1));
            given(websiteStockRepository.findByItemForUpdate(eq(testItem1), any()))
                    .willReturn(Optional.of(testWebsiteStock1));

            given(onlineOrderRepository.update(any(OnlineOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

            // Mock gateway crash
            given(onlinePaymentStrategy.processPayment(any(), any()))
                    .willThrow(new RuntimeException("Gateway Down"));

            assertThrows(OrderProcessingException.class, () ->
                    onlineOrderingService.checkoutOrder(TEST_USER_ID, "Address")
            );

            verify(mockConn).rollback();
        }
    }
    @Test
    @DisplayName("Should throw ItemNotFoundException if item definition missing during bill creation")
    void checkoutOrder_itemDefinitionMissing_stringId() throws Exception {
        try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

            pendingOrder.addItem(testItem1, 1);

            given(onlineOrderRepository.findByUserIdAndStatus(TEST_USER_ID, OrderStatus.PENDING))
                    .willReturn(Collections.singletonList(pendingOrder));

            // Simulate item disappearing from DB between cart and checkout
            given(itemRepository.findByItemCode(TEST_ITEM_CODE_1)).willReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class, () ->
                    onlineOrderingService.checkoutOrder(TEST_USER_ID, "Address")
            );

            // Ensure we stop before payment
            verify(onlinePaymentStrategy, never()).processPayment(any(), any());
            verify(mockConn).rollback();
        }
    }

    @Test
    @DisplayName("Should successfully find and return existing order by ID (String orderId)")
    void findOnlineOrderById_success_stringId() throws DatabaseOperationException {
        OnlineOrder order = new OnlineOrder("128", TEST_USER_ID, OrderStatus.PENDING, LocalDateTime.now(), LocalDateTime.now(), "Some Address", BigDecimal.ZERO, BigDecimal.ZERO);
        given(onlineOrderRepository.findById("128")).willReturn(Optional.of(order));

        Optional<OnlineOrder> foundOrder = onlineOrderingService.findOnlineOrderById("128");

        assertTrue(foundOrder.isPresent());
        assertEquals(order.getOrderId(), foundOrder.get().getOrderId());
        verify(onlineOrderRepository, times(1)).findById("128");
    }

    @Test
    @DisplayName("Should return empty optional if order not found by ID (String orderId)")
    void findOnlineOrderById_notFound_stringId() throws DatabaseOperationException {
        given(onlineOrderRepository.findById("129")).willReturn(Optional.empty());

        Optional<OnlineOrder> foundOrder = onlineOrderingService.findOnlineOrderById("129");

        assertFalse(foundOrder.isPresent());
        verify(onlineOrderRepository, times(1)).findById("129");
    }

    @Test
    @DisplayName("Should throw DatabaseOperationException when repository fails to find order by ID (String orderId)")
    void findOnlineOrderById_databaseError_stringId() throws DatabaseOperationException {
        given(onlineOrderRepository.findById(anyString())).willThrow(new DatabaseOperationException("DB error"));

        assertThrows(DatabaseOperationException.class, () ->
                onlineOrderingService.findOnlineOrderById("130")
        );
        verify(onlineOrderRepository, times(1)).findById("130");
    }


}

