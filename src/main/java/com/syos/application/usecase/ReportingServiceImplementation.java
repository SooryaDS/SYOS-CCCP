package com.syos.application.usecase;

import com.syos.application.port.ReportingService;
import com.syos.domain.model.Item;
import com.syos.domain.model.ShelfStock;
import com.syos.domain.model.StockBatch;
import com.syos.domain.enums.TransactionType;
import com.syos.adapter.dto.BatchStockReportItemDTO;
import com.syos.adapter.dto.BillSummaryDTO;
import com.syos.adapter.dto.DailySalesReportDTO;
import com.syos.adapter.dto.DailySalesReportItemDTO;
import com.syos.adapter.dto.ReorderReportItemDTO;
import com.syos.adapter.dto.ReshelvingNeedsItemDTO;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.BillRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ShelfStockRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.StockBatchRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReportingServiceImplementation implements ReportingService {

    private final BillRepository billRepository;
    private final ItemRepository itemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final ShelfStockRepository shelfStockRepository;

    public ReportingServiceImplementation(BillRepository billRepository,
                                          ItemRepository itemRepository,
                                          StockBatchRepository stockBatchRepository,
                                          ShelfStockRepository shelfStockRepository) {
        this.billRepository = billRepository;
        this.itemRepository = itemRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.shelfStockRepository = shelfStockRepository;
    }

    @Override
    public DailySalesReportDTO generateDailySalesReport(LocalDate date, Optional<TransactionType> transactionTypeFilter)
            throws DatabaseOperationException, IllegalArgumentException {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null for daily sales report.");
        }

        TransactionType type = transactionTypeFilter.orElse(null);

        List<DailySalesReportItemDTO> salesItems = billRepository.findSalesDataByDateAndType(date, type);

        List<DailySalesReportItemDTO> filteredSalesItems = salesItems.stream()
                .filter(item -> item.getTotalQuantitySold() > 0)
                .collect(Collectors.toList());

        BigDecimal overallTotalRevenue = filteredSalesItems.stream()
                .map(DailySalesReportItemDTO::getTotalRevenueForItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DailySalesReportDTO(filteredSalesItems, overallTotalRevenue);
    }

    @Override
    public List<ReorderReportItemDTO> generateReorderLevelReport() throws DatabaseOperationException {
        List<ReorderReportItemDTO> reorderItems = new ArrayList<>();
        List<Item> allItems = itemRepository.findAll();

        for (Item item : allItems) {
            int totalStockInStore = stockBatchRepository.findByItem(item).stream()
                    .mapToInt(StockBatch::getCurrentQuantityInStore)
                    .sum();

            int totalStockOnShelf = shelfStockRepository.findByItem(item)
                    .map(ShelfStock::getQuantityOnShelf)
                    .orElse(0);

            int currentTotalStock = totalStockInStore + totalStockOnShelf;

            if (currentTotalStock < item.getReorderLevel()) {
                reorderItems.add(new ReorderReportItemDTO(
                        item.getItemCode(),
                        item.getName(),
                        currentTotalStock,
                        item.getReorderLevel()
                ));
            }
        }
        return reorderItems;
    }

    @Override
    public List<BatchStockReportItemDTO> generateBatchStockReport() throws DatabaseOperationException {
        List<StockBatch> allBatches = stockBatchRepository.findAll();
        List<BatchStockReportItemDTO> reportItems = new ArrayList<>();

        for (StockBatch batch : allBatches) {
            reportItems.add(new BatchStockReportItemDTO(
                    batch.getItem().getItemCode(),
                    batch.getItem().getName(),
                    batch.getBatchId(),
                    batch.getReceivedDate(),
                    batch.getReceivedQuantity(),
                    batch.getCurrentQuantityInStore(),
                    batch.getExpiryDate()
            ));
        }

        reportItems.sort(Comparator.comparing(BatchStockReportItemDTO::getItemCode)
                .thenComparing(BatchStockReportItemDTO::getReceivedDate)
                .thenComparing(BatchStockReportItemDTO::getBatchId));

        return reportItems;
    }

    @Override
    public List<ReshelvingNeedsItemDTO> generateDailyReshelvingNeedsReport() throws DatabaseOperationException {
        List<ReshelvingNeedsItemDTO> needsReshelving = new ArrayList<>();
        List<Item> allItems = itemRepository.findAll();

        for (Item item : allItems) {
            int currentShelfQuantity = shelfStockRepository.findByItem(item)
                    .map(ShelfStock::getQuantityOnShelf)
                    .orElse(0);

            if (item.getReorderQuantity() > 0 && currentShelfQuantity < item.getReorderQuantity()) {
                needsReshelving.add(new ReshelvingNeedsItemDTO(
                        item.getItemCode(),
                        item.getName(),
                        currentShelfQuantity,
                        item.getReorderQuantity()
                ));
            }
        }

        needsReshelving.sort(Comparator.comparing(ReshelvingNeedsItemDTO::getItemName));
        return needsReshelving;
    }

    @Override
    public List<BillSummaryDTO> generateBillTransactionReport(LocalDate startDate, LocalDate endDate, Optional<TransactionType> transactionTypeFilter)
            throws DatabaseOperationException {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null.");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        TransactionType type = transactionTypeFilter.orElse(null);
        return billRepository.findAllBillSummaries(startDate, endDate, type);
    }
}
