package com.cinema.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily aggregated revenue metrics (Screen 47 & 49).
 */
public class DailyRevenueItem {

    private LocalDate date;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal ticketRevenue = BigDecimal.ZERO;
    private BigDecimal fbRevenue = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private int ticketCount = 0;
    private int bookingCount = 0;

    public DailyRevenueItem() {
    }

    public DailyRevenueItem(LocalDate date, BigDecimal totalRevenue, BigDecimal ticketRevenue,
                            BigDecimal fbRevenue, BigDecimal discountAmount, int ticketCount, int bookingCount) {
        this.date = date;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.ticketRevenue = ticketRevenue != null ? ticketRevenue : BigDecimal.ZERO;
        this.fbRevenue = fbRevenue != null ? fbRevenue : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.ticketCount = ticketCount;
        this.bookingCount = bookingCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTicketRevenue() {
        return ticketRevenue;
    }

    public void setTicketRevenue(BigDecimal ticketRevenue) {
        this.ticketRevenue = ticketRevenue;
    }

    public BigDecimal getFbRevenue() {
        return fbRevenue;
    }

    public void setFbRevenue(BigDecimal fbRevenue) {
        this.fbRevenue = fbRevenue;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(int ticketCount) {
        this.ticketCount = ticketCount;
    }

    public int getBookingCount() {
        return bookingCount;
    }

    public void setBookingCount(int bookingCount) {
        this.bookingCount = bookingCount;
    }
}
