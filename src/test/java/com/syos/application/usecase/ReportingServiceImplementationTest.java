package com.syos.application.usecase;

import com.syos.adapter.dto.BatchStockReportItemDTO;
import com.syos.adapter.dto.BillSummaryDTO;
import com.syos.adapter.dto.DailySalesReportDTO;
import com.syos.adapter.dto.DailySalesReportItemDTO;
import com.syos.adapter.dto.ReorderReportItemDTO;
import com.syos.adapter.dto.ReshelvingNeedsItemDTO;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.BillRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ShelfStockRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.StockBatchRepository;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.model.Item;
import com.syos.domain.model.ShelfStock;
import com.syos.domain.model.StockBatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReportingServiceImplementationTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private StockBatchRepository stockBatchRepository;
    @Mock
    private ShelfStockRepository shelfStockRepository;

    @InjectMocks
    private ReportingServiceImplementation reportingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should generate daily sales report successfully with items and total revenue")
    void shouldGenerateDailySalesReportSuccessfully() throws DatabaseOperationException {
        LocalDate testDate = LocalDate.of(2023, 1, 15);
        List<DailySalesReportItemDTO> salesItems = new ArrayList<>();
        salesItems.add(new DailySalesReportItemDTO("ITEM001", "Item A", 2, BigDecimal.valueOf(40.0)));
        salesItems.add(new DailySalesReportItemDTO("ITEM002", "Item B", 1, BigDecimal.valueOf(25.0)));

        when(billRepository.findSalesDataByDateAndType(testDate, null)).thenReturn(salesItems);

        DailySalesReportDTO report = reportingService.generateDailySalesReport(testDate, Optional.empty());

        assertNotNull(report);
        assertEquals(2, report.getSaleItems().size());
        assertEquals(BigDecimal.valueOf(65.0), report.getOverallTotalRevenue());
        verify(billRepository, times(1)).findSalesDataByDateAndType(testDate, null);
    }

    @Test
    @DisplayName("Should generate daily sales report successfully with transaction type filter")
    void shouldGenerateDailySalesReportWithFilterSuccessfully() throws DatabaseOperationException {
        LocalDate testDate = LocalDate.of(2023, 1, 15);
        List<DailySalesReportItemDTO> salesItems = new ArrayList<>();
        salesItems.add(new DailySalesReportItemDTO("ITEM001", "Item A", 2, BigDecimal.valueOf(40.0)));

        when(billRepository.findSalesDataByDateAndType(testDate, TransactionType.ONLINE_SALE)).thenReturn(salesItems);

        DailySalesReportDTO report = reportingService.generateDailySalesReport(testDate, Optional.of(TransactionType.ONLINE_SALE));

        assertNotNull(report);
        assertEquals(1, report.getSaleItems().size());
        assertEquals(BigDecimal.valueOf(40.0), report.getOverallTotalRevenue());
        verify(billRepository, times(1)).findSalesDataByDateAndType(testDate, TransactionType.ONLINE_SALE);
    }

    @Test
    @DisplayName("Should return empty daily sales report if no sales data for the given date")
    void shouldReturnEmptyDailySalesReportIfNoSalesData() throws DatabaseOperationException {
        LocalDate testDate = LocalDate.of(2023, 1, 15);
        when(billRepository.findSalesDataByDateAndType(testDate, null)).thenReturn(Collections.emptyList());

        DailySalesReportDTO report = reportingService.generateDailySalesReport(testDate, Optional.empty());

        assertNotNull(report);
        assertTrue(report.getSaleItems().isEmpty());
        assertEquals(BigDecimal.ZERO, report.getOverallTotalRevenue());
        verify(billRepository, times(1)).findSalesDataByDateAndType(testDate, null);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if date is null for daily sales report")
    void shouldThrowIllegalArgumentExceptionWhenDateIsNullForDailySalesReport() {
        assertThrows(IllegalArgumentException.class, () -> reportingService.generateDailySalesReport(null, Optional.empty()));
        verifyNoInteractions(billRepository);
    }

    @Test
    @DisplayName("Should throw DatabaseOperationException when billRepository fails to find sales data")
    void shouldThrowDatabaseOperationExceptionWhenBillRepoFailsForDailySalesReport() throws DatabaseOperationException {
        LocalDate testDate = LocalDate.of(2023, 1, 15);
        when(billRepository.findSalesDataByDateAndType(testDate, null)).thenThrow(new DatabaseOperationException("DB error"));

        assertThrows(DatabaseOperationException.class, () -> reportingService.generateDailySalesReport(testDate, Optional.empty()));
        verify(billRepository, times(1)).findSalesDataByDateAndType(testDate, null);
    }

    @Test
    @DisplayName("Should generate daily sales report excluding items with zero or negative quantity")
    void shouldGenerateDailySalesReportExcludingZeroAndNegativeQuantityItems() throws DatabaseOperationException {
        LocalDate testDate = LocalDate.of(2023, 1, 16);
        List<DailySalesReportItemDTO> salesItems = new ArrayList<>();
        salesItems.add(new DailySalesReportItemDTO("ITEM003", "Item C", 5, BigDecimal.valueOf(50.0)));
        salesItems.add(new DailySalesReportItemDTO("ITEM004", "Item D", 0, BigDecimal.valueOf(0.0)));
        salesItems.add(new DailySalesReportItemDTO("ITEM005", "Item E", -2, BigDecimal.valueOf(-20.0)));
        salesItems.add(new DailySalesReportItemDTO("ITEM006", "Item F", 3, BigDecimal.valueOf(36.0)));

        when(billRepository.findSalesDataByDateAndType(testDate, null)).thenReturn(salesItems);

        DailySalesReportDTO report = reportingService.generateDailySalesReport(testDate, Optional.empty());

        assertNotNull(report);
        assertEquals(2, report.getSaleItems().size());
        assertEquals(BigDecimal.valueOf(86.0), report.getOverallTotalRevenue());
        verify(billRepository, times(1)).findSalesDataByDateAndType(testDate, null);
    }


    @Test
    @DisplayName("Should generate reorder level report with items below reorder level")
    void shouldGenerateReorderLevelReport() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Description A", "Category A", BigDecimal.valueOf(10.0), 5, 20); // Reorder level 5
        Item item2 = new Item("ITEM002", "Product B", "Description B", "Category B", BigDecimal.valueOf(15.0), 10, 30); // Reorder level 10
        Item item3 = new Item("ITEM003", "Product C", "Description C", "Category C", BigDecimal.valueOf(5.0), 20, 10); // Reorder level 20

        List<Item> allItems = List.of(item1, item2, item3);

        StockBatch batch1 = new StockBatch(1L, item1, 50, 2, LocalDate.now().plusMonths(6), BigDecimal.ZERO, LocalDate.now());
        StockBatch batch2 = new StockBatch(2L, item2, 30, 8, LocalDate.now().plusMonths(6), BigDecimal.ZERO, LocalDate.now());

        ShelfStock shelfStock1 = new ShelfStock(item1, 0);
        ShelfStock shelfStock2 = new ShelfStock(item2, 0);
        ShelfStock shelfStock3 = new ShelfStock(item3, 25);

        when(itemRepository.findAll()).thenReturn(allItems);
        when(stockBatchRepository.findByItem(item1)).thenReturn(List.of(batch1));
        when(stockBatchRepository.findByItem(item2)).thenReturn(List.of(batch2));
        when(stockBatchRepository.findByItem(item3)).thenReturn(Collections.emptyList());

        when(shelfStockRepository.findByItem(item1)).thenReturn(Optional.of(shelfStock1));
        when(shelfStockRepository.findByItem(item2)).thenReturn(Optional.of(shelfStock2));
        when(shelfStockRepository.findByItem(item3)).thenReturn(Optional.of(shelfStock3));


        List<ReorderReportItemDTO> report = reportingService.generateReorderLevelReport();

        assertNotNull(report);
        assertEquals(2, report.size());

        ReorderReportItemDTO item1Report = report.stream().filter(r -> r.getItemCode().equals("ITEM001")).findFirst().orElse(null);
        assertNotNull(item1Report);
        assertEquals("Product A", item1Report.getItemName());
        assertEquals(2, item1Report.getCurrentTotalStock());
        assertEquals(5, item1Report.getReorderLevelThreshold());

        ReorderReportItemDTO item2Report = report.stream().filter(r -> r.getItemCode().equals("ITEM002")).findFirst().orElse(null);
        assertNotNull(item2Report);
        assertEquals("Product B", item2Report.getItemName());
        assertEquals(8, item2Report.getCurrentTotalStock());
        assertEquals(10, item2Report.getReorderLevelThreshold());

        verify(itemRepository, times(1)).findAll();
        verify(stockBatchRepository, times(3)).findByItem(any(Item.class));
        verify(shelfStockRepository, times(3)).findByItem(any(Item.class));
    }

    @Test
    @DisplayName("Should return empty reorder level report if all items are above reorder level")
    void shouldReturnEmptyReorderLevelReportIfAllAboveLevel() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Description A", "Category A", BigDecimal.valueOf(10.0), 5, 20);
        Item item2 = new Item("ITEM002", "Product B", "Description B", "Category B", BigDecimal.valueOf(15.0), 10, 30);

        List<Item> allItems = List.of(item1, item2);

        StockBatch batch1 = new StockBatch(1L, item1, 50, 10, LocalDate.now().plusMonths(6), BigDecimal.ZERO, LocalDate.now());
        StockBatch batch2 = new StockBatch(2L, item2, 30, 20, LocalDate.now().plusMonths(6), BigDecimal.ZERO, LocalDate.now());

        ShelfStock shelfStock1 = new ShelfStock(item1, 0);
        ShelfStock shelfStock2 = new ShelfStock(item2, 0);

        when(itemRepository.findAll()).thenReturn(allItems);
        when(stockBatchRepository.findByItem(item1)).thenReturn(List.of(batch1));
        when(stockBatchRepository.findByItem(item2)).thenReturn(List.of(batch2));
        when(shelfStockRepository.findByItem(item1)).thenReturn(Optional.of(shelfStock1));
        when(shelfStockRepository.findByItem(item2)).thenReturn(Optional.of(shelfStock2));

        List<ReorderReportItemDTO> report = reportingService.generateReorderLevelReport();

        assertNotNull(report);
        assertTrue(report.isEmpty());

        verify(itemRepository, times(1)).findAll();
        verify(stockBatchRepository, times(2)).findByItem(any(Item.class));
        verify(shelfStockRepository, times(2)).findByItem(any(Item.class));
    }

    @Test
    @DisplayName("Should throw DatabaseOperationException when itemRepository fails to find all items for reorder report")
    void shouldThrowDatabaseOperationExceptionWhenItemRepoFailsForReorderReport() throws DatabaseOperationException {
        when(itemRepository.findAll()).thenThrow(new DatabaseOperationException("DB error"));

        assertThrows(DatabaseOperationException.class, () -> reportingService.generateReorderLevelReport());
        verify(itemRepository, times(1)).findAll();
        verifyNoInteractions(stockBatchRepository, shelfStockRepository);
    }

    @Test
    @DisplayName("Should throw DatabaseOperationException when stockBatchRepository fails for reorder report")
    void shouldThrowDatabaseOperationExceptionWhenStockBatchRepoFailsForReorderReport() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Description A", "Category A", BigDecimal.valueOf(10.0), 5, 20);
        when(itemRepository.findAll()).thenReturn(List.of(item1));
        when(stockBatchRepository.findByItem(item1)).thenThrow(new DatabaseOperationException("DB error"));

        assertThrows(DatabaseOperationException.class, () -> reportingService.generateReorderLevelReport());
        verify(itemRepository, times(1)).findAll();
        verify(stockBatchRepository, times(1)).findByItem(item1);
        verifyNoInteractions(shelfStockRepository);
    }

    @Test
    @DisplayName("Should handle item with no stock batches or shelf stock in reorder level report")
    void shouldHandleItemWithNoStockOrShelfStockInReorderReport() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Description A", "Category A", BigDecimal.valueOf(10.0), 5, 20); // Reorder level 5
        Item item2 = new Item("ITEM002", "Product B", "Description B", "Category B", BigDecimal.valueOf(15.0), 10, 30); // Reorder level 10

        List<Item> allItems = List.of(item1, item2);

        when(itemRepository.findAll()).thenReturn(allItems);
        when(stockBatchRepository.findByItem(item1)).thenReturn(Collections.emptyList());
        when(shelfStockRepository.findByItem(item1)).thenReturn(Optional.empty());

        StockBatch batch2 = new StockBatch(2L, item2, 10, 20, LocalDate.now().plusMonths(6), BigDecimal.ZERO, LocalDate.now());
        ShelfStock shelfStock2 = new ShelfStock(item2, 15);
        when(stockBatchRepository.findByItem(item2)).thenReturn(List.of(batch2));
        when(shelfStockRepository.findByItem(item2)).thenReturn(Optional.of(shelfStock2));


        List<ReorderReportItemDTO> report = reportingService.generateReorderLevelReport();

        assertNotNull(report);
        assertEquals(1, report.size());
        assertEquals("ITEM001", report.get(0).getItemCode());
        assertEquals(0, report.get(0).getCurrentTotalStock());
        assertEquals(5, report.get(0).getReorderLevelThreshold());

        verify(itemRepository, times(1)).findAll();
        verify(stockBatchRepository, times(2)).findByItem(any(Item.class));
        verify(shelfStockRepository, times(2)).findByItem(any(Item.class));
    }


    @Test
    @DisplayName("Should generate batch stock report successfully and sorted")
    void shouldGenerateBatchStockReportSuccessfullyAndSorted() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Description A", "Category A", BigDecimal.valueOf(10.0), 5, 20);
        Item item2 = new Item("ITEM002", "Product B", "Description B", "Category B", BigDecimal.valueOf(15.0), 10, 30);

        StockBatch batch1 = new StockBatch(1L, item1, 100, 90, LocalDate.of(2024, 1, 1), BigDecimal.ZERO, LocalDate.of(2023, 1, 1));
        StockBatch batch2 = new StockBatch(2L, item2, 50, 45, LocalDate.of(2024, 2, 1), BigDecimal.ZERO, LocalDate.now());
        StockBatch batch3 = new StockBatch(3L, item1, 75, 70, LocalDate.of(2024, 1, 5), BigDecimal.ZERO, LocalDate.of(2023, 1, 5));

        List<StockBatch> allBatches = List.of(batch1, batch2, batch3);
        when(stockBatchRepository.findAll()).thenReturn(allBatches);

        List<BatchStockReportItemDTO> report = reportingService.generateBatchStockReport();

        assertNotNull(report);
        assertEquals(3, report.size());

        assertEquals("ITEM001", report.get(0).getItemCode());
        assertEquals(LocalDate.of(2023, 1, 1), report.get(0).getReceivedDate());
        assertEquals(1L, report.get(0).getBatchId());

        assertEquals("ITEM001", report.get(1).getItemCode());
        assertEquals(LocalDate.of(2023, 1, 5), report.get(1).getReceivedDate());
        assertEquals(3L, report.get(1).getBatchId());

        assertEquals("ITEM002", report.get(2).getItemCode());
        assertEquals(LocalDate.now(), report.get(2).getReceivedDate());
        assertEquals(2L, report.get(2).getBatchId());

        assertEquals("Product A", report.get(0).getItemName());
        assertEquals(90, report.get(0).getCurrentQuantityInStore());

        verify(stockBatchRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty batch stock report if no batches exist")
    void shouldReturnEmptyBatchStockReportIfNoBatches() throws DatabaseOperationException {
        when(stockBatchRepository.findAll()).thenReturn(Collections.emptyList());

        List<BatchStockReportItemDTO> report = reportingService.generateBatchStockReport();

        assertNotNull(report);
        assertTrue(report.isEmpty());
        verify(stockBatchRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should throw DatabaseOperationException when stockBatchRepository fails for batch stock report")
    void shouldThrowDatabaseOperationExceptionWhenStockBatchRepoFailsForBatchStockReport() throws DatabaseOperationException {
        when(stockBatchRepository.findAll()).thenThrow(new DatabaseOperationException("DB error"));

        assertThrows(DatabaseOperationException.class, () -> reportingService.generateBatchStockReport());
        verify(stockBatchRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should generate batch stock report with complex sorting by itemCode, receivedDate, then batchId")
    void shouldGenerateBatchStockReportWithComplexSorting() throws DatabaseOperationException {
        Item itemA = new Item("ITEMA", "Product A", "Desc A", "Cat A", BigDecimal.TEN, 5, 20);
        Item itemB = new Item("ITEMB", "Product B", "Desc B", "Cat B", BigDecimal.TEN, 5, 20);

        StockBatch batchA1 = new StockBatch(10L, itemA, 50, 45, LocalDate.of(2025, 3, 1), BigDecimal.ZERO, LocalDate.of(2024, 1, 10));
        StockBatch batchB1 = new StockBatch(20L, itemB, 30, 25, LocalDate.of(2025, 4, 1), BigDecimal.ZERO, LocalDate.of(2024, 1, 5));
        StockBatch batchA2 = new StockBatch(11L, itemA, 60, 55, LocalDate.of(2025, 3, 15), BigDecimal.ZERO, LocalDate.of(2024, 1, 10));
        StockBatch batchA3 = new StockBatch(12L, itemA, 40, 35, LocalDate.of(2025, 2, 28), BigDecimal.ZERO, LocalDate.of(2024, 1, 1));
        StockBatch batchB2 = new StockBatch(21L, itemB, 70, 65, LocalDate.of(2025, 4, 15), BigDecimal.ZERO, LocalDate.of(2024, 1, 5));

        List<StockBatch> allBatches = List.of(batchA1, batchB1, batchA2, batchA3, batchB2);
        when(stockBatchRepository.findAll()).thenReturn(allBatches);

        List<BatchStockReportItemDTO> report = reportingService.generateBatchStockReport();

        assertNotNull(report);
        assertEquals(5, report.size());

        assertEquals("ITEMA", report.get(0).getItemCode());
        assertEquals(LocalDate.of(2024, 1, 1), report.get(0).getReceivedDate());
        assertEquals(12L, report.get(0).getBatchId());

        assertEquals("ITEMA", report.get(1).getItemCode());
        assertEquals(LocalDate.of(2024, 1, 10), report.get(1).getReceivedDate());
        assertEquals(10L, report.get(1).getBatchId());

        assertEquals("ITEMA", report.get(2).getItemCode());
        assertEquals(LocalDate.of(2024, 1, 10), report.get(2).getReceivedDate());
        assertEquals(11L, report.get(2).getBatchId());

        assertEquals("ITEMB", report.get(3).getItemCode());
        assertEquals(LocalDate.of(2024, 1, 5), report.get(3).getReceivedDate());
        assertEquals(20L, report.get(3).getBatchId());

        assertEquals("ITEMB", report.get(4).getItemCode());
        assertEquals(LocalDate.of(2024, 1, 5), report.get(4).getReceivedDate());
        assertEquals(21L, report.get(4).getBatchId());

        verify(stockBatchRepository, times(1)).findAll();
    }


    @Test
    @DisplayName("Should generate daily reshelving needs report with items below reorder quantity")
    void shouldGenerateDailyReshelvingNeedsReport() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Description A", "Category A", BigDecimal.valueOf(10.0), 5, 20); // reorderQuantity = 20
        Item item2 = new Item("ITEM002", "Product B", "Description B", "Category B", BigDecimal.valueOf(15.0), 10, 30); // reorderQuantity = 30
        Item item3 = new Item("ITEM003", "Product C", "Description C", "Category C", BigDecimal.valueOf(5.0), 20, 10); // reorderQuantity = 10

        List<Item> allItems = List.of(item1, item2, item3);

        ShelfStock shelfStock1 = new ShelfStock(item1, 15);
        ShelfStock shelfStock2 = new ShelfStock(item2, 25);
        ShelfStock shelfStock3 = new ShelfStock(item3, 10);

        when(itemRepository.findAll()).thenReturn(allItems);
        when(shelfStockRepository.findByItem(item1)).thenReturn(Optional.of(shelfStock1));
        when(shelfStockRepository.findByItem(item2)).thenReturn(Optional.of(shelfStock2));
        when(shelfStockRepository.findByItem(item3)).thenReturn(Optional.of(shelfStock3));


        List<ReshelvingNeedsItemDTO> report = reportingService.generateDailyReshelvingNeedsReport();

        assertNotNull(report);
        assertEquals(2, report.size());

        ReshelvingNeedsItemDTO item1Report = report.stream().filter(r -> r.getItemCode().equals("ITEM001")).findFirst().orElse(null);
        assertNotNull(item1Report);
        assertEquals("Product A", item1Report.getItemName());
        assertEquals(15, item1Report.getCurrentShelfQuantity());
        assertEquals(20, item1Report.getMinShelfStockThreshold());
        assertEquals(5, item1Report.getQuantityToReshelve());


        ReshelvingNeedsItemDTO item2Report = report.stream().filter(r -> r.getItemCode().equals("ITEM002")).findFirst().orElse(null);
        assertNotNull(item2Report);
        assertEquals("Product B", item2Report.getItemName());
        assertEquals(25, item2Report.getCurrentShelfQuantity());
        assertEquals(30, item2Report.getMinShelfStockThreshold());
        assertEquals(5, item2Report.getQuantityToReshelve());

        verify(itemRepository, times(1)).findAll();
        verify(shelfStockRepository, times(3)).findByItem(any(Item.class));
    }

    @Test
    @DisplayName("Should return empty daily reshelving needs report if all items are above reorder quantity")
    void shouldReturnEmptyDailyReshelvingNeedsReportIfAllAboveQuantity() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Description A", "Category A", BigDecimal.valueOf(10.0), 5, 20);
        Item item2 = new Item("ITEM002", "Product B", "Description B", "Category B", BigDecimal.valueOf(15.0), 10, 30);

        List<Item> allItems = List.of(item1, item2);

        ShelfStock shelfStock1 = new ShelfStock(item1, 20);
        ShelfStock shelfStock2 = new ShelfStock(item2, 35);

        when(itemRepository.findAll()).thenReturn(allItems);
        when(shelfStockRepository.findByItem(item1)).thenReturn(Optional.of(shelfStock1));
        when(shelfStockRepository.findByItem(item2)).thenReturn(Optional.of(shelfStock2));

        List<ReshelvingNeedsItemDTO> report = reportingService.generateDailyReshelvingNeedsReport();

        assertNotNull(report);
        assertTrue(report.isEmpty());

        verify(itemRepository, times(1)).findAll();
        verify(shelfStockRepository, times(2)).findByItem(any(Item.class));
    }

    @Test
    @DisplayName("Should throw DatabaseOperationException when itemRepository fails for reshelving report")
    void shouldThrowDatabaseOperationExceptionWhenItemRepoFailsForReshelvingReport() throws DatabaseOperationException {
        when(itemRepository.findAll()).thenThrow(new DatabaseOperationException("DB error"));

        assertThrows(DatabaseOperationException.class, () -> reportingService.generateDailyReshelvingNeedsReport());
        verify(itemRepository, times(1)).findAll();
        verifyNoInteractions(shelfStockRepository);
    }

    @Test
    @DisplayName("Should throw DatabaseOperationException when shelfStockRepository fails for reshelving report")
    void shouldThrowDatabaseOperationExceptionWhenShelfStockRepoFailsForReshelvingReport() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Description A", "Category A", BigDecimal.valueOf(10.0), 5, 20);
        when(itemRepository.findAll()).thenReturn(List.of(item1));
        when(shelfStockRepository.findByItem(item1)).thenThrow(new DatabaseOperationException("DB error"));

        assertThrows(DatabaseOperationException.class, () -> reportingService.generateDailyReshelvingNeedsReport());
        verify(itemRepository, times(1)).findAll();
        verify(shelfStockRepository, times(1)).findByItem(item1);
    }

    @Test
    @DisplayName("Should not include items in reshelving report if minShelfStockThreshold (reorderQuantity) is zero or less")
    void shouldNotIncludeItemsInReshelvingReportIfMinThresholdIsZeroOrLess() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Description A", "Category A", BigDecimal.TEN, 5, 0);
        Item item2 = new Item("ITEM002", "Product B", "Description B", "Category B", BigDecimal.TEN, 5, 1);
        Item item3 = new Item("ITEM003", "Product C", "Description C", "Category C", BigDecimal.TEN, 5, 10);

        List<Item> allItems = List.of(item1, item2, item3);

        ShelfStock shelfStock1 = new ShelfStock(item1, 5);
        ShelfStock shelfStock2 = new ShelfStock(item2, 0);
        ShelfStock shelfStock3 = new ShelfStock(item3, 5);

        when(itemRepository.findAll()).thenReturn(allItems);
        when(shelfStockRepository.findByItem(item1)).thenReturn(Optional.of(shelfStock1));
        when(shelfStockRepository.findByItem(item2)).thenReturn(Optional.of(shelfStock2));
        when(shelfStockRepository.findByItem(item3)).thenReturn(Optional.of(shelfStock3));

        List<ReshelvingNeedsItemDTO> report = reportingService.generateDailyReshelvingNeedsReport();

        assertNotNull(report);
        assertEquals(2, report.size());
        assertEquals("ITEM002", report.get(0).getItemCode());
        assertEquals(0, report.get(0).getCurrentShelfQuantity());
        assertEquals(1, report.get(0).getMinShelfStockThreshold());
        assertEquals(1, report.get(0).getQuantityToReshelve());

        assertEquals("ITEM003", report.get(1).getItemCode());
        assertEquals(5, report.get(1).getCurrentShelfQuantity());
        assertEquals(10, report.get(1).getMinShelfStockThreshold());
        assertEquals(5, report.get(1).getQuantityToReshelve());

        verify(itemRepository, times(1)).findAll();
        verify(shelfStockRepository, times(3)).findByItem(any(Item.class));
    }


    @Test
    @DisplayName("Should generate bill transaction report successfully within date range")
    void shouldGenerateBillTransactionReportSuccessfully() throws DatabaseOperationException {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        List<BillSummaryDTO> billSummaries = new ArrayList<>();
        billSummaries.add(new BillSummaryDTO("BILL001", LocalDate.of(2023, 1, 10).atStartOfDay(), TransactionType.ONLINE_SALE, 101, BigDecimal.valueOf(100.0)));
        billSummaries.add(new BillSummaryDTO("BILL002", LocalDate.of(2023, 1, 20).atStartOfDay(), TransactionType.POS_CARD, 102, BigDecimal.valueOf(200.0)));

        when(billRepository.findAllBillSummaries(startDate, endDate, null)).thenReturn(billSummaries);

        List<BillSummaryDTO> report = reportingService.generateBillTransactionReport(startDate, endDate, Optional.empty());

        assertNotNull(report);
        assertEquals(2, report.size());
        assertEquals("BILL001", report.get(0).getBillSerialNumber());
        assertEquals("BILL002", report.get(1).getBillSerialNumber());
        verify(billRepository, times(1)).findAllBillSummaries(startDate, endDate, null);
    }

    @Test
    @DisplayName("Should generate bill transaction report successfully with transaction type filter")
    void shouldGenerateBillTransactionReportWithFilterSuccessfully() throws DatabaseOperationException {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        List<BillSummaryDTO> billSummaries = new ArrayList<>();
        billSummaries.add(new BillSummaryDTO("BILL001", LocalDate.of(2023, 1, 10).atStartOfDay(), TransactionType.ONLINE_SALE, 101, BigDecimal.valueOf(100.0)));

        when(billRepository.findAllBillSummaries(startDate, endDate, TransactionType.ONLINE_SALE)).thenReturn(billSummaries);

        List<BillSummaryDTO> report = reportingService.generateBillTransactionReport(startDate, endDate, Optional.of(TransactionType.ONLINE_SALE));

        assertNotNull(report);
        assertEquals(1, report.size());
        assertEquals("BILL001", report.get(0).getBillSerialNumber());
        verify(billRepository, times(1)).findAllBillSummaries(startDate, endDate, TransactionType.ONLINE_SALE);
    }

    @Test
    @DisplayName("Should return empty bill transaction report if no bills within date range")
    void shouldReturnEmptyBillTransactionReportIfNoBills() throws DatabaseOperationException {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        when(billRepository.findAllBillSummaries(startDate, endDate, null)).thenReturn(Collections.emptyList());

        List<BillSummaryDTO> report = reportingService.generateBillTransactionReport(startDate, endDate, Optional.empty());

        assertNotNull(report);
        assertTrue(report.isEmpty());
        verify(billRepository, times(1)).findAllBillSummaries(startDate, endDate, null);
    }

    @Test
    @DisplayName("Should throw DatabaseOperationException when billRepository fails for bill transaction report")
    void shouldThrowDatabaseOperationExceptionWhenBillRepoFailsForBillTransactionReport() throws DatabaseOperationException {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        when(billRepository.findAllBillSummaries(startDate, endDate, null)).thenThrow(new DatabaseOperationException("DB error"));

        // Changed expected exception type from IllegalArgumentException to DatabaseOperationException
        assertThrows(DatabaseOperationException.class, () -> reportingService.generateBillTransactionReport(startDate, endDate, Optional.empty()));
        verify(billRepository, times(1)).findAllBillSummaries(startDate, endDate, null);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if start date is after end date for bill transaction report")
    void shouldThrowIllegalArgumentExceptionWhenStartDateAfterEndDateForBillTransactionReport() {
        LocalDate startDate = LocalDate.of(2023, 1, 31);
        LocalDate endDate = LocalDate.of(2023, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> reportingService.generateBillTransactionReport(startDate, endDate, Optional.empty()));
        verifyNoInteractions(billRepository);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if start date is null for bill transaction report")
    void shouldThrowIllegalArgumentExceptionWhenStartDateIsNullForBillTransactionReport() {
        LocalDate endDate = LocalDate.of(2023, 1, 31);

        assertThrows(IllegalArgumentException.class, () -> reportingService.generateBillTransactionReport(null, endDate, Optional.empty()));
        verifyNoInteractions(billRepository);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if end date is null for bill transaction report")
    void shouldThrowIllegalArgumentExceptionWhenEndDateIsNullForBillTransactionReport() {
        LocalDate startDate = LocalDate.of(2023, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> reportingService.generateBillTransactionReport(startDate, null, Optional.empty()));
        verifyNoInteractions(billRepository);
    }


    // --- 10 NEW TEST CASES ---

    @Test
    @DisplayName("Daily Sales Report: Should return empty if all sales items have zero or negative quantity after fetch")
    void shouldReturnEmptyDailySalesReportIfAllSalesAreZeroOrNegativeQuantity() throws DatabaseOperationException {
        LocalDate testDate = LocalDate.of(2023, 2, 1);
        List<DailySalesReportItemDTO> salesItems = new ArrayList<>();
        salesItems.add(new DailySalesReportItemDTO("ITEM007", "Item G", 0, BigDecimal.valueOf(0.0)));
        salesItems.add(new DailySalesReportItemDTO("ITEM008", "Item H", -1, BigDecimal.valueOf(-5.0)));

        when(billRepository.findSalesDataByDateAndType(testDate, null)).thenReturn(salesItems);

        DailySalesReportDTO report = reportingService.generateDailySalesReport(testDate, Optional.empty());

        assertNotNull(report);
        assertTrue(report.getSaleItems().isEmpty());
        assertEquals(BigDecimal.ZERO, report.getOverallTotalRevenue());
        verify(billRepository, times(1)).findSalesDataByDateAndType(testDate, null);
    }

    @Test
    @DisplayName("Daily Sales Report: Should return empty report when specific transaction type yields no sales")
    void shouldReturnEmptyDailySalesReportForSpecificTransactionTypeWithNoSales() throws DatabaseOperationException {
        LocalDate testDate = LocalDate.of(2023, 2, 2);
        when(billRepository.findSalesDataByDateAndType(testDate, TransactionType.POS_CASH)).thenReturn(Collections.emptyList());

        DailySalesReportDTO report = reportingService.generateDailySalesReport(testDate, Optional.of(TransactionType.POS_CASH));

        assertNotNull(report);
        assertTrue(report.getSaleItems().isEmpty());
        assertEquals(BigDecimal.ZERO, report.getOverallTotalRevenue());
        verify(billRepository, times(1)).findSalesDataByDateAndType(testDate, TransactionType.POS_CASH);
    }

    @Test
    @DisplayName("Reorder Level Report: Should include item with only shelf stock below reorder level")
    void shouldIncludeItemWithOnlyShelfStockBelowReorderLevel() throws DatabaseOperationException {
        Item item1 = new Item("ITEM004", "Product D", "Desc D", "Cat D", BigDecimal.valueOf(20.0), 10, 50);
        List<Item> allItems = List.of(item1);

        ShelfStock shelfStock1 = new ShelfStock(item1, 8);

        when(itemRepository.findAll()).thenReturn(allItems);
        when(stockBatchRepository.findByItem(item1)).thenReturn(Collections.emptyList());
        when(shelfStockRepository.findByItem(item1)).thenReturn(Optional.of(shelfStock1));

        List<ReorderReportItemDTO> report = reportingService.generateReorderLevelReport();

        assertNotNull(report);
        assertEquals(1, report.size());
        assertEquals("ITEM004", report.get(0).getItemCode());
        assertEquals(8, report.get(0).getCurrentTotalStock());
        assertEquals(10, report.get(0).getReorderLevelThreshold());

        verify(itemRepository, times(1)).findAll();
        verify(stockBatchRepository, times(1)).findByItem(item1);
        verify(shelfStockRepository, times(1)).findByItem(item1);
    }

    @Test
    @DisplayName("Reorder Level Report: Should include item with only batch stock below reorder level")
    void shouldIncludeItemWithOnlyBatchStockBelowReorderLevel() throws DatabaseOperationException {
        Item item1 = new Item("ITEM005", "Product E", "Desc E", "Cat E", BigDecimal.valueOf(25.0), 15, 60);
        List<Item> allItems = List.of(item1);

        StockBatch batch1 = new StockBatch(4L, item1, 20, 12, LocalDate.now().plusMonths(3), BigDecimal.valueOf(15.0), LocalDate.now());

        when(itemRepository.findAll()).thenReturn(allItems);
        when(stockBatchRepository.findByItem(item1)).thenReturn(List.of(batch1));
        when(shelfStockRepository.findByItem(item1)).thenReturn(Optional.empty());

        List<ReorderReportItemDTO> report = reportingService.generateReorderLevelReport();

        assertNotNull(report);
        assertEquals(1, report.size());
        assertEquals("ITEM005", report.get(0).getItemCode());
        assertEquals(12, report.get(0).getCurrentTotalStock());
        assertEquals(15, report.get(0).getReorderLevelThreshold());

        verify(itemRepository, times(1)).findAll();
        verify(stockBatchRepository, times(1)).findByItem(item1);
        verify(shelfStockRepository, times(1)).findByItem(item1);
    }

    @Test
    @DisplayName("Batch Stock Report: Should handle batches with null expiry dates correctly")
    void shouldHandleBatchesWithNullExpiryDates() throws DatabaseOperationException {
        Item item1 = new Item("ITEM001", "Product A", "Desc A", "Cat A", BigDecimal.TEN, 5, 20);
        Item item2 = new Item("ITEM002", "Product B", "Desc B", "Cat B", BigDecimal.TEN, 5, 20);

        StockBatch batch1 = new StockBatch(1L, item1, 10, 8, null, BigDecimal.valueOf(5.0), LocalDate.of(2023, 1, 1));
        StockBatch batch2 = new StockBatch(2L, item2, 20, 15, LocalDate.of(2025, 12, 31), BigDecimal.valueOf(10.0), LocalDate.of(2023, 1, 10));

        List<StockBatch> allBatches = List.of(batch1, batch2);
        when(stockBatchRepository.findAll()).thenReturn(allBatches);

        List<BatchStockReportItemDTO> report = reportingService.generateBatchStockReport();

        assertNotNull(report);
        assertEquals(2, report.size());

        assertEquals(1L, report.get(0).getBatchId());
        assertEquals(LocalDate.of(2023, 1, 1), report.get(0).getReceivedDate());
        assertNull(report.get(0).getExpiryDate());

        assertEquals(2L, report.get(1).getBatchId());
        assertEquals(LocalDate.of(2023, 1, 10), report.get(1).getReceivedDate());
        assertEquals(LocalDate.of(2025, 12, 31), report.get(1).getExpiryDate());

        verify(stockBatchRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Batch Stock Report: Should sort correctly with a large number of batches")
    void shouldSortBatchStockReportWithLargeNumberOfBatches() throws DatabaseOperationException {
        List<StockBatch> largeBatchList = new ArrayList<>();
        Random random = new Random();
        Item itemA = new Item("ITEMA", "Product A", "Desc A", "Cat A", BigDecimal.TEN, 5, 20);
        Item itemB = new Item("ITEMB", "Product B", "Desc B", "Cat B", BigDecimal.TEN, 5, 20);

        for (int i = 0; i < 50; i++) {
            LocalDate receivedDate = LocalDate.of(2024, 1, 1).plusDays(random.nextInt(365));
            LocalDate expiryDate = receivedDate.plusMonths(random.nextInt(12) + 1);
            largeBatchList.add(new StockBatch((long) i, itemA, 100, 50, expiryDate, BigDecimal.ONE, receivedDate));
            largeBatchList.add(new StockBatch((long) (i + 100), itemB, 100, 50, expiryDate, BigDecimal.ONE, receivedDate));
        }

        when(stockBatchRepository.findAll()).thenReturn(largeBatchList);

        List<BatchStockReportItemDTO> report = reportingService.generateBatchStockReport();

        assertNotNull(report);
        assertEquals(largeBatchList.size(), report.size());

        for (int i = 0; i < report.size() - 1; i++) {
            BatchStockReportItemDTO current = report.get(i);
            BatchStockReportItemDTO next = report.get(i + 1);

            int itemCodeComparison = current.getItemCode().compareTo(next.getItemCode());
            if (itemCodeComparison > 0) {
                fail("Batch stock report is not sorted by ItemCode.");
            } else if (itemCodeComparison == 0) {
                int receivedDateComparison = current.getReceivedDate().compareTo(next.getReceivedDate());
                if (receivedDateComparison > 0) {
                    fail("Batch stock report for same ItemCode is not sorted by ReceivedDate.");
                } else if (receivedDateComparison == 0) {
                    if (current.getBatchId() > next.getBatchId()) {
                        fail("Batch stock report for same ItemCode and ReceivedDate is not sorted by BatchId.");
                    }
                }
            }
        }
        verify(stockBatchRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Reshelving Needs Report: Should include item with zero shelf stock but positive threshold")
    void shouldIncludeItemWithZeroShelfStockAndPositiveThreshold() throws DatabaseOperationException {
        Item item1 = new Item("ITEM006", "Product F", "Desc F", "Cat F", BigDecimal.valueOf(50.0), 5, 10);
        List<Item> allItems = List.of(item1);

        ShelfStock shelfStock1 = new ShelfStock(item1, 0);

        when(itemRepository.findAll()).thenReturn(allItems);
        when(shelfStockRepository.findByItem(item1)).thenReturn(Optional.of(shelfStock1));

        List<ReshelvingNeedsItemDTO> report = reportingService.generateDailyReshelvingNeedsReport();

        assertNotNull(report);
        assertEquals(1, report.size());
        assertEquals("ITEM006", report.get(0).getItemCode());
        assertEquals(0, report.get(0).getCurrentShelfQuantity());
        assertEquals(10, report.get(0).getMinShelfStockThreshold());
        assertEquals(10, report.get(0).getQuantityToReshelve());

        verify(itemRepository, times(1)).findAll();
        verify(shelfStockRepository, times(1)).findByItem(item1);
    }



    @Test
    @DisplayName("Bill Transaction Report: Should return bills when start and end dates are the same (single day)")
    void shouldGenerateBillTransactionReportForSingleDay() throws DatabaseOperationException {
        LocalDate testDate = LocalDate.of(2023, 3, 10);
        List<BillSummaryDTO> billSummaries = new ArrayList<>();
        billSummaries.add(new BillSummaryDTO("BILL003", testDate.atTime(10, 0), TransactionType.POS_CASH, 103, BigDecimal.valueOf(75.0)));

        when(billRepository.findAllBillSummaries(testDate, testDate, null)).thenReturn(billSummaries);

        List<BillSummaryDTO> report = reportingService.generateBillTransactionReport(testDate, testDate, Optional.empty());

        assertNotNull(report);
        assertEquals(1, report.size());
        assertEquals("BILL003", report.get(0).getBillSerialNumber());
        verify(billRepository, times(1)).findAllBillSummaries(testDate, testDate, null);
    }

    @Test
    @DisplayName("Bill Transaction Report: Should return empty list if no bills for specific transaction type in range (e.g., POS_CASH)")
    void shouldReturnEmptyBillTransactionReportForSpecificTypeNoSales() throws DatabaseOperationException {
        LocalDate startDate = LocalDate.of(2023, 4, 1);
        LocalDate endDate = LocalDate.of(2023, 4, 30);


        when(billRepository.findAllBillSummaries(startDate, endDate, TransactionType.POS_CASH)).thenReturn(Collections.emptyList());

        List<BillSummaryDTO> report = reportingService.generateBillTransactionReport(startDate, endDate, Optional.of(TransactionType.POS_CASH));

        assertNotNull(report);
        assertTrue(report.isEmpty());

        verify(billRepository, times(1)).findAllBillSummaries(startDate, endDate, TransactionType.POS_CASH);
    }
    @Test
    @DisplayName("Daily Sales Report: Should correctly handle very large revenue values")
    void shouldHandleDailySalesReportWithLargeRevenue() throws DatabaseOperationException {
        LocalDate testDate = LocalDate.of(2023, 5, 1);
        List<DailySalesReportItemDTO> salesItems = List.of(
                new DailySalesReportItemDTO("ITEM100", "Expensive Item", 2, new BigDecimal("999999999.99")),
                new DailySalesReportItemDTO("ITEM101", "Another Expensive Item", 1, new BigDecimal("888888888.88"))
        );

        when(billRepository.findSalesDataByDateAndType(testDate, null)).thenReturn(salesItems);

        DailySalesReportDTO report = reportingService.generateDailySalesReport(testDate, Optional.empty());

        assertNotNull(report);
        assertEquals(2, report.getSaleItems().size());
        assertEquals(new BigDecimal("1888888888.87"), report.getOverallTotalRevenue());
        verify(billRepository, times(1)).findSalesDataByDateAndType(testDate, null);
    }
    @Test
    @DisplayName("Reorder Level Report: Should not include item exactly at reorder level")
    void shouldNotIncludeItemExactlyAtReorderLevel() throws DatabaseOperationException {
        Item item = new Item("ITEM200", "Product F", "Desc F", "Cat F", BigDecimal.TEN, 5, 10);

        StockBatch batch = new StockBatch(1L, item, 10, 10, LocalDate.now().plusMonths(6), BigDecimal.ZERO, LocalDate.now());
        ShelfStock shelfStock = new ShelfStock(item, 0);

        when(itemRepository.findAll()).thenReturn(List.of(item));
        when(stockBatchRepository.findByItem(item)).thenReturn(List.of(batch));
        when(shelfStockRepository.findByItem(item)).thenReturn(Optional.of(shelfStock));

        List<ReorderReportItemDTO> report = reportingService.generateReorderLevelReport();

        assertNotNull(report);
        assertTrue(report.isEmpty());
        verify(itemRepository, times(1)).findAll();
        verify(stockBatchRepository, times(1)).findByItem(item);
        verify(shelfStockRepository, times(1)).findByItem(item);
    }

    @Test
    @DisplayName("Batch Stock Report: Should correctly sort batches with same receivedDate by batchId")
    void shouldSortBatchStockReportWithSameReceivedDate() throws DatabaseOperationException {
        Item item = new Item("ITEM300", "Product G", "Desc G", "Cat G", BigDecimal.TEN, 5, 20);

        StockBatch batch1 = new StockBatch(2L, item, 10, 10, LocalDate.of(2024, 1, 1), BigDecimal.ZERO, LocalDate.of(2023, 1, 1));
        StockBatch batch2 = new StockBatch(1L, item, 10, 10, LocalDate.of(2024, 1, 1), BigDecimal.ZERO, LocalDate.of(2023, 1, 1));

        when(stockBatchRepository.findAll()).thenReturn(List.of(batch1, batch2));

        List<BatchStockReportItemDTO> report = reportingService.generateBatchStockReport();

        assertEquals(2, report.size());
        assertEquals(1L, report.get(0).getBatchId());
        assertEquals(2L, report.get(1).getBatchId());
    }
    @Test
    @DisplayName("Reshelving Needs Report: Should handle negative shelf stock values")
    void shouldHandleNegativeShelfStockInReshelvingReport() throws DatabaseOperationException {
        // Arrange
        Item item = new Item("ITEM400", "Product H", "Desc H", "Cat H", BigDecimal.TEN, 5, 10);

        // Mock ShelfStock instead of creating a real one with negative quantity
        ShelfStock mockShelfStock = mock(ShelfStock.class);
        when(mockShelfStock.getItem()).thenReturn(item);
        when(mockShelfStock.getQuantity()).thenReturn(-5); // simulate negative quantity

        when(itemRepository.findAll()).thenReturn(List.of(item));
        when(shelfStockRepository.findByItem(item)).thenReturn(Optional.of(mockShelfStock));

        // Act
        List<ReshelvingNeedsItemDTO> report = reportingService.generateDailyReshelvingNeedsReport();

        // Assert
        assertNotNull(report);
        assertEquals(1, report.size());
        assertEquals(0, report.get(0).getCurrentShelfQuantity()); // negative treated as 0
        assertEquals(10, report.get(0).getMinShelfStockThreshold());
        assertEquals(10, report.get(0).getQuantityToReshelve());
    }

    @Test
    @DisplayName("Bill Transaction Report: Should handle very large date ranges")
    void shouldHandleBillTransactionReportWithLargeDateRange() throws DatabaseOperationException {
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);

        List<BillSummaryDTO> billSummaries = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            billSummaries.add(new BillSummaryDTO("BILL" + i, LocalDate.of(2023, 1, 1).atStartOfDay(), TransactionType.POS_CASH, 100 + i, BigDecimal.valueOf(100.0 + i)));
        }

        when(billRepository.findAllBillSummaries(startDate, endDate, null)).thenReturn(billSummaries);

        List<BillSummaryDTO> report = reportingService.generateBillTransactionReport(startDate, endDate, Optional.empty());

        assertNotNull(report);
        assertEquals(100, report.size());
        verify(billRepository, times(1)).findAllBillSummaries(startDate, endDate, null);
    }





}