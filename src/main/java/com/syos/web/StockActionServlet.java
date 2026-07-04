package com.syos.web;

import com.syos.application.port.StockManagementService;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.model.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@WebServlet("/stockAction")
public class StockActionServlet extends HttpServlet {

    private StockManagementService stockService;

    @Override
    public void init() {
        stockService = (StockManagementService) getServletContext().getAttribute("stockService");
        if (stockService == null) {
            throw new IllegalStateException("StockManagementService not found in ServletContext");
        }
    }

    // -------------------- GET --------------------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = Optional.ofNullable(req.getParameter("action")).orElse("viewItems");

        try {
            switch (action) {
                case "viewItems" -> {
                    req.setAttribute("items", stockService.getAllItems());
                    req.getRequestDispatcher("/viewItems.jsp").forward(req, resp);
                }
                case "viewBatches" -> {
                    req.setAttribute("stockBatches", stockService.getAllStockBatches());
                    req.getRequestDispatcher("/stock-batch.jsp").forward(req, resp);
                }
                case "viewShelf" -> {
                    req.setAttribute("items", stockService.getAllItems());
                    req.setAttribute("shelfStock", stockService.getAllShelfStock());
                    req.getRequestDispatcher("/shelf-stock.jsp").forward(req, resp);
                }
                case "viewWebsite" -> {
                    req.setAttribute("items", stockService.getAllItems());
                    req.setAttribute("websiteStock", stockService.getAllWebsiteStock());
                    req.getRequestDispatcher("/website-stock.jsp").forward(req, resp);
                }
                case "removeExpired" -> {
                    req.setAttribute("expiredRemoved", stockService.processAndRemoveExpiredStock());
                    req.getRequestDispatcher("/remove-expired.jsp").forward(req, resp);
                }
                default -> resp.sendRedirect("stockAction?action=viewItems");
            }

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Error during GET action: " + e.getMessage());
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
        }
    }

    // -------------------- POST --------------------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = Optional.ofNullable(req.getParameter("action")).orElse("");

        try {
            switch (action) {

                // ---------- ITEM ----------
                case "addItem" -> {
                    String itemCode = requireParam(req, "itemCode");
                    String name = requireParam(req, "name");
                    String description = Optional.ofNullable(req.getParameter("description")).orElse("");
                    String category = requireParam(req, "category");

                    BigDecimal unitPrice = parseBigDecimal(req, "unitPrice");
                    int reorderLevel = parseInt(req, "reorderLevel");
                    int reorderQuantity = parseInt(req, "reorderQuantity");

                    Item item = new Item(itemCode, name, description, category, unitPrice, reorderLevel, reorderQuantity);
                    stockService.addItem(item);

                    req.getSession().setAttribute("success", "Item added successfully.");
                    resp.sendRedirect("stockAction?action=viewItems");
                }

                case "deleteItem" -> {
                    String itemCode = requireParam(req, "itemCode");
                    try {
                        // Attempt to delete the item
                        stockService.deleteItem(itemCode);

                        // Add success message to session for display in JSP
                        req.getSession().setAttribute("successMessage", "Item " + itemCode + " deleted successfully.");

                    } catch (DatabaseOperationException | ItemNotFoundException e) {
                        // If deletion fails due to DB issues or item not found, store error message
                        req.getSession().setAttribute("errorMessage", "Failed to delete item " + itemCode + ": " + e.getMessage());
                    }

                    // Redirect to refresh the list
                    resp.sendRedirect("stockAction?action=viewItems");
                }



                // ---------- BATCH ----------
                case "receiveBatch" -> {
                    String itemCode = requireParam(req, "itemCode");
                    int quantity = parseInt(req, "quantity");
                    String expiryStr = req.getParameter("expiryDate");
                    LocalDate expiryDate = (expiryStr != null && !expiryStr.isBlank()) ? LocalDate.parse(expiryStr) : null;
                    BigDecimal costPerUnit = parseBigDecimal(req, "costPerUnit");

                    stockService.receiveNewStockBatch(itemCode, quantity, expiryDate, costPerUnit);

                    req.getSession().setAttribute("stockActionMessage", "Stock batch received successfully.");
                    resp.sendRedirect("stockAction?action=viewBatches");
                }
                case "deleteStockBatch" -> {
                    String batchIdStr = requireParam(req, "batchId");

                    try {
                        long batchId = Long.parseLong(batchIdStr);
                        stockService.deleteStockBatch(batchId); // delete only this batch

                        req.getSession().setAttribute(
                                "stockActionMessage",
                                "Deleted stock batch " + batchId + " successfully."
                        );

                    } catch (NumberFormatException e) {
                        req.getSession().setAttribute(
                                "stockActionError",
                                "Invalid batch ID: " + batchIdStr
                        );
                    } catch (DatabaseOperationException | ItemNotFoundException e) {
                        req.getSession().setAttribute(
                                "stockActionError",
                                "Failed to delete batch " + batchIdStr + ": " + e.getMessage()
                        );
                    } catch (Exception e) {
                        req.getSession().setAttribute(
                                "stockActionError",
                                "Unexpected error: " + e.getMessage()
                        );
                    }

                    resp.sendRedirect("stockAction?action=viewBatches");
                }



                // ---------- SHELF ----------
                case "addShelf" -> {
                    String itemCode = requireParam(req, "itemCode");
                    int quantity = parseInt(req, "quantity");

                    // Ensure shelf stock exists or create, then add quantity
                    stockService.addToShelf(itemCode, quantity);

                    req.getSession().setAttribute("stockActionMessage", "Added to shelf stock successfully.");
                    resp.sendRedirect("stockAction?action=viewShelf");
                }

                case "removeShelf" -> {
                    String itemCode = requireParam(req, "itemCode");
                    int quantity = parseInt(req, "quantity");

                    stockService.removeFromShelf(itemCode, quantity);
                    req.getSession().setAttribute("success", "Removed from shelf stock successfully.");
                    resp.sendRedirect("stockAction?action=viewShelf");
                }

                // ---------- WEBSITE ----------
                case "addWebsite" -> {
                    String itemCode = requireParam(req, "itemCode");
                    int quantity = parseInt(req, "quantity");

                    // Ensure website stock exists or create, then add quantity
                    stockService.addToWebsiteStock(itemCode, quantity);

                    req.getSession().setAttribute("stockActionMessage", "Added to website stock successfully.");
                    resp.sendRedirect("stockAction?action=viewWebsite");
                }

                case "removeWebsite" -> {
                    String itemCode = requireParam(req, "itemCode");
                    int quantity = parseInt(req, "quantity");

                    stockService.removeFromWebsiteStock(itemCode, quantity);
                    req.getSession().setAttribute("success", "Removed from website stock successfully.");
                    resp.sendRedirect("stockAction?action=viewWebsite");
                }

                default -> resp.sendRedirect("stockAction?action=viewItems");
            }

        } catch (ItemNotFoundException | InsufficientStockException | DatabaseOperationException e) {
            e.printStackTrace();
            req.setAttribute("error", "Operation failed: " + e.getMessage());
            req.getRequestDispatcher("/error.jsp").forward(req, resp);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            req.setAttribute("error", "Invalid input: " + e.getMessage());
            req.getRequestDispatcher("/error.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Unexpected error: " + e.getMessage());
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
        }
    }

    // -------------------- Helper Methods --------------------
    private String requireParam(HttpServletRequest req, String name) {
        return Optional.ofNullable(req.getParameter(name))
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(name + " is required"));
    }

    private int parseInt(HttpServletRequest req, String paramName) {
        String value = requireParam(req, paramName);
        try {
            int v = Integer.parseInt(value);
            if (v <= 0) throw new IllegalArgumentException(paramName + " must be positive");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(paramName + " must be a valid integer");
        }
    }

    private BigDecimal parseBigDecimal(HttpServletRequest req, String paramName) {
        String value = requireParam(req, paramName);
        try {
            BigDecimal v = new BigDecimal(value);
            if (v.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException(paramName + " cannot be negative");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(paramName + " must be a valid number");
        }
    }
}

