<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Add Item</title>
    <link rel="stylesheet" href="css/add-item.css">
</head>
<body>
    <div class="page-container">
        <h2>Add Item</h2>
        <form action="stock" method="post">
            <input type="hidden" name="action" value="addItem">

            <div class="form-group">
                <label for="itemCode">Item Code</label>
                <input type="text" name="itemCode" id="itemCode" required>
            </div>

            <div class="form-group">
                <label for="name">Item Name</label>
                <input type="text" name="name" id="name" required>
            </div>

            <div class="form-group full-width">
                <label for="description">Description</label>
                <input type="text" name="description" id="description" required>
            </div>

            <div class="form-group">
                <label for="category">Category</label>
                <input type="text" name="category" id="category" required>
            </div>

            <div class="form-group">
                <label for="unitPrice">Unit Price</label>
                <input type="number" step="0.01" name="unitPrice" id="unitPrice" required>
            </div>

            <div class="form-group">
                <label for="reorderLevel">Reorder Level</label>
                <input type="number" name="reorderLevel" id="reorderLevel" required>
            </div>

            <div class="form-group">
                <label for="reorderQuantity">Reorder Quantity</label>
                <input type="number" name="reorderQuantity" id="reorderQuantity" required>
            </div>

            <div class="form-actions">
                <button type="submit" class="btn btn-primary">Add Item</button>
                <a href="stock" class="btn btn-secondary">Cancel</a>
            </div>
        </form>
    </div>
</body>
</html>
