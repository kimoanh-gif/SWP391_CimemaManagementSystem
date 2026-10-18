package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a Seat Type entity (Screen 57 / Screen 60 / UC-28 / BR-50).
 * Holds seat classification, price multiplier, badge color and status.
 */
public class SeatType {

    private Integer seatTypeId;
    private String typeCode;
    private String typeName;
    private String description;
    private BigDecimal priceMultiplier; // e.g. 1.00, 1.25, 1.80
    private String colorCode;           // e.g. #3B82F6
    private String status;              // 'Active', 'Inactive'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SeatType() {
    }

    public SeatType(Integer seatTypeId, String typeCode, String typeName, String description,
                    BigDecimal priceMultiplier, String colorCode, String status) {
        this.seatTypeId = seatTypeId;
        this.typeCode = typeCode;
        this.typeName = typeName;
        this.description = description;
        this.priceMultiplier = priceMultiplier;
        this.colorCode = colorCode;
        this.status = status;
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

    public boolean isActive() {
        return "Active".equalsIgnoreCase(this.status);
    }
}
