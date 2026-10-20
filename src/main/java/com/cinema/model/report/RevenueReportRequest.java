package com.cinema.model.report;

import java.time.LocalDate;

/**
 * Filter criteria for Revenue Reports (Screen 47 & 49).
 */
public class RevenueReportRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private String preset; // "today", "last7days", "thismonth", "thisquarter", "all", "custom"
    private String reportType; // "DAILY", "MOVIE", "TRANSACTIONS"

    public RevenueReportRequest() {
        this.reportType = "DAILY";
        this.preset = "thismonth";
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
}
