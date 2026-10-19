package com.cinema.controller;

import com.cinema.model.report.RevenueReportData;
import com.cinema.model.report.RevenueReportRequest;
import com.cinema.service.RevenueReportService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller for Screen 47 & 49: View Reports & Statistics & Revenue Report Export (UC-34).
 */
@Controller
@RequestMapping({"/manager/reports", "/manager/reports/revenue"})
public class RevenueReportController {

    private final RevenueReportService revenueReportService;

    public RevenueReportController(RevenueReportService revenueReportService) {
        this.revenueReportService = revenueReportService;
    }

    @GetMapping
    public String viewReports(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAILY") String reportType,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        RevenueReportRequest reportRequest = new RevenueReportRequest();
        reportRequest.setPreset(preset != null ? preset : ((startDate != null || endDate != null) ? "custom" : "thismonth"));
        reportRequest.setStartDate(startDate);
        reportRequest.setEndDate(endDate);
        reportRequest.setReportType(reportType);

        try {
            RevenueReportData reportData = revenueReportService.getReportData(reportRequest);
            model.addAttribute("reportData", reportData);
            model.addAttribute("summary", reportData.getSummary());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            // Fallback to default dates if validation failed
            reportRequest.setPreset("thismonth");
            reportRequest.setStartDate(null);
            reportRequest.setEndDate(null);
            RevenueReportData reportData = revenueReportService.getReportData(reportRequest);
            model.addAttribute("reportData", reportData);
            model.addAttribute("summary", reportData.getSummary());
        }

        model.addAttribute("filterRequest", reportRequest);
        model.addAttribute("userRole", ManagerSecurityUtils.resolveRole(request));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, 'ngày' dd/MM/yyyy", new Locale("vi", "VN"));
        model.addAttribute("currentDateStr", LocalDate.now().format(dateFormatter));

        return "manager/reports";
    }
}
