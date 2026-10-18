package com.cinema.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Form DTO for SeatType configuration and price multiplier editing.
 */
public class SeatTypeForm {

    private Integer seatTypeId;

    @NotBlank(message = "Mã loại ghế không được để trống")
    @Size(max = 50, message = "Mã loại ghế tối đa 50 ký tự")
    private String typeCode;

    @NotBlank(message = "Tên loại ghế không được để trống")
    @Size(max = 100, message = "Tên loại ghế tối đa 100 ký tự")
    private String typeName;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @NotNull(message = "Hệ số giá không được để trống")
    @DecimalMin(value = "0.50", message = "Hệ số giá tối thiểu là 0.50")
    @DecimalMax(value = "3.00", message = "Hệ số giá tối đa là 3.00")
    private BigDecimal priceMultiplier;

    private String colorCode = "#3B82F6";

    private String status = "Active";

    public SeatTypeForm() {
    }

    public Integer getSeatTypeId() {
        return seatTypeId;
    }

    public void setSeatTypeId(Integer seatTypeId) {
        this.seatTypeId = seatTypeId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(BigDecimal priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
