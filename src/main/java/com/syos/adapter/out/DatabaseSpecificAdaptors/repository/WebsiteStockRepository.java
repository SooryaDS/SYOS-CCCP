package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.WebsiteStock;
import com.syos.domain.exception.DatabaseOperationException;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface WebsiteStockRepository {
    WebsiteStock saveOrUpdate(WebsiteStock websiteStock) throws DatabaseOperationException;
    Optional<WebsiteStock> findByItemCodeAndBatchId(String itemCode, int batchId) throws DatabaseOperationException;
    List<WebsiteStock> findByItemCode(String itemCode) throws DatabaseOperationException;
    WebsiteStock update(WebsiteStock websiteStock) throws DatabaseOperationException;
    void delete(WebsiteStock websiteStock) throws DatabaseOperationException;

    // --- Methods for Service-Layer Transactions and Helpers ---
    WebsiteStock update(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException;
    void delete(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException;
    int getTotalStockByItemCode(String itemCode) throws DatabaseOperationException;
}