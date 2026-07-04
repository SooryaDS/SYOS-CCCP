package com.syos.application.usecase;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.*;
import com.syos.application.usecase.StockManagementServiceImplementation;
import com.syos.domain.exception.*;
import com.syos.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class StockManagementServiceImplementationTest {

    @Mock private ItemRepository itemRepository;
    @Mock private StockBatchRepository stockBatchRepository;
    @Mock private ShelfStockRepository shelfStockRepository;
    @Mock private WebsiteStockRepository websiteStockRepository;

    @InjectMocks
    private StockManagementServiceImplementation service;

    private Item testItem;

    @BeforeEach
    void setUp() {
        testItem = new Item("I001", "Bread", "Whole Wheat", "Bakery",
                new BigDecimal("250.00"), 10, 50);
    }

    /**
     * NOTE: We add 'throws Exception' to the method signature.
     * This resolves the "Unhandled exception" error in your IDE.
     */
    @Test
    void addItem_ShouldReturnSavedItem_WhenSuccessful() throws Exception {
        // Arrange
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        Item result = service.addItem(testItem);

        // Assert
        assertNotNull(result);
        assertEquals("I001", result.getItemCode());
        verify(itemRepository, times(1)).save(testItem);
    }

    @Test
    void registerNewItem_ShouldThrowException_WhenItemAlreadyExists() throws DatabaseOperationException {
        // Arrange
        when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.registerNewItem("I001", "Bread", "Desc", "Cat",
                    new BigDecimal("100"), 5, 5);
        });
    }

    @Test
    void getItemByCode_ShouldThrowDatabaseOperationException_WhenRepoFails() throws Exception {
        // Arrange
        when(itemRepository.findByItemCode("I001"))
                .thenThrow(new DatabaseOperationException("Connection Lost"));

        // Act & Assert
        assertThrows(DatabaseOperationException.class, () -> {
            service.getItemByCode("I001");
        });
    }

    @Test
    void receiveNewStockBatch_ShouldSaveBatch_WhenItemExists() throws Exception {
        // Arrange
        when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));
        when(stockBatchRepository.save(any(StockBatch.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        StockBatch batch = service.receiveNewStockBatch("I001", 100,
                LocalDate.now().plusDays(7), new BigDecimal("200"));

        // Assert
        assertEquals(100, batch.getCurrentQuantityInStore());
        verify(stockBatchRepository).save(any(StockBatch.class));
    }

    @Test
    void getAvailableShelfStock_ShouldSumShelfAndBatches() throws Exception {
        // Arrange
        when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));
        when(shelfStockRepository.findByItem(testItem)).thenReturn(Optional.of(new ShelfStock(testItem, 10)));

        StockBatch batch1 = new StockBatch();
        batch1.setCurrentQuantityInStore(20);
        batch1.setExpiryDate(LocalDate.now().plusDays(10)); // Not expired

        StockBatch batch2 = new StockBatch();
        batch2.setCurrentQuantityInStore(50);
        batch2.setExpiryDate(LocalDate.now().minusDays(1)); // EXPIRED - should be ignored

        when(stockBatchRepository.findByItem(testItem)).thenReturn(List.of(batch1, batch2));

        // Act
        int total = service.getAvailableShelfStock("I001");

        // Assert: 10 (shelf) + 20 (batch1) = 30. Batch2 is ignored.
        assertEquals(30, total);
    }

    @Test
    void addToShelf_ShouldThrowInsufficientStock_WhenNotEnoughInBatches() throws Exception {
        // Mock static DatabaseConnection to avoid real DB hits
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));
            // Return only 5 items in batches
            StockBatch batch = new StockBatch();
            batch.setCurrentQuantityInStore(5);
            batch.setExpiryDate(LocalDate.now().plusDays(1));
            when(stockBatchRepository.findByItem(testItem)).thenReturn(List.of(batch));

            // Act & Assert: Requesting 10 when only 5 are available
            assertThrows(DatabaseOperationException.class, () -> service.addToShelf("I001", 10));
            verify(mockConn).rollback(); // Verify transaction rollback was attempted
        }
    }

    @Test
    void getAvailableWebsiteStock_ShouldReturnZero_WhenNoStockEntryExists() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCodeForUpdate(eq("I001"), any())).thenReturn(Optional.of(testItem));
            when(websiteStockRepository.findByItemForUpdate(eq(testItem), any())).thenReturn(Optional.empty());

            int available = service.getAvailableWebsiteStock("I001");
            assertEquals(0, available);
        }
    }

    @Test
    void returnStockToShelf_ShouldCreateNewShelfStock_IfNoneExisted() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));
            when(shelfStockRepository.findByItem(testItem)).thenReturn(Optional.empty());

            service.returnStockToShelf("I001", 15);

            // Verify that a new ShelfStock object with quantity 15 was saved
            verify(shelfStockRepository).save(argThat(ss -> ss.getQuantityOnShelf() == 15), eq(mockConn));
        }
    }

    @Test
    void removeFromShelf_ShouldThrowItemNotFound_WhenCodeIsInvalid() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb =
                     mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {

            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            // Arrange
            when(itemRepository.findByItemCode("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            // Change DatabaseOperationException.class -> ItemNotFoundException.class
            assertThrows(ItemNotFoundException.class, () ->
                    service.removeFromShelf("INVALID", 10)
            );
        }
    }
    @Test
    void addToShelf_ShouldDeductFromMultipleBatches_FIFO() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));

            // Batch 1: 10 units, expires in 2 days (Should be used first)
            StockBatch b1 = new StockBatch();
            b1.setCurrentQuantityInStore(10);
            b1.setExpiryDate(LocalDate.now().plusDays(2));

            // Batch 2: 20 units, expires in 10 days
            StockBatch b2 = new StockBatch();
            b2.setCurrentQuantityInStore(20);
            b2.setExpiryDate(LocalDate.now().plusDays(10));

            when(stockBatchRepository.findByItem(testItem)).thenReturn(List.of(b1, b2));
            when(shelfStockRepository.findByItem(testItem)).thenReturn(Optional.empty());

            // Act: Request 15 units. It should take 10 from b1 and 5 from b2.
            service.addToShelf("I001", 15);

            // Assert
            assertEquals(0, b1.getCurrentQuantityInStore());
            assertEquals(15, b2.getCurrentQuantityInStore());
            verify(stockBatchRepository, times(2)).save(any(StockBatch.class));
        }
    }

    @Test
    void addToWebsiteStock_ShouldThrowInsufficientStock_WhenBatchesAreExpired() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            // 1. Arrange
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));

            StockBatch expiredBatch = new StockBatch();
            expiredBatch.setCurrentQuantityInStore(100);
            expiredBatch.setExpiryDate(LocalDate.now().minusDays(5)); // Expired

            when(stockBatchRepository.findByItem(testItem)).thenReturn(List.of(expiredBatch));

            // 2. Act & Assert (Trigger the method call here!)
            assertThrows(InsufficientStockException.class, () ->
                    service.addToWebsiteStock("I001", 10)
            );

            // 3. Verify
            verify(mockConn, atLeastOnce()).rollback();
        }
    }
    @Test
    void removeFromWebsiteStock_ShouldThrowInsufficientStock_WhenQuantityTooHigh() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCodeForUpdate(eq("I001"), any())).thenReturn(Optional.of(testItem));

            // Only 5 available online
            WebsiteStock webStock = new WebsiteStock(testItem, 5);
            when(websiteStockRepository.findByItemForUpdate(eq(testItem), any())).thenReturn(Optional.of(webStock));

            // Act & Assert
            assertThrows(InsufficientStockException.class, () -> service.removeFromWebsiteStock("I001", 10));
        }
    }

    @Test
    void getWebsiteStockByItemCode_ShouldReturnEmpty_WhenItemDoesNotExist() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCodeForUpdate(eq("NONEXISTENT"), any())).thenReturn(Optional.empty());

            Optional<WebsiteStock> result = service.getWebsiteStockByItemCode("NONEXISTENT");

            assertTrue(result.isEmpty());
        }
    }

    @Test
    void receiveNewStockBatch_ShouldSetCurrentDateAsReceivedDate() throws Exception {
        when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));
        when(stockBatchRepository.save(any(StockBatch.class))).thenAnswer(i -> i.getArguments()[0]);

        StockBatch result = service.receiveNewStockBatch("I001", 50, LocalDate.now().plusMonths(1), new BigDecimal("100"));

        // Verify the logic inside receiveNewStockBatch that sets today's date
        assertEquals(LocalDate.now(), result.getReceivedDate());
        assertEquals(50, result.getReceivedQuantity());
    }

    @Test
    void getAllShelfStock_ShouldReturnEmptyList_WhenNoStockRegistered() throws Exception {
        when(shelfStockRepository.findAll()).thenReturn(new ArrayList<>());
        List<ShelfStock> result = service.getAllShelfStock();
        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableWebsiteStock_ShouldThrowItemNotFound_WhenCodeIsMissing() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCodeForUpdate(eq("MISSING"), any())).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class, () -> service.getAvailableWebsiteStock("MISSING"));
        }
    }

    @Test
    void addToShelf_ShouldIgnoreExpiredBatches_EvenIfQuantityIsLarge() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));

            StockBatch expired = new StockBatch();
            expired.setCurrentQuantityInStore(500);
            expired.setExpiryDate(LocalDate.now().minusDays(10)); // Expired

            when(stockBatchRepository.findByItem(testItem)).thenReturn(List.of(expired));

            // Should throw because it won't use the expired 500 units
            assertThrows(DatabaseOperationException.class, () -> service.addToShelf("I001", 10));
        }
    }

    @Test
    void getStockBatchesByItemCode_ShouldHandleDatabaseException() throws Exception {
        when(itemRepository.findByItemCode("I001")).thenThrow(new DatabaseOperationException("DB error"));

        assertThrows(DatabaseOperationException.class, () -> service.getStockBatchesByItemCode("I001"));
    }

    @Test
    void updateItem_ShouldThrowException_WhenItemIsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.updateItem(null));
    }

    @Test
    void getAvailableShelfStock_ShouldReturnZero_WhenNoShelfOrBatchStockExists() throws Exception {
        // Arrange
        when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));
        when(shelfStockRepository.findByItem(testItem)).thenReturn(Optional.empty());
        when(stockBatchRepository.findByItem(testItem)).thenReturn(Collections.emptyList());

        // Act
        int available = service.getAvailableShelfStock("I001");

        // Assert
        assertEquals(0, available);
    }

    @Test
    void removeFromShelf_ShouldThrowInsufficientStock_WhenShelfStockIsZero() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            // Arrange
            when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));
            when(shelfStockRepository.findByItem(testItem)).thenReturn(Optional.empty()); // No shelf stock record

            // Act & Assert
            assertThrows(InsufficientStockException.class, () -> service.removeFromShelf("I001", 1));
            verify(mockConn).rollback();
        }
    }

    @Test
    void updateItem_ShouldThrowDatabaseException_WhenRepositoryFails() throws Exception {
        // Arrange
        when(itemRepository.update(any(Item.class))).thenThrow(new RuntimeException("SQL Error"));

        // Act & Assert
        assertThrows(DatabaseOperationException.class, () -> service.updateItem(testItem));
    }

    @Test
    void getShelfStockByItemCode_ShouldReturnEmpty_WhenItemCodeIsMissing() throws Exception {
        // Arrange
        when(itemRepository.findByItemCode("I999")).thenReturn(Optional.empty());

        // Act
        Optional<ShelfStock> result = service.getShelfStockByItemCode("I999");

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(shelfStockRepository); // Should return early
    }

    @Test
    void registerNewItem_ShouldThrowIllegalArgument_WhenReorderQuantityIsNegative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.registerNewItem("I001", "Bread", "D", "C", BigDecimal.TEN, 10, -5));
    }

    @Test
    void addToShelf_ShouldSortBatchesByExpiryDate_FIFO() throws Exception {
        try (MockedStatic<com.zaxxer.hikari.HikariDataSource> mockedDs = mockStatic(com.zaxxer.hikari.HikariDataSource.class);
             MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {

            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));

            // Batch A: Expires later
            StockBatch batchA = new StockBatch();
            batchA.setCurrentQuantityInStore(10);
            batchA.setExpiryDate(LocalDate.now().plusDays(20));

            // Batch B: Expires sooner (Should be picked first)
            StockBatch batchB = new StockBatch();
            batchB.setCurrentQuantityInStore(10);
            batchB.setExpiryDate(LocalDate.now().plusDays(5));

            // Mockito returns them in "wrong" order, service should sort them
            when(stockBatchRepository.findByItem(testItem)).thenReturn(List.of(batchA, batchB));
            when(shelfStockRepository.findByItem(testItem)).thenReturn(Optional.empty());

            // Act: Deduct 5
            service.addToShelf("I001", 5);

            // Assert: Batch B (earlier expiry) should be reduced to 5, Batch A stays 10
            assertEquals(5, batchB.getCurrentQuantityInStore());
            assertEquals(10, batchA.getCurrentQuantityInStore());
        }
    }

    @Test
    void getAllShelfStock_ShouldReturnAllRecords_WhenMultipleItemsExist() throws Exception {
        // Arrange
        List<ShelfStock> shelfList = List.of(
                new ShelfStock(testItem, 10),
                new ShelfStock(new Item("I002", "Milk", "D", "C", BigDecimal.ONE, 1, 1), 20)
        );
        when(shelfStockRepository.findAll()).thenReturn(shelfList);

        // Act
        List<ShelfStock> result = service.getAllShelfStock();

        // Assert
        assertEquals(2, result.size());
        verify(shelfStockRepository, times(1)).findAll();
    }

    @Test
    void updateItem_ShouldThrowIllegalArgument_WhenItemCodeIsNull() {
        // Arrange
        Item nullCodeItem = new Item();
        nullCodeItem.setName("Invalid");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.updateItem(nullCodeItem));
    }

    @Test
    void getAvailableWebsiteStock_ShouldReturnZero_WhenWebsiteStockRecordMissing() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCodeForUpdate(eq("I001"), any())).thenReturn(Optional.of(testItem));
            // No website stock entry for this item
            when(websiteStockRepository.findByItemForUpdate(eq(testItem), any())).thenReturn(Optional.empty());

            // Act
            int result = service.getAvailableWebsiteStock("I001");

            // Assert
            assertEquals(0, result);
        }
    }

    @Test
    void returnStockToShelf_ShouldThrowDatabaseOperationException_WhenConnectionFails() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            // Mock connection failure
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenThrow(new java.sql.SQLException("Connection Refused"));

            // Act & Assert
            assertThrows(DatabaseOperationException.class, () -> service.returnStockToShelf("I001", 10));
        }
    }

    @Test
    void addToShelf_ShouldPrioritizeEarlierReceivedDate_WhenExpiryDatesAreEqual() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCode("I001")).thenReturn(Optional.of(testItem));

            // Batch 1: Expires in 10 days, Received 5 days ago
            StockBatch b1 = new StockBatch();
            b1.setCurrentQuantityInStore(10);
            b1.setExpiryDate(LocalDate.now().plusDays(10));
            b1.setReceivedDate(LocalDate.now().minusDays(5));

            // Batch 2: Expires in 10 days, Received today (Should be used second)
            StockBatch b2 = new StockBatch();
            b2.setCurrentQuantityInStore(10);
            b2.setExpiryDate(LocalDate.now().plusDays(10));
            b2.setReceivedDate(LocalDate.now());

            when(stockBatchRepository.findByItem(testItem)).thenReturn(List.of(b2, b1)); // Return in "wrong" order
            when(shelfStockRepository.findByItem(testItem)).thenReturn(Optional.empty());

            // Act: Take 5 units
            service.addToShelf("I001", 5);

            // Assert: b1 should be reduced because it was received earlier
            assertEquals(5, b1.getCurrentQuantityInStore());
            assertEquals(10, b2.getCurrentQuantityInStore());
        }
    }

    @Test
    void removeFromWebsiteStock_ShouldRollbackAndThrow_WhenWebsiteStockRecordIsMissing() throws Exception {
        try (MockedStatic<com.syos.adapter.out.util.DatabaseConnection> mockedDb = mockStatic(com.syos.adapter.out.util.DatabaseConnection.class)) {
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            mockedDb.when(com.syos.adapter.out.util.DatabaseConnection::getConnection).thenReturn(mockConn);

            when(itemRepository.findByItemCodeForUpdate(eq("I001"), any())).thenReturn(Optional.of(testItem));
            // Simulate missing WebsiteStock entry
            when(websiteStockRepository.findByItemForUpdate(eq(testItem), any())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(InsufficientStockException.class, () -> service.removeFromWebsiteStock("I001", 10));
            verify(mockConn).rollback();
        }
    }

    @Test
    void getAllWebsiteStock_ShouldReturnMultipleRecords_WhenTheyExist() throws Exception {
        // Arrange
        List<WebsiteStock> webStocks = List.of(
                new WebsiteStock(testItem, 50),
                new WebsiteStock(new Item("I002", "Juice", "D", "C", BigDecimal.TEN, 1, 1), 30)
        );
        when(websiteStockRepository.findAll()).thenReturn(webStocks);

        // Act
        List<WebsiteStock> result = service.getAllWebsiteStock();

        // Assert
        assertEquals(2, result.size());
        assertEquals(50, result.get(0).getQuantityAvailableOnline());
    }

    @Test
    void getStockBatchById_ShouldReturnEmpty_WhenIdDoesNotExist() throws Exception {
        // Arrange
        when(stockBatchRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<StockBatch> result = service.getStockBatchById(999);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void registerNewItem_ShouldThrowIllegalArgument_WhenCategoryIsEmptyString() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.registerNewItem("I001", "Bread", "Desc", " ", BigDecimal.TEN, 10, 10));
    }
}
