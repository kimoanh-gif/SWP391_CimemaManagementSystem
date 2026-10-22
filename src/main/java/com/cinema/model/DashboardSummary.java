package com.cinema.model;

import java.math.BigDecimal;

/** KPI numbers shown on the Manager dashboard (M01). */
public class DashboardSummary {

    private int totalMovies;
    private int nowShowingMovies;
    private int comingSoonMovies;
    private int totalGenres;
    private int showtimesToday;
    private int showtimesFinishedToday;
    private int showtimesUpcomingToday;
    private int ticketsToday;
    private int ticketsYesterday;
    private BigDecimal revenueToday = BigDecimal.ZERO;
    private BigDecimal revenueYesterday = BigDecimal.ZERO;
    private int activePromotions;
    private int activeVouchers;

    public int getTotalMovies() { return totalMovies; }
    public void setTotalMovies(int totalMovies) { this.totalMovies = totalMovies; }
    public int getNowShowingMovies() { return nowShowingMovies; }
    public void setNowShowingMovies(int nowShowingMovies) { this.nowShowingMovies = nowShowingMovies; }
    public int getComingSoonMovies() { return comingSoonMovies; }
    public void setComingSoonMovies(int comingSoonMovies) { this.comingSoonMovies = comingSoonMovies; }
    public int getTotalGenres() { return totalGenres; }
    public void setTotalGenres(int totalGenres) { this.totalGenres = totalGenres; }
    public int getShowtimesToday() { return showtimesToday; }
    public void setShowtimesToday(int showtimesToday) { this.showtimesToday = showtimesToday; }
    public int getShowtimesFinishedToday() { return showtimesFinishedToday; }
    public void setShowtimesFinishedToday(int showtimesFinishedToday) { this.showtimesFinishedToday = showtimesFinishedToday; }
    public int getShowtimesUpcomingToday() { return showtimesUpcomingToday; }
    public void setShowtimesUpcomingToday(int showtimesUpcomingToday) { this.showtimesUpcomingToday = showtimesUpcomingToday; }
    public int getTicketsToday() { return ticketsToday; }
    public void setTicketsToday(int ticketsToday) { this.ticketsToday = ticketsToday; }
    public int getTicketsYesterday() { return ticketsYesterday; }
    public void setTicketsYesterday(int ticketsYesterday) { this.ticketsYesterday = ticketsYesterday; }
    public BigDecimal getRevenueToday() { return revenueToday; }
    public void setRevenueToday(BigDecimal revenueToday) { this.revenueToday = revenueToday == null ? BigDecimal.ZERO : revenueToday; }
    public BigDecimal getRevenueYesterday() { return revenueYesterday; }
    public void setRevenueYesterday(BigDecimal revenueYesterday) { this.revenueYesterday = revenueYesterday == null ? BigDecimal.ZERO : revenueYesterday; }
    public int getActivePromotions() { return activePromotions; }
    public void setActivePromotions(int activePromotions) { this.activePromotions = activePromotions; }
    public int getActiveVouchers() { return activeVouchers; }
    public void setActiveVouchers(int activeVouchers) { this.activeVouchers = activeVouchers; }

    /** Percentage change of tickets vs. yesterday, or null when yesterday had no sales. */
    public Double getTicketChangePercent() {
        return percentChange(ticketsToday, ticketsYesterday);
    }

    /** Percentage change of revenue vs. yesterday, or null when yesterday had no revenue. */
    public Double getRevenueChangePercent() {
        return percentChange(revenueToday.doubleValue(), revenueYesterday.doubleValue());
    }

    static Double percentChange(double today, double yesterday) {
        if (yesterday <= 0) return null;
        return Math.round((today - yesterday) / yesterday * 1000.0) / 10.0;
    }
}
