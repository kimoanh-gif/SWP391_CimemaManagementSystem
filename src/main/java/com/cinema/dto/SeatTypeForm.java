package com.cinema.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Form DTO for SeatType configuration and price multiplier editing.
 */
public class SeatTypeForm {

    private Integer seatTypeId;

    @NotBlank(message = "Seat type code cannot be empty")
    @Size(max = 50, message = "Seat type code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Seat type code can only contain letters, numbers, hyphens or underscores")
    private String typeCode;

    @NotBlank(message = "Seat type name cannot be empty")
    @Size(max = 100, message = "Seat type name cannot exceed 100 characters")
    private String typeName;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Price multiplier cannot be empty")
    @DecimalMin(value = "0.50", message = "Minimum price multiplier is 0.50")
    @DecimalMax(value = "3.00", message = "Maximum price multiplier is 3.00")
    private BigDecimal priceMultiplier;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color code must be in HEX format (e.g. #3B82F6)")
    private String colorCode = "#3B82F6";

    @NotBlank(message = "Status cannot be empty")
    @Pattern(regexp = "^(Active|Inactive)$", message = "Seat type status must be Active or Inactive")
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
