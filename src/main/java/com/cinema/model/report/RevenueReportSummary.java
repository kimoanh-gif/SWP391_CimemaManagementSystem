package com.cinema.model.report;

import java.math.BigDecimal;

/**
 * Top KPI Summary Cards for Revenue Report (Screen 47 & 49).
 */
public class RevenueReportSummary {

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal ticketRevenue = BigDecimal.ZERO;
    private BigDecimal fbRevenue = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private int totalTickets = 0;
    private int totalPaidBookings = 0;

    public RevenueReportSummary() {
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTicketRevenue() {
        return ticketRevenue != null ? ticketRevenue : BigDecimal.ZERO;
    }

    public void setTicketRevenue(BigDecimal ticketRevenue) {
        this.ticketRevenue = ticketRevenue;
    }

    public BigDecimal getFbRevenue() {
        return fbRevenue != null ? fbRevenue : BigDecimal.ZERO;
    }

    public void setFbRevenue(BigDecimal fbRevenue) {
        this.fbRevenue = fbRevenue;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public int getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(int totalTickets) {
        this.totalTickets = totalTickets;
    }

    public int getTotalPaidBookings() {
        return totalPaidBookings;
    }

    public void setTotalPaidBookings(int totalPaidBookings) {
        this.totalPaidBookings = totalPaidBookings;
    }
}
