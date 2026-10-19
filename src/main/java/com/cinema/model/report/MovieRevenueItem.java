package com.cinema.model.report;

import java.math.BigDecimal;

/**
 * Movie performance metrics for revenue reports (Screen 47 & 49).
 */
public class MovieRevenueItem {

    private int movieId;
    private String title;
    private int showtimesCount;
    private int soldTickets;
    private BigDecimal ticketRevenue = BigDecimal.ZERO;
    private BigDecimal fbRevenue = BigDecimal.ZERO;
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    public MovieRevenueItem() {
    }

    public MovieRevenueItem(int movieId, String title, int showtimesCount, int soldTickets,
                            BigDecimal ticketRevenue, BigDecimal fbRevenue, BigDecimal totalRevenue) {
        this.movieId = movieId;
        this.title = title;
        this.showtimesCount = showtimesCount;
        this.soldTickets = soldTickets;
        this.ticketRevenue = ticketRevenue != null ? ticketRevenue : BigDecimal.ZERO;
        this.fbRevenue = fbRevenue != null ? fbRevenue : BigDecimal.ZERO;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getShowtimesCount() {
        return showtimesCount;
    }

    public void setShowtimesCount(int showtimesCount) {
        this.showtimesCount = showtimesCount;
    }

    public int getSoldTickets() {
        return soldTickets;
    }

    public void setSoldTickets(int soldTickets) {
        this.soldTickets = soldTickets;
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

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
