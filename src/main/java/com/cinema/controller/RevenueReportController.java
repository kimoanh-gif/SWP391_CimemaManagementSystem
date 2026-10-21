package com.cinema.controller;

import com.cinema.model.report.MovieRevenueItem;
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
 * Unified Controller for Reports & Analytics (Screen M12, M13, M14 merged).
 * Consolidates Revenue Overview, Movie Performance, and Transaction Ledgers into one screen.
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
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String reportType,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        // Normalize active internal tab
        String activeTab = "revenue";
        if (tab != null && !tab.isBlank()) {
            String clean = tab.toLowerCase().trim();
            if ("movies".equals(clean) || "movie".equals(clean)) {
                activeTab = "movies";
            } else if ("transactions".equals(clean) || "transaction".equals(clean) || "summary".equals(clean) || "financial".equals(clean) || "export".equals(clean)) {
                activeTab = "transactions";
            } else {
                activeTab = "revenue";
            }
        } else if (reportType != null && !reportType.isBlank()) {
            if ("MOVIE".equalsIgnoreCase(reportType)) {
                activeTab = "movies";
            } else if ("TRANSACTIONS".equalsIgnoreCase(reportType)) {
                activeTab = "transactions";
            } else {
                activeTab = "revenue";
            }
        }

        String effectiveReportType = "DAILY";
        if ("movies".equals(activeTab)) {
            effectiveReportType = "MOVIE";
        } else if ("transactions".equals(activeTab)) {
            effectiveReportType = "TRANSACTIONS";
        }

        RevenueReportRequest reportRequest = new RevenueReportRequest();
        reportRequest.setPreset(preset != null ? preset : ((startDate != null || endDate != null) ? "custom" : "thismonth"));
        reportRequest.setStartDate(startDate);
        reportRequest.setEndDate(endDate);
        reportRequest.setReportType(effectiveReportType);

        RevenueReportData reportData;
        try {
            reportData = revenueReportService.getReportData(reportRequest);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            // Fallback to default month dates if validation failed
            reportRequest.setPreset("thismonth");
            reportRequest.setStartDate(null);
            reportRequest.setEndDate(null);
            reportData = revenueReportService.getReportData(reportRequest);
        }

        model.addAttribute("reportData", reportData);
        model.addAttribute("summary", reportData != null ? reportData.getSummary() : null);
        model.addAttribute("filterRequest", reportRequest);
        model.addAttribute("activeTab", activeTab);

        // Unified layout bindings
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("pageTitle", "Reports & Analytics");
        model.addAttribute("pageDescription", "Revenue monitoring, movie performance, and financial auditing.");
        model.addAttribute("userRole", ManagerSecurityUtils.resolveRole(request));

        // Aggregate total movie showtimes for Movie Performance tab footer
        int totalMovieShowtimes = 0;
        if (reportData != null && reportData.getMovieItems() != null) {
            totalMovieShowtimes = reportData.getMovieItems().stream()
                    .mapToInt(MovieRevenueItem::getShowtimesCount)
                    .sum();
        }
        model.addAttribute("totalMovieShowtimes", totalMovieShowtimes);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
        model.addAttribute("currentDateStr", LocalDate.now().format(dateFormatter));

        return "manager/reports";
    }

    @GetMapping("/movies")
    public String redirectMovies(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return buildRedirectUrl("movies", preset, startDate, endDate);
    }

    @GetMapping("/transactions")
    public String redirectTransactions(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return buildRedirectUrl("transactions", preset, startDate, endDate);
    }

    private String buildRedirectUrl(String tab, String preset, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder("redirect:/manager/reports?tab=").append(tab);
        if (preset != null && !preset.isBlank()) {
            sb.append("&preset=").append(preset);
        }
        if (startDate != null) {
            sb.append("&startDate=").append(startDate);
        }
        if (endDate != null) {
            sb.append("&endDate=").append(endDate);
        }
        return sb.toString();
    }
}
