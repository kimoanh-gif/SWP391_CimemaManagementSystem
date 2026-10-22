package com.cinema.dto;

import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Filter criteria for Revenue Reports (Screen 47 & 49).
 */
public class RevenueReportRequest {

    private LocalDate startDate;
    private LocalDate endDate;

    @Pattern(regexp = "^(today|last7days|thismonth|thisquarter|all|custom)$",
             message = "Khoảng thời gian chọn sẵn không hợp lệ")
    private String preset; // "today", "last7days", "thismonth", "thisquarter", "all", "custom"

    @Pattern(regexp = "^(DAILY|MOVIE|TRANSACTIONS)$",
             message = "Loại báo cáo không hợp lệ")
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
