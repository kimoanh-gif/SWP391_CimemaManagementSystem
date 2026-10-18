package com.cinema.model;

import java.math.BigDecimal;

public class SeatOption {
    private int seatId;
    private String rowLabel;
    private int seatNumber;
    private int gridRow;
    private int gridColumn;
    private String typeName;
    private BigDecimal priceMultiplier;
    private BigDecimal price;
    private boolean available;

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }
    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
    public int getGridRow() { return gridRow; }
    public void setGridRow(int gridRow) { this.gridRow = gridRow; }
    public int getGridColumn() { return gridColumn; }
    public void setGridColumn(int gridColumn) { this.gridColumn = gridColumn; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public BigDecimal getPriceMultiplier() { return priceMultiplier; }
    public void setPriceMultiplier(BigDecimal priceMultiplier) { this.priceMultiplier = priceMultiplier; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getLabel() { return rowLabel + seatNumber; }
}
