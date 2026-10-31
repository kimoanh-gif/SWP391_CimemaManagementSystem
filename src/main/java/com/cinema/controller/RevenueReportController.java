package com.cinema.controller;

import com.cinema.dto.ReportFilterForm;
import com.cinema.model.report.RevenueReportData;
import com.cinema.service.RevenueReportService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

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
            @Valid @ModelAttribute("filterRequest") ReportFilterForm reportRequest,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (reportRequest.getPreset() == null || reportRequest.getPreset().isBlank()) {
            reportRequest.setPreset((reportRequest.getStartDate() != null || reportRequest.getEndDate() != null) ? "custom" : "thismonth");
        }
        if (reportRequest.getReportType() == null || reportRequest.getReportType().isBlank()) {
            reportRequest.setReportType("DAILY");
        }

        try {
            if (bindingResult.hasErrors()) {
                String msg = bindingResult.getAllErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException(msg);
            }
            RevenueReportData reportData = revenueReportService.getReportData(reportRequest);
            model.addAttribute("reportData", reportData);
            model.addAttribute("summary", reportData.getSummary());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            // Fallback to default dates if validation failed
            reportRequest.setPreset("thismonth");
            reportRequest.setStartDate(null);
            reportRequest.setEndDate(null);
            reportRequest.setReportType("DAILY");
            RevenueReportData reportData;
            try {
                reportData = revenueReportService.getReportData(reportRequest);
            } catch (IllegalArgumentException retryEx) {
                reportData = revenueReportService.getReportData(reportRequest);
            }
            model.addAttribute("reportData", reportData);
            model.addAttribute("summary", reportData != null ? reportData.getSummary() : null);
        }

        model.addAttribute("filterRequest", reportRequest);
        model.addAttribute("userRole", ManagerSecurityUtils.resolveRole(request));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.ENGLISH);
        model.addAttribute("currentDateStr", LocalDate.now().format(dateFormatter));

        return "manager/reports";
    }
}
