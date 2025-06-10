package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.Item;
import com.syos.domain.exception.DatabaseOperationException;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {
    Optional<Item> findByItemCode(String itemCode) throws DatabaseOperationException;
    List<Item> findAll() throws DatabaseOperationException;
    Item save(Item item) throws DatabaseOperationException; // For both create and update
    // boolean deleteByItemCode(String itemCode) throws DatabaseOperationException; // Add if needed
}