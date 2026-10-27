package com.cinema.model.report;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite container for revenue report responses.
 */
public class RevenueReportData {

    private RevenueReportSummary summary = new RevenueReportSummary();
    private List<DailyRevenueItem> dailyItems = new ArrayList<>();
    private List<MovieRevenueItem> movieItems = new ArrayList<>();
    private List<TransactionRevenueItem> transactionItems = new ArrayList<>();

    public RevenueReportData() {
    }

    public RevenueReportSummary getSummary() {
        return summary;
    }

    public void setSummary(RevenueReportSummary summary) {
        this.summary = summary;
    }

    public List<DailyRevenueItem> getDailyItems() {
        return dailyItems;
    }

    public void setDailyItems(List<DailyRevenueItem> dailyItems) {
        this.dailyItems = dailyItems;
    }

    public List<MovieRevenueItem> getMovieItems() {
        return movieItems;
    }

    public void setMovieItems(List<MovieRevenueItem> movieItems) {
        this.movieItems = movieItems;
    }

    public List<TransactionRevenueItem> getTransactionItems() {
        return transactionItems;
    }

    public void setTransactionItems(List<TransactionRevenueItem> transactionItems) {
        this.transactionItems = transactionItems;
    }

    public int getTotalMovieShowtimes() {
        if (movieItems == null || movieItems.isEmpty()) return 0;
        return movieItems.stream().mapToInt(MovieRevenueItem::getShowtimesCount).sum();
    }

    public int getTotalMovieTicketsSold() {
        if (movieItems == null || movieItems.isEmpty()) return 0;
        return movieItems.stream().mapToInt(MovieRevenueItem::getSoldTickets).sum();
    }

    public java.math.BigDecimal getTotalMovieTicketRevenue() {
        if (movieItems == null || movieItems.isEmpty()) return java.math.BigDecimal.ZERO;
        return movieItems.stream()
                .map(MovieRevenueItem::getTicketRevenue)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public java.math.BigDecimal getTotalMovieFbRevenue() {
        if (movieItems == null || movieItems.isEmpty()) return java.math.BigDecimal.ZERO;
        return movieItems.stream()
                .map(MovieRevenueItem::getFbRevenue)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public java.math.BigDecimal getTotalMovieRevenue() {
        if (movieItems == null || movieItems.isEmpty()) return java.math.BigDecimal.ZERO;
        return movieItems.stream()
                .map(MovieRevenueItem::getTotalRevenue)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public java.math.BigDecimal getTotalTransactionTicketAmount() {
        if (transactionItems == null || transactionItems.isEmpty()) return java.math.BigDecimal.ZERO;
        return transactionItems.stream()
                .map(TransactionRevenueItem::getTicketAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public java.math.BigDecimal getTotalTransactionFbAmount() {
        if (transactionItems == null || transactionItems.isEmpty()) return java.math.BigDecimal.ZERO;
        return transactionItems.stream()
                .map(TransactionRevenueItem::getFbAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public java.math.BigDecimal getTotalTransactionTotalAmount() {
        if (transactionItems == null || transactionItems.isEmpty()) return java.math.BigDecimal.ZERO;
        return transactionItems.stream()
                .map(TransactionRevenueItem::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}
