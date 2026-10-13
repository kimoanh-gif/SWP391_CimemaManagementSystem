package com.cinema.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Food, Beverage or Combo product (Screen 61 / UC-32).
 */
public class FBItem {

    private Integer itemId;
    private String itemCode;
    private String itemName;
    private String category; // 'Combo', 'Food', 'Drink', 'Snack'
    private String description;
    private BigDecimal sellingPrice;
    private BigDecimal costPrice;
    private String image;
    private String status; // 'Active', 'Inactive'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Associated components if this item is a combo
    private List<ComboComponent> comboComponents = new ArrayList<>();

    public FBItem() {
    }

    public FBItem(Integer itemId, String itemCode, String itemName, String category,
                  String description, BigDecimal sellingPrice, BigDecimal costPrice,
                  String image, String status) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;
        this.image = image;
        this.status = status;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ComboComponent> getComboComponents() {
        return comboComponents;
    }

    public void setComboComponents(List<ComboComponent> comboComponents) {
        this.comboComponents = comboComponents != null ? comboComponents : new ArrayList<>();
    }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(this.status);
    }

    public boolean isCombo() {
        return "Combo".equalsIgnoreCase(this.category);
    }

    public BigDecimal getProfitMarginPercent() {
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) <= 0 || costPrice == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal profit = sellingPrice.subtract(costPrice);
        return profit.multiply(new BigDecimal("100")).divide(sellingPrice, 1, RoundingMode.HALF_UP);
    }

    public static class ComboComponent {
        private Integer componentItemId;
        private String componentName;
        private Integer quantity;

        public ComboComponent() {}

        public ComboComponent(Integer componentItemId, String componentName, Integer quantity) {
            this.componentItemId = componentItemId;
            this.componentName = componentName;
            this.quantity = quantity;
        }

        public Integer getComponentItemId() { return componentItemId; }
        public void setComponentItemId(Integer componentItemId) { this.componentItemId = componentItemId; }
        public String getComponentName() { return componentName; }
        public void setComponentName(String componentName) { this.componentName = componentName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
