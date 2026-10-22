package com.cinema.controller.api;

import com.cinema.dto.ApiResponse;
import com.cinema.model.report.RevenueReportData;
import com.cinema.dto.RevenueReportRequest;
import com.cinema.service.RevenueReportService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * REST API for Revenue Report & Export (Screen 47 & 49).
 */
@RestController
@RequestMapping("/api/manager/reports")
public class RevenueReportApiController {

    private final RevenueReportService revenueReportService;

    public RevenueReportApiController(RevenueReportService revenueReportService) {
        this.revenueReportService = revenueReportService;
    }

    @GetMapping("/data")
    public ResponseEntity<ApiResponse<RevenueReportData>> getReportData(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAILY") String reportType,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        RevenueReportRequest reportRequest = new RevenueReportRequest();
        reportRequest.setPreset(preset != null ? preset : ((startDate != null || endDate != null) ? "custom" : "thismonth"));
        reportRequest.setStartDate(startDate);
        reportRequest.setEndDate(endDate);
        reportRequest.setReportType(reportType);

        try {
            RevenueReportData data = revenueReportService.getReportData(reportRequest);
            return ResponseEntity.ok(ApiResponse.ok("Lấy dữ liệu báo cáo doanh thu thành công", data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportRevenueReport(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAILY") String reportType,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        RevenueReportRequest reportRequest = new RevenueReportRequest();
        reportRequest.setPreset(preset != null ? preset : ((startDate != null || endDate != null) ? "custom" : "thismonth"));
        reportRequest.setStartDate(startDate);
        reportRequest.setEndDate(endDate);
        reportRequest.setReportType(reportType);

        try {
            byte[] csvBytes = revenueReportService.generateCsvReport(reportRequest);

            String filename = String.format("BaoCaoDoanhThu_%s_%s.csv",
                    reportRequest.getReportType().toLowerCase(),
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
            return new ResponseEntity<>(ex.getMessage().getBytes(StandardCharsets.UTF_8), errorHeaders, HttpStatus.BAD_REQUEST);
        }
    }
}
