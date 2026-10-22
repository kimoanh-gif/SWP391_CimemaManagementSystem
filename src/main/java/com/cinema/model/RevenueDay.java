package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ticket and food & beverage revenue of one day (confirmed bookings only). */
public class RevenueDay {

    private final LocalDate date;
    private final BigDecimal ticketRevenue;
    private final BigDecimal fnbRevenue;

    public RevenueDay(LocalDate date, BigDecimal ticketRevenue, BigDecimal fnbRevenue) {
        this.date = date;
        this.ticketRevenue = ticketRevenue == null ? BigDecimal.ZERO : ticketRevenue;
        this.fnbRevenue = fnbRevenue == null ? BigDecimal.ZERO : fnbRevenue;
    }

    public LocalDate getDate() { return date; }
    public BigDecimal getTicketRevenue() { return ticketRevenue; }
    public BigDecimal getFnbRevenue() { return fnbRevenue; }
    public BigDecimal getTotal() { return ticketRevenue.add(fnbRevenue); }
}
