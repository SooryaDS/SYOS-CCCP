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

        // Correctly unwrap the Optional TransactionType for the repository call
        TransactionType type = transactionTypeFilter.orElse(null);

        List<DailySalesReportItemDTO> salesItems = billRepository.findSalesDataByDateAndType(date, type);
        BigDecimal overallTotalRevenue = BigDecimal.ZERO;
        for (DailySalesReportItemDTO itemDTO : salesItems) {
            overallTotalRevenue = overallTotalRevenue.add(itemDTO.getTotalRevenueForItem());
        }
        return new DailySalesReportDTO(salesItems, overallTotalRevenue);
    }

    @Override
    public List<ReorderReportItemDTO> generateReorderLevelReport() throws DatabaseOperationException {
        List<ReorderReportItemDTO> reorderItems = new ArrayList<>();
        List<Item> allItems = itemRepository.findAll();

        for (Item item : allItems) {
            int totalStockInStore = 0;
            List<StockBatch> batchesInStore = stockBatchRepository.findByItemCode(item.getItemCode());
            for (StockBatch batch : batchesInStore) {
                totalStockInStore += batch.getCurrentQuantityInStore();
            }

            // Note: website stock is not included here, which is fine if this report is only for physical store stock
            int totalStockOnShelf = 0;
            List<ShelfStock> shelfStocks = shelfStockRepository.findByItemCode(item.getItemCode());
            for (ShelfStock shelfEntry : shelfStocks) {
                totalStockOnShelf += shelfEntry.getQuantityOnShelf();
            }

            int currentTotalStock = totalStockInStore + totalStockOnShelf;

            // This comparison is for overall reorder level (store + shelf)
            if (currentTotalStock < item.getReorderLevelThreshold()) {
                reorderItems.add(new ReorderReportItemDTO(
                        item.getItemCode(),
                        item.getName(),
                        currentTotalStock,
                        item.getReorderLevelThreshold()
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
            String itemName = itemRepository.findByItemCode(batch.getItemCode())
                    .map(Item::getName)
                    .orElse("Unknown Item");

            reportItems.add(new BatchStockReportItemDTO(
                    batch.getItemCode(),
                    itemName,
                    batch.getBatchId(),
                    batch.getPurchaseDate(),
                    batch.getQuantityReceived(),
                    batch.getCurrentQuantityInStore(),
                    batch.getExpiryDate()
            ));
        }
        reportItems.sort(Comparator.comparing(BatchStockReportItemDTO::getItemCode)
                .thenComparing(BatchStockReportItemDTO::getPurchaseDate)
                .thenComparing(BatchStockReportItemDTO::getBatchId)
        );
        return reportItems;
    }

    @Override
    public List<ReshelvingNeedsItemDTO> generateDailyReshelvingNeedsReport() throws DatabaseOperationException {
        List<ReshelvingNeedsItemDTO> needsReshelving = new ArrayList<>();
        List<Item> allItems = itemRepository.findAll();

        for (Item item : allItems) {
            int currentShelfQuantity = 0;
            List<ShelfStock> shelfStocksForItem = shelfStockRepository.findByItemCode(item.getItemCode());
            for (ShelfStock ss : shelfStocksForItem) {
                currentShelfQuantity += ss.getQuantityOnShelf();
            }
            // Use the correct method: getMinShelfStockThreshold()
            if (currentShelfQuantity < item.getMinShelfStockThreshold()) {
                needsReshelving.add(new ReshelvingNeedsItemDTO(
                        item.getItemCode(),
                        item.getName(),
                        currentShelfQuantity,
                        item.getMinShelfStockThreshold() // Use the correct method here too
                ));
            }
        }
        needsReshelving.sort(Comparator.comparing(ReshelvingNeedsItemDTO::getItemName));
        return needsReshelving;
    }

    @Override
    public List<BillSummaryDTO> generateBillTransactionReport(LocalDate startDate, LocalDate endDate, Optional<TransactionType> transactionTypeFilter)
            throws DatabaseOperationException {
        // Correctly unwrap the Optional TransactionType for the repository call
        TransactionType type = transactionTypeFilter.orElse(null);

        // Date validation can be added here if strict rules are needed (e.g., startDate before endDate)
        return billRepository.findAllBillSummaries(startDate, endDate, type);
    }
}