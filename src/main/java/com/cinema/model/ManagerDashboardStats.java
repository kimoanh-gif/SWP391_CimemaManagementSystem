package com.cinema.model;

import java.math.BigDecimal;

/**
 * DTO representing live operational summary metrics for Screen 53: Manager Dashboard.
 * All metrics are strictly computed from real database records.
 */
public class ManagerDashboardStats {

    private int totalMovies;
    private int nowShowingMovies;
    private int upcomingMovies;
    private int totalGenres;
    private int totalRooms;
    private int activeRooms;
    private int todayShowtimesCount;
    private int totalSeatTypes;
    private int totalPricingRules;
    private BigDecimal baseTicketPrice;
    private BigDecimal averageTicketPrice;
    private int totalFBItems;
    private int activeFBItems;

    public ManagerDashboardStats() {
        this.baseTicketPrice = BigDecimal.ZERO;
        this.averageTicketPrice = BigDecimal.ZERO;
    }

    // Getters and Setters
    public int getTotalMovies() { return totalMovies; }
    public void setTotalMovies(int totalMovies) { this.totalMovies = totalMovies; }

    public int getNowShowingMovies() { return nowShowingMovies; }
    public void setNowShowingMovies(int nowShowingMovies) { this.nowShowingMovies = nowShowingMovies; }

    public int getUpcomingMovies() { return upcomingMovies; }
    public void setUpcomingMovies(int upcomingMovies) { this.upcomingMovies = upcomingMovies; }

    public int getTotalGenres() { return totalGenres; }
    public void setTotalGenres(int totalGenres) { this.totalGenres = totalGenres; }

    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }

    public int getActiveRooms() { return activeRooms; }
    public void setActiveRooms(int activeRooms) { this.activeRooms = activeRooms; }

    public int getTodayShowtimesCount() { return todayShowtimesCount; }
    public void setTodayShowtimesCount(int todayShowtimesCount) { this.todayShowtimesCount = todayShowtimesCount; }

    public int getTotalSeatTypes() { return totalSeatTypes; }
    public void setTotalSeatTypes(int totalSeatTypes) { this.totalSeatTypes = totalSeatTypes; }

    public int getTotalPricingRules() { return totalPricingRules; }
    public void setTotalPricingRules(int totalPricingRules) { this.totalPricingRules = totalPricingRules; }

    public BigDecimal getBaseTicketPrice() { return baseTicketPrice; }
    public void setBaseTicketPrice(BigDecimal baseTicketPrice) { this.baseTicketPrice = baseTicketPrice; }

    public BigDecimal getAverageTicketPrice() { return averageTicketPrice; }
    public void setAverageTicketPrice(BigDecimal averageTicketPrice) { this.averageTicketPrice = averageTicketPrice; }

    public int getTotalFBItems() { return totalFBItems; }
    public void setTotalFBItems(int totalFBItems) { this.totalFBItems = totalFBItems; }

    public int getActiveFBItems() { return activeFBItems; }
    public void setActiveFBItems(int activeFBItems) { this.activeFBItems = activeFBItems; }
}
