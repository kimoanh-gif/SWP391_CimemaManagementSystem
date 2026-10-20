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
}
