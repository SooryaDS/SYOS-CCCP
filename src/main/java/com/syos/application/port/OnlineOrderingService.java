package com.syos.application.port;

import com.syos.domain.model.OnlineOrder;
import com.syos.domain.model.Bill;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.exception.OrderProcessingException;

import java.util.Optional; // Import Optional

public interface OnlineOrderingService {

    /**
     * Gets the active PENDING order for a user, or creates a new one if none exists.
     * @param onlineUserId The ID of the online user.
     * @return The active or newly created OnlineOrder.
     * @throws DatabaseOperationException If a database error occurs.
     */
    OnlineOrder getActiveOrderForUser(int onlineUserId) throws DatabaseOperationException;

    /**
     * Adds an item to the user's active (pending) order.
     * Checks website stock before adding.
     * @param onlineUserId The ID of the online user.
     * @param itemCode The code of the item to add.
     * @param quantity The quantity to add.
     * @return The updated OnlineOrder.
     * @throws ItemNotFoundException If the item doesn't exist.
     * @throws InsufficientStockException If website stock is not enough.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws IllegalArgumentException If inputs are invalid.
     */
    OnlineOrder addItemToActiveOrder(int onlineUserId, String itemCode, int quantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException, IllegalArgumentException;

    /**
     * Updates the quantity of an item in the user's active (pending) order.
     * If new quantity is 0, removes the item. Checks website stock.
     * @param onlineUserId The ID of the online user.
     * @param itemCode The code of the item to update.
     * @param newQuantity The new quantity.
     * @return The updated OnlineOrder.
     * @throws ItemNotFoundException If the item isn't in the order or doesn't exist.
     * @throws InsufficientStockException If new quantity exceeds website stock.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws IllegalArgumentException If inputs are invalid.
     */
    OnlineOrder updateOrderItemQuantity(int onlineUserId, String itemCode, int newQuantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException, IllegalArgumentException;

    /**
     * Removes an item from the user's active (pending) order.
     * @param onlineUserId The ID of the online user.
     * @param itemCode The code of the item to remove.
     * @return The updated OnlineOrder.
     * @throws ItemNotFoundException If the item isn't in the order.
     * @throws DatabaseOperationException If a database error occurs.
     */
    OnlineOrder removeItemFromActiveOrder(int onlineUserId, String itemCode)
            throws ItemNotFoundException, DatabaseOperationException;

    /**
     * Processes the checkout for the user's active order.
     * Converts OnlineOrder to a Bill, updates OnlineOrder status, reduces website stock.
     * @param onlineUserId The ID of the online user.
     * @param shippingAddress The shipping address for this order.
     * @return The generated Bill.
     * @throws OrderProcessingException If the order is empty or cannot be processed.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws InsufficientStockException If stock becomes insufficient during checkout.
     * @throws ItemNotFoundException If an item in the order is no longer valid.
     */
    Bill checkoutOrder(int onlineUserId, String shippingAddress)
            throws OrderProcessingException, DatabaseOperationException, InsufficientStockException, ItemNotFoundException;

    /**
     * Finds an OnlineOrder by its ID.
     * @param orderId The ID of the order to find.
     * @return An Optional containing the OnlineOrder if found, empty otherwise.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Optional<OnlineOrder> findOnlineOrderById(String orderId) throws DatabaseOperationException;

    /**
     * Updates the shipping address for the user's active (pending) order.
     * @param onlineUserId The ID of the online user.
     * @param newAddress The new shipping address.
     * @return The updated OnlineOrder.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws IllegalArgumentException If the new address is invalid.
     * @throws IllegalStateException If the order is not in a PENDING state.
     */
    OnlineOrder updateShippingAddress(int onlineUserId, String newAddress)
            throws DatabaseOperationException, IllegalArgumentException, IllegalStateException;
}