package com.cinema.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Form DTO for creating and updating F&B items and combos (Screen 61 / UC-32).
 */
public class FBItemForm {

    private Integer itemId;

    @NotBlank(message = "Mã sản phẩm / SKU không được để trống")
    @Size(max = 50, message = "Mã sản phẩm tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$", message = "Mã sản phẩm chỉ gồm chữ, số, dấu gạch ngang hoặc gạch dưới (2-50 ký tự)")
    private String itemCode;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 150, message = "Tên sản phẩm tối đa 150 ký tự")
    private String itemName;

    @NotBlank(message = "Vui lòng chọn danh mục")
    @Pattern(regexp = "^(?i)(Combo|Food|Drink|Snack)$", message = "Danh mục sản phẩm chỉ nhận giá trị Combo, Food, Drink hoặc Snack")
    private String category = "Combo"; // 'Combo', 'Food', 'Drink', 'Snack'

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", message = "Giá bán không được là số âm")
    @DecimalMax(value = "10000000.0", message = "Giá bán vượt quá giới hạn cho phép")
    private BigDecimal sellingPrice;

    @NotNull(message = "Giá vốn không được để trống")
    @DecimalMin(value = "0.0", message = "Giá vốn không được là số âm")
    @DecimalMax(value = "10000000.0", message = "Giá vốn vượt quá giới hạn cho phép")
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Size(max = 500, message = "Đường dẫn hình ảnh tối đa 500 ký tự")
    private String image;

    @Pattern(regexp = "^(?i)(Active|Inactive)$", message = "Trạng thái sản phẩm chỉ nhận giá trị Active hoặc Inactive")
    private String status = "Active";

    // Component IDs and quantities for Combo
    private List<Integer> componentItemIds = new ArrayList<>();
    private List<Integer> componentQuantities = new ArrayList<>();

    public FBItemForm() {
    }

    @AssertTrue(message = "Giá vốn không được lớn hơn giá bán")
    public boolean isCostPriceNotExceedingSellingPrice() {
        if (sellingPrice == null || costPrice == null) {
            return true;
        }
        if (sellingPrice.compareTo(BigDecimal.ZERO) < 0 || costPrice.compareTo(BigDecimal.ZERO) < 0) {
            return true;
        }
        return costPrice.compareTo(sellingPrice) <= 0;
    }

    @AssertTrue(message = "Số lượng mặt hàng thành phần trong Combo phải từ 1 đến 100")
    public boolean isValidComboQuantities() {
        if (componentQuantities == null || componentQuantities.isEmpty()) {
            return true;
        }
        for (Integer qty : componentQuantities) {
            if (qty != null && (qty <= 0 || qty > 100)) {
                return false;
            }
        }
        return true;
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

    public List<Integer> getComponentItemIds() {
        return componentItemIds;
    }

    public void setComponentItemIds(List<Integer> componentItemIds) {
        this.componentItemIds = componentItemIds != null ? componentItemIds : new ArrayList<>();
    }

    public List<Integer> getComponentQuantities() {
        return componentQuantities;
    }

    public void setComponentQuantities(List<Integer> componentQuantities) {
        this.componentQuantities = componentQuantities != null ? componentQuantities : new ArrayList<>();
    }
}
