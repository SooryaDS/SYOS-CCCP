package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.OnlineOrder;
import com.syos.domain.enums.OrderStatus;
import com.syos.domain.exception.DatabaseOperationException;

import java.util.List;
import java.util.Optional;

public interface OnlineOrderRepository {
    OnlineOrder save(OnlineOrder order) throws DatabaseOperationException;
    Optional<OnlineOrder> findById(String orderId) throws DatabaseOperationException;
    List<OnlineOrder> findByUserIdAndStatus(int onlineUserId, OrderStatus status) throws DatabaseOperationException;
    OnlineOrder update(OnlineOrder order) throws DatabaseOperationException; // For status, address, etc.
    // delete might not be common, perhaps just change status to CANCELLED
}