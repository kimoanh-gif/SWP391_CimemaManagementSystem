package com.cinema.dto;

import com.cinema.model.ManagerPromotionRow;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Form for M11.1 / M11.3 when the type is "Promotion campaign".
 * Validation is done in ManagerPromotionService so the messages match the mockup.
 */
public class PromotionForm {

    private String name;
    private String description;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
    /** ACTIVE, DRAFT (create) – ACTIVE, PAUSED, INACTIVE (edit). */
    private String status = "ACTIVE";

    public static PromotionForm from(ManagerPromotionRow row) {
        PromotionForm f = new PromotionForm();
        f.name = row.getName();
        f.description = row.getDescription();
        f.startDate = row.getStartDate();
        f.endDate = row.getEndDate();
        f.status = row.getStatus();
        return f;
    }

    private static final java.time.format.DateTimeFormatter INPUT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    /** Value for an HTML datetime-local input. */
    public static String toInput(LocalDateTime value) {
        return value != null ? value.format(INPUT) : "";
    }

    public String getStartDateInput() { return toInput(startDate); }
    public String getEndDateInput() { return toInput(endDate); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
