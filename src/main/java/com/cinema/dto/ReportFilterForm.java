package com.cinema.dto;

import com.cinema.model.report.RevenueReportRequest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Form DTO for validating Revenue Report filter & export parameters (Functions 44 & 46).
 */
public class ReportFilterForm extends RevenueReportRequest {

    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    public LocalDate getStartDate() {
        return super.getStartDate();
    }

    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    public LocalDate getEndDate() {
        return super.getEndDate();
    }

    @Override
    @Pattern(regexp = "^(?i)(today|last7days|thismonth|thisquarter|all|custom)$",
            message = "Report period preset is invalid")
    public String getPreset() {
        return super.getPreset();
    }

    @Override
    @Pattern(regexp = "^(?i)(DAILY|MOVIE|TRANSACTIONS)$",
            message = "Report type must be DAILY, MOVIE or TRANSACTIONS")
    public String getReportType() {
        return super.getReportType();
    }

    @AssertTrue(message = "Start date cannot be after end date.")
    public boolean isValidDateRange() {
        if (getStartDate() != null && getEndDate() != null) {
            return !getStartDate().isAfter(getEndDate());
        }
        return true;
    }
}

