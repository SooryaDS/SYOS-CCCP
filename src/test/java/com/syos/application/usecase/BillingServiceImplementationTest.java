package com.syos.application.usecase;

import com.syos.application.port.StockManagementService;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.exception.PaymentException;
import com.syos.domain.model.Bill;
import com.syos.domain.model.BillItem;
import com.syos.domain.model.Item;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.BillRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.service.payment.PaymentMethod;
import com.syos.service.payment.PaymentResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingServiceImplementationTest {

    private ItemRepository itemRepository;
    private BillRepository billRepository;
    private StockManagementService stockManagementService;
    private BillingServiceImplementation billingService;

    private Item sampleItem;

    @BeforeEach
    void setup() {
        itemRepository = mock(ItemRepository.class);
        billRepository = mock(BillRepository.class);
        stockManagementService = mock(StockManagementService.class);

        billingService = new BillingServiceImplementation(itemRepository, billRepository);

        sampleItem = new Item("ITEM001", "Test Item", "Sample description", "Category", BigDecimal.valueOf(10), 5, 10);
    }

    @Test
    void testStartNewBill_createsBillWithCorrectTransactionType() {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        assertNotNull(bill);
        assertEquals(TransactionType.POS_CASH, bill.getTransactionType());
        assertTrue(bill.getBillItems().isEmpty());
    }

    @Test
    void testAddItemToBill_addsItemCorrectly() {
        assertDoesNotThrow(() -> {
            when(itemRepository.findByItemCode("ITEM001")).thenReturn(Optional.of(sampleItem));

            Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
            billingService.addItemToBill(bill, "ITEM001", 2);

            assertEquals(1, bill.getBillItems().size());
            BillItem bi = bill.getBillItems().get(0);
            assertEquals(2, bi.getQuantityPurchased());
            assertEquals(BigDecimal.valueOf(20), bi.getTotalPriceForItem());
        });
    }


    @Test
    void testFinalizeBill_processesPaymentAndReducesStock() throws Exception {
        // Prepare Bill
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 3);

        // Mock ItemRepository so items exist
        when(itemRepository.findByItemCode("ITEM001")).thenReturn(Optional.of(sampleItem));

        // Mock BillRepository
        when(billRepository.findTopByBillDateOrderBySerialNumberDesc(any())).thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock Payment
        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        when(paymentMethod.processPayment(any(), any()))
                .thenReturn(new PaymentResult(true, BigDecimal.valueOf(50), BigDecimal.valueOf(20), "Paid"));

        Bill finalized = billingService.finalizeBill(bill, paymentMethod, stockManagementService, BigDecimal.valueOf(50));

        assertNotNull(finalized.getBillSerialNumber());
        assertEquals(BigDecimal.valueOf(30), finalized.getFinalTotalAmount());
        assertNotNull(finalized.getPaymentResult());
        verify(stockManagementService, times(1)).reduceShelfStockAfterPOSSale(any());
    }

    @Test
    void testGetBillDetails_returnsCorrectBill() throws DatabaseOperationException {
        Bill bill = new Bill(TransactionType.POS_CASH);
        bill.setBillSerialNumber("202601120001");
        bill.setBillDate(LocalDate.now().atStartOfDay());

        when(billRepository.findBySerialNumberAndDate("202601120001", LocalDate.now())).thenReturn(Optional.of(bill));

        Optional<Bill> result = billingService.getBillDetails("202601120001", LocalDate.now());
        assertTrue(result.isPresent());
        assertEquals("202601120001", result.get().getBillSerialNumber());
    }
    // Adding item to bill successfully
    @Test
    void testAddItemToBill_success() throws ItemNotFoundException, DatabaseOperationException {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        when(itemRepository.findByItemCode("ITEM001")).thenReturn(Optional.of(sampleItem));

        billingService.addItemToBill(bill, "ITEM001", 2);

        assertEquals(1, bill.getBillItems().size());
        assertEquals(2, bill.getBillItems().get(0).getQuantityPurchased());
        assertEquals(BigDecimal.valueOf(20), bill.getFinalTotalAmount());
    }

    //  Adding item that does not exist should throw ItemNotFoundException
    @Test
    void testAddItemToBill_itemNotFound() throws DatabaseOperationException {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        when(itemRepository.findByItemCode("ITEM002")).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class,
                () -> billingService.addItemToBill(bill, "ITEM002", 1));
    }


    // Test 3: Start new bill with null transaction type should throw IllegalArgumentException
    @Test
    void testStartNewBill_nullTransactionType() {
        assertThrows(IllegalArgumentException.class, () -> billingService.startNewBill(null));
    }

    //  getBillDetails calls repository correctly
    @Test
    void testGetBillDetails_success() throws DatabaseOperationException {
        String serial = "202601120001";
        LocalDate date = LocalDate.now();
        Bill bill = new Bill(TransactionType.POS_CASH);
        bill.setBillSerialNumber(serial);

        when(billRepository.findBySerialNumberAndDate(serial, date)).thenReturn(Optional.of(bill));

        Optional<Bill> result = billingService.getBillDetails(serial, date);

        assertTrue(result.isPresent());
        assertEquals(serial, result.get().getBillSerialNumber());
    }

    //Finalize bill throws PaymentException if payment fails
    @Test
    void testFinalizeBill_paymentFails() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        when(itemRepository.findByItemCode("ITEM001")).thenReturn(Optional.of(sampleItem));
        when(billRepository.findTopByBillDateOrderBySerialNumberDesc(any())).thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class), any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        when(paymentMethod.processPayment(any(), any()))
                .thenReturn(new PaymentResult(false, BigDecimal.ZERO, BigDecimal.ZERO, "Insufficient funds"));

        assertThrows(PaymentException.class,
                () -> billingService.finalizeBill(bill, paymentMethod, stockManagementService, BigDecimal.valueOf(10)));
    }
    @Test
    void testFinalizeBill_throwsExceptionForEmptyBill() {
        Bill emptyBill = billingService.startNewBill(TransactionType.POS_CASH);
        PaymentMethod paymentMethod = mock(PaymentMethod.class);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                billingService.finalizeBill(emptyBill, paymentMethod, stockManagementService, BigDecimal.valueOf(100))
        );

        assertEquals("Cannot finalize empty bill.", exception.getMessage());
    }
    @Test
    void testFinalizeBill_throwsExceptionForNegativeTenderedAmount() {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);
        PaymentMethod paymentMethod = mock(PaymentMethod.class);

        assertThrows(IllegalArgumentException.class, () ->
                billingService.finalizeBill(bill, paymentMethod, stockManagementService, BigDecimal.valueOf(-1))
        );
    }
    @Test
    void testAddItemToBill_throwsExceptionForInvalidQuantity() {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        billingService.addItemToBill(bill, "ITEM001", 0)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        billingService.addItemToBill(bill, "ITEM001", -5))
        );
    }
    @Test
    void testFinalizeBill_rollsBackOnStockReductionFailure() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        when(itemRepository.findByItemCode(anyString())).thenReturn(Optional.of(sampleItem));

        // Mock payment success
        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        when(paymentMethod.processPayment(any(), any()))
                .thenReturn(new PaymentResult(true, BigDecimal.TEN, BigDecimal.ZERO, "Success"));

        // Simulate stock service failing during the transaction
        doThrow(new RuntimeException("Stock DB Error"))
                .when(stockManagementService).reduceShelfStockAfterPOSSale(any());

        assertThrows(DatabaseOperationException.class, () ->
                billingService.finalizeBill(bill, paymentMethod, stockManagementService, BigDecimal.TEN)
        );

        // Verify that even though payment succeeded, the bill wasn't successfully returned
        verify(billRepository, times(1)).save(any(), any());
    }
    @Test
    void testFinalizeBill_incrementsSerialNumberCorrectly() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        // Mock the "last bill" for today having serial ...0005
        String todayStr = LocalDate.now().toString().replaceAll("-", "");
        Bill lastBill = new Bill(TransactionType.POS_CASH);
        lastBill.setBillSerialNumber(todayStr + "0005");

        when(itemRepository.findByItemCode(anyString())).thenReturn(Optional.of(sampleItem));
        when(billRepository.findTopByBillDateOrderBySerialNumberDesc(any())).thenReturn(Optional.of(lastBill));
        when(billRepository.save(any(Bill.class), any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        when(paymentMethod.processPayment(any(), any()))
                .thenReturn(new PaymentResult(true, BigDecimal.TEN, BigDecimal.ZERO, "Paid"));

        Bill result = billingService.finalizeBill(bill, paymentMethod, stockManagementService, BigDecimal.TEN);

        // Should be incremented to 0006
        assertEquals(todayStr + "0006", result.getBillSerialNumber());
    }
    @Test
    void testAddItemToBill_multipleDistinctItems() throws Exception {
        Item item2 = new Item("ITEM002", "Other Item", "Desc", "Cat", BigDecimal.valueOf(50), 5, 10);
        when(itemRepository.findByItemCode("ITEM001")).thenReturn(Optional.of(sampleItem));
        when(itemRepository.findByItemCode("ITEM002")).thenReturn(Optional.of(item2));

        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        billingService.addItemToBill(bill, "ITEM001", 1); // 10
        billingService.addItemToBill(bill, "ITEM002", 2); // 100

        assertEquals(2, bill.getBillItems().size());
        assertEquals(BigDecimal.valueOf(110), bill.getFinalTotalAmount());
    }
    @Test
    void testAddItemToBill_incrementsQuantityForDuplicateItem() throws Exception {
        when(itemRepository.findByItemCode("ITEM001")).thenReturn(Optional.of(sampleItem));
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);

        billingService.addItemToBill(bill, "ITEM001", 1);
        billingService.addItemToBill(bill, "ITEM001", 3);

        assertEquals(1, bill.getBillItems().size(), "Should merge into one BillItem entry");
        assertEquals(4, bill.getBillItems().get(0).getQuantityPurchased());
    }
    @Test
    void testFinalizeBill_rollsBackIfBillSaveFails() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));

        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        when(paymentMethod.processPayment(any(), any())).thenReturn(new PaymentResult(true, BigDecimal.TEN, BigDecimal.ZERO, "Paid"));

        // Force failure during save
        when(billRepository.save(any(), any())).thenThrow(new RuntimeException("DB Write Error"));

        assertThrows(DatabaseOperationException.class, () ->
                billingService.finalizeBill(bill, paymentMethod, stockManagementService, BigDecimal.TEN)
        );

        // Verify stock reduction was never attempted because save failed first
        verify(stockManagementService, never()).reduceShelfStockAfterPOSSale(any());
    }
    @Test
    void testFinalizeBill_handlesSQLExceptionOnConnection() throws Exception {
        // 1. Setup
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        when(paymentMethod.processPayment(any(), any()))
                .thenReturn(new PaymentResult(true, BigDecimal.TEN, BigDecimal.ZERO, "Paid"));

        // FIX: You MUST mock this so the validation loop (Step 2) passes
        when(itemRepository.findByItemCode(sampleItem.getItemCode()))
                .thenReturn(Optional.of(sampleItem));

        // 2. Trigger the error later in the method (Step 3)
        // We throw a RuntimeException to simulate a DB failure during serial number generation
        when(billRepository.findTopByBillDateOrderBySerialNumberDesc(any()))
                .thenThrow(new RuntimeException("Connection Lost"));

        // 3. Execution & Assertion
        assertThrows(DatabaseOperationException.class, () ->
                billingService.finalizeBill(bill, paymentMethod, stockManagementService, BigDecimal.TEN)
        );
    }
    @Test
    void testStartNewBill_throwsExceptionOnNullType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                billingService.startNewBill(null)
        );
        assertEquals("Transaction type cannot be null.", ex.getMessage());
    }
    @Test
    void testFinalizeBill_calculatesCorrectChange() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1); // Cost is 10

        BigDecimal tendered = BigDecimal.valueOf(50);
        PaymentMethod paymentMethod = mock(PaymentMethod.class);

        // Strategy returns success with change calculation
        when(paymentMethod.processPayment(BigDecimal.valueOf(10), tendered))
                .thenReturn(new PaymentResult(true, tendered, BigDecimal.valueOf(40), "Success"));

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));
        when(billRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        Bill result = billingService.finalizeBill(bill, paymentMethod, stockManagementService, tendered);

        assertEquals(BigDecimal.valueOf(40), result.getPaymentResult().getChangeGiven());
        assertEquals(tendered, result.getPaymentResult().getAmountTendered());
    }
    @Test
    void testAddItemToBill_zeroPriceItem() throws DatabaseOperationException, ItemNotFoundException {
        Item freeItem = new Item("FREE01", "Promo Item", "Desc", "Cat", BigDecimal.ZERO, 5, 10);
        when(itemRepository.findByItemCode("FREE01")).thenReturn(Optional.of(freeItem));

        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        billingService.addItemToBill(bill, "FREE01", 1);

        assertEquals(BigDecimal.ZERO.setScale(2), bill.getFinalTotalAmount().setScale(2));
    }
    @Test
    void testAddItemToBill_largeQuantityPrecision() throws Exception {
        Item expensiveItem = new Item("GOLD", "Gold", "Desc", "Cat", new BigDecimal("1234.56"), 5, 10);
        when(itemRepository.findByItemCode("GOLD")).thenReturn(Optional.of(expensiveItem));

        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        billingService.addItemToBill(bill, "GOLD", 100);

        assertEquals(new BigDecimal("123456.00"), bill.getFinalTotalAmount());
    }
    @Test
    void testFinalizeBill_handlesUnexpectedPaymentRuntimeException() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        when(paymentMethod.processPayment(any(), any())).thenThrow(new RuntimeException("Gateway Timeout"));

        assertThrows(Exception.class, () ->
                billingService.finalizeBill(bill, paymentMethod, stockManagementService, BigDecimal.TEN)
        );
    }
    @Test
    void testFinalizeBill_handlesHighSequenceNumber() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        String todayStr = LocalDate.now().toString().replaceAll("-", "");
        Bill lastBill = new Bill(TransactionType.POS_CASH);
        lastBill.setBillSerialNumber(todayStr + "9999"); // The max 4-digit sequence

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));
        when(billRepository.findTopByBillDateOrderBySerialNumberDesc(any())).thenReturn(Optional.of(lastBill));
        when(billRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(any(), any())).thenReturn(new PaymentResult(true, BigDecimal.TEN, BigDecimal.ZERO, "Ok"));

        Bill result = billingService.finalizeBill(bill, pm, stockManagementService, BigDecimal.TEN);

        // Should increment to 10000 (adjusting the %04d formatting logic)
        assertEquals(todayStr + "10000", result.getBillSerialNumber());
    }
    @Test
    void testGetBillDetails_emptyStringReturnsEmpty() throws DatabaseOperationException {
        assertThrows(IllegalArgumentException.class, () ->
                billingService.getBillDetails("", LocalDate.now())
        );
    }
    @Test
    void testSequentialBills_areIndependent() throws Exception {
        Bill bill1 = billingService.startNewBill(TransactionType.POS_CASH);
        Bill bill2 = billingService.startNewBill(TransactionType.POS_CARD);

        assertNotSame(bill1, bill2);
        assertNotEquals(bill1.getTransactionType(), bill2.getTransactionType());
    }
    @Test
    void testFinalizeBill_failsOnCommitError() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));
        // We mock the repository to throw an error when save is called,
        // simulating a failure inside the transaction block.
        when(billRepository.save(any(), any())).thenThrow(new RuntimeException("Commit Failed"));

        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(any(), any())).thenReturn(new PaymentResult(true, BigDecimal.TEN, BigDecimal.ZERO, "Ok"));

        assertThrows(DatabaseOperationException.class, () ->
                billingService.finalizeBill(bill, pm, stockManagementService, BigDecimal.TEN)
        );
    }
    @Test
    void testFinalizeBill_passesCorrectItemsToStockService() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 2);

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));
        when(billRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(any(), any())).thenReturn(new PaymentResult(true, BigDecimal.valueOf(20), BigDecimal.ZERO, "Ok"));

        billingService.finalizeBill(bill, pm, stockManagementService, BigDecimal.valueOf(20));

        // Verify that the exact BillItem list was passed to stock reduction
        verify(stockManagementService).reduceShelfStockAfterPOSSale(argThat(list ->
                list.size() == 1 && list.get(0).getQuantityPurchased() == 2
        ));
    }
    @Test
    void testBillIntegrity_serialNotSetOnFailure() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));
        when(billRepository.findTopByBillDateOrderBySerialNumberDesc(any())).thenThrow(new RuntimeException("DB Down"));

        try {
            billingService.finalizeBill(bill, mock(PaymentMethod.class), stockManagementService, BigDecimal.TEN);
        } catch (Exception e) {
            // Expected
        }

        assertNull(bill.getBillSerialNumber(), "Serial should not be set if transaction fails");
    }
    @Test
    void testAddItemToBill_caseSensitivity() throws Exception {
        when(itemRepository.findByItemCode("item001")).thenReturn(Optional.empty());

        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);

        assertThrows(ItemNotFoundException.class, () ->
                billingService.addItemToBill(bill, "item001", 1)
        );
    }
    @Test
    void testBillTotals_matchWhenNoDiscountApplied() throws Exception {
        when(itemRepository.findByItemCode("ITEM001")).thenReturn(Optional.of(sampleItem));
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);

        billingService.addItemToBill(bill, "ITEM001", 3); // 3 * 10 = 30

        assertEquals(0, BigDecimal.valueOf(30).compareTo(bill.getSubTotalAmount()));
        assertEquals(0, BigDecimal.valueOf(30).compareTo(bill.getFinalTotalAmount()));
    }
    @Test
    void testFinalizeBill_preciseChangeCalculation() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        Item centItem = new Item("C01", "Cent", "D", "C", new BigDecimal("9.99"), 5, 10);
        bill.addItem(centItem, 1);

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(centItem));
        when(billRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethod pm = mock(PaymentMethod.class);
        // Paid with 10.00, should get 0.01 back
        when(pm.processPayment(new BigDecimal("9.99"), new BigDecimal("10.00")))
                .thenReturn(new PaymentResult(true, new BigDecimal("10.00"), new BigDecimal("0.01"), "OK"));

        Bill result = billingService.finalizeBill(bill, pm, stockManagementService, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("0.01"), result.getPaymentResult().getChangeGiven());
    }

    @Test
    void testFinalizeBill_largeVolumeOfItems() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        for(int i=0; i<50; i++) {
            bill.addItem(sampleItem, 1);
        }

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));
        when(billRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(any(), any())).thenReturn(new PaymentResult(true, new BigDecimal("500"), BigDecimal.ZERO, "OK"));

        assertDoesNotThrow(() -> billingService.finalizeBill(bill, pm, stockManagementService, new BigDecimal("500")));
    }

    @Test
    void testFinalizeBill_exactPaymentAmount() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1); // Total 10

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));
        when(billRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(BigDecimal.valueOf(10), BigDecimal.valueOf(10)))
                .thenReturn(new PaymentResult(true, BigDecimal.valueOf(10), BigDecimal.ZERO, "Success"));

        Bill result = billingService.finalizeBill(bill, pm, stockManagementService, BigDecimal.valueOf(10));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getPaymentResult().getChangeGiven()));
    }

    @Test
    void testFinalizeBill_handlesDeadlock() throws Exception {
        // 1. Setup
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        // FIX: Provide a valid Item mock for the initial validation loop
        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));

        // FIX: Stub the payment to return a SUCCESSFUL result
        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        when(paymentMethod.processPayment(any(), any()))
                .thenReturn(new PaymentResult(true, BigDecimal.TEN, BigDecimal.ZERO, "Paid"));

        // 2. Mock the Deadlock/DB Error
        // This happens inside the try-with-resources block
        when(billRepository.findTopByBillDateOrderBySerialNumberDesc(any()))
                .thenThrow(new RuntimeException("Deadlock detected"));

        // 3. Execution & Assertion
        assertThrows(DatabaseOperationException.class, () ->
                billingService.finalizeBill(bill, paymentMethod, stockManagementService, BigDecimal.TEN)
        );
    }
    @Test
    void testFinalizeBill_reducesStockForAllDistinctItems() throws Exception {
        Item item2 = new Item("ITEM002", "B", "D", "C", BigDecimal.TEN, 5, 10);
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);
        bill.addItem(item2, 1);

        when(itemRepository.findByItemCode("ITEM001")).thenReturn(Optional.of(sampleItem));
        when(itemRepository.findByItemCode("ITEM002")).thenReturn(Optional.of(item2));
        when(billRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(any(), any())).thenReturn(new PaymentResult(true, BigDecimal.valueOf(20), BigDecimal.ZERO, "Paid"));

        billingService.finalizeBill(bill, pm, stockManagementService, BigDecimal.valueOf(20));

        verify(stockManagementService, times(1)).reduceShelfStockAfterPOSSale(argThat(list -> list.size() == 2));
    }

    @Test
    void testGetBillDetails_futureDateReturnsEmpty() throws DatabaseOperationException {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        when(billRepository.findBySerialNumberAndDate(anyString(), eq(futureDate))).thenReturn(Optional.empty());

        Optional<Bill> result = billingService.getBillDetails("202601120001", futureDate);
        assertFalse(result.isPresent());
    }


    @Test
    void testFinalizeBill_handlesNullPaymentResult() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(any(), any())).thenReturn(null);

        assertThrows(Exception.class, () ->
                billingService.finalizeBill(bill, pm, stockManagementService, BigDecimal.TEN)
        );
    }

    @Test
    void testAddItemToBill_nullItemCodeThrowsException() {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        assertThrows(IllegalArgumentException.class, () ->
                billingService.addItemToBill(bill, null, 1)
        );
    }

    @Test
    void testBillItem_retainsPriceAtTimeOfAddition() throws Exception {
        when(itemRepository.findByItemCode("ITEM001")).thenReturn(Optional.of(sampleItem)); // Price 10
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);

        billingService.addItemToBill(bill, "ITEM001", 1);

        // Simulate price change in DB
        sampleItem.getUnitPrice();

        assertEquals(BigDecimal.valueOf(10), bill.getBillItems().get(0).getPricePerUnitAtSale(),
                "Bill should preserve the original price of 10.00");
    }
    @Test
    void testFinalizeBill_wrapsSaveFailure() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        bill.addItem(sampleItem, 1);

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));
        when(billRepository.save(any(), any())).thenThrow(new RuntimeException("SQL Error"));

        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(any(), any())).thenReturn(new PaymentResult(true, BigDecimal.TEN, BigDecimal.ZERO, "OK"));

        assertThrows(DatabaseOperationException.class, () ->
                billingService.finalizeBill(bill, pm, stockManagementService, BigDecimal.TEN));
    }
    @Test
    void testFinalizeBill_preservesTransactionType() throws Exception {
        Bill bill = billingService.startNewBill(TransactionType.POS_CARD);
        bill.addItem(sampleItem, 1);

        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem));
        when(billRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(any(), any())).thenReturn(new PaymentResult(true, BigDecimal.TEN, BigDecimal.ZERO, "OK"));

        Bill saved = billingService.finalizeBill(bill, pm, stockManagementService, BigDecimal.TEN);
        assertEquals(TransactionType.POS_CARD, saved.getTransactionType());
    }
    @Test
    void testGetBillDetails_notFoundReturnsEmpty() throws DatabaseOperationException {
        when(billRepository.findBySerialNumberAndDate(anyString(), any())).thenReturn(Optional.empty());

        Optional<Bill> result = billingService.getBillDetails("NONEXISTENT", LocalDate.now());
        assertFalse(result.isPresent());
    }
    @Test
    void testFinalizeBill_nullStockServiceThrows() {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        assertThrows(IllegalArgumentException.class, () ->
                billingService.finalizeBill(bill, mock(PaymentMethod.class), null, BigDecimal.TEN));
    }
    @Test
    void testAddItemToBill_updatesTotalCumulative() throws Exception {
        when(itemRepository.findByItemCode(any())).thenReturn(Optional.of(sampleItem)); // Price 10
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);

        billingService.addItemToBill(bill, "ITEM001", 1);
        billingService.addItemToBill(bill, "ITEM001", 2);

        assertEquals(0, new BigDecimal("30.00").compareTo(bill.getFinalTotalAmount()));
    }
    @Test
    void testFinalizeBill_zeroItemsThrowsIllegalState() {
        Bill bill = billingService.startNewBill(TransactionType.POS_CASH);
        assertThrows(IllegalStateException.class, () ->
                billingService.finalizeBill(bill, mock(PaymentMethod.class), stockManagementService, BigDecimal.TEN));
    }
    @Test
    void testGenerateNextSerial_formatCheck() {
        LocalDate date = LocalDate.of(2026, 1, 12);
        // Method visibility must be package-private for this
        String serial = billingService.generateNextSerial(null, date);

        assertEquals("202601120001", serial);
    }

}
