package com.cinema.controller.api;

import com.cinema.model.report.DailyRevenueItem;
import com.cinema.model.report.RevenueReportData;
import com.cinema.model.report.RevenueReportSummary;
import com.cinema.service.RevenueReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RevenueReportApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class RevenueReportApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevenueReportService revenueReportService;

    @Test
    @DisplayName("GET /api/manager/reports/data: Returns JSON report data successfully")
    void testGetReportData_Success() throws Exception {
        RevenueReportData data = new RevenueReportData();
        RevenueReportSummary summary = new RevenueReportSummary();
        summary.setTotalRevenue(new BigDecimal("500000"));
        summary.setTicketRevenue(new BigDecimal("400000"));
        summary.setFbRevenue(new BigDecimal("100000"));
        summary.setTotalTickets(5);
        summary.setTotalPaidBookings(2);
        data.setSummary(summary);

        DailyRevenueItem daily = new DailyRevenueItem(LocalDate.now(), new BigDecimal("500000"),
                new BigDecimal("400000"), new BigDecimal("100000"), BigDecimal.ZERO, 5, 2);
        data.setDailyItems(List.of(daily));

        when(revenueReportService.getReportData(any())).thenReturn(data);

        mockMvc.perform(get("/api/manager/reports/data")
                        .param("preset", "thismonth")
                        .param("reportType", "DAILY"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.totalRevenue").value(500000))
                .andExpect(jsonPath("$.data.dailyItems[0].ticketCount").value(5));
    }

    @Test
    @DisplayName("GET /api/manager/reports/data: Invalid date range returns 400 Bad Request")
    void testGetReportData_InvalidDateRange() throws Exception {
        when(revenueReportService.getReportData(any()))
                .thenThrow(new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc."));

        mockMvc.perform(get("/api/manager/reports/data")
                        .param("startDate", "2026-10-20")
                        .param("endDate", "2026-10-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ngày bắt đầu không được lớn hơn ngày kết thúc."));
    }

    @Test
    @DisplayName("GET /api/manager/reports/data: Unauthorized role returns 403 Forbidden")
    void testGetReportData_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/manager/reports/data").param("role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/manager/reports/export: Exports downloadable CSV file with correct headers")
    void testExportRevenueReport_Success() throws Exception {
        String csvContent = "BÁO CÁO DOANH THU\nTổng cộng,1000000\n";
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        when(revenueReportService.generateCsvReport(any())).thenReturn(csvBytes);

        mockMvc.perform(get("/api/manager/reports/export")
                        .param("preset", "thismonth")
                        .param("reportType", "DAILY"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"BaoCaoDoanhThu_daily_")))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(content().bytes(csvBytes));
    }

    @Test
    @DisplayName("GET /api/manager/reports/export: Invalid date range returns 400 Bad Request")
    void testExportRevenueReport_InvalidDateRange() throws Exception {
        when(revenueReportService.generateCsvReport(any()))
                .thenThrow(new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc."));

        mockMvc.perform(get("/api/manager/reports/export")
                        .param("startDate", "2026-10-30")
                        .param("endDate", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ngày bắt đầu không được lớn hơn ngày kết thúc."));
    }
}
