package com.syos.adapter.dto;

import java.math.BigDecimal;
import java.util.List;

public class DailySalesReportDTO {
    private final List<DailySalesReportItemDTO> saleItems;
    private final BigDecimal overallTotalRevenue;

    public DailySalesReportDTO(List<DailySalesReportItemDTO> saleItems, BigDecimal overallTotalRevenue) {
        this.saleItems = saleItems;
        this.overallTotalRevenue = overallTotalRevenue;
    }

    public List<DailySalesReportItemDTO> getSaleItems() { return saleItems; }
    public BigDecimal getOverallTotalRevenue() { return overallTotalRevenue; }
}