package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One bar group of the Manager Dashboard "Revenue Overview" chart (ticket + F&B revenue for a day).
 * Heights are percentages of the tallest value in the displayed period so the template stays logic-free.
 */
public class DashboardRevenueDay {
    private LocalDate date;
    private String label;
    private boolean today;
    private BigDecimal ticketRevenue = BigDecimal.ZERO;
    private BigDecimal fbRevenue = BigDecimal.ZERO;
    private int ticketHeightPercent;
    private int fbHeightPercent;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isToday() { return today; }
    public void setToday(boolean today) { this.today = today; }

    public BigDecimal getTicketRevenue() { return ticketRevenue; }
    public void setTicketRevenue(BigDecimal ticketRevenue) {
        this.ticketRevenue = ticketRevenue == null ? BigDecimal.ZERO : ticketRevenue;
    }

    public BigDecimal getFbRevenue() { return fbRevenue; }
    public void setFbRevenue(BigDecimal fbRevenue) {
        this.fbRevenue = fbRevenue == null ? BigDecimal.ZERO : fbRevenue;
    }

    public int getTicketHeightPercent() { return ticketHeightPercent; }
    public void setTicketHeightPercent(int ticketHeightPercent) { this.ticketHeightPercent = ticketHeightPercent; }

    public int getFbHeightPercent() { return fbHeightPercent; }
    public void setFbHeightPercent(int fbHeightPercent) { this.fbHeightPercent = fbHeightPercent; }

    public BigDecimal getTotalRevenue() {
        return ticketRevenue.add(fbRevenue);
    }
}
