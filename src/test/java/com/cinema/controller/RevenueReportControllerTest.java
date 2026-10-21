package com.cinema.controller;

import com.cinema.model.report.RevenueReportData;
import com.cinema.model.report.RevenueReportSummary;
import com.cinema.service.RevenueReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RevenueReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class RevenueReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevenueReportService revenueReportService;

    @Test
    @DisplayName("GET /manager/reports: Authorized Manager can access report screen")
    void testViewReports_Authorized() throws Exception {
        RevenueReportData data = new RevenueReportData();
        RevenueReportSummary summary = new RevenueReportSummary();
        summary.setTotalRevenue(new BigDecimal("1500000"));
        data.setSummary(summary);

        when(revenueReportService.getReportData(any())).thenReturn(data);

        mockMvc.perform(get("/manager/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/reports"))
                .andExpect(model().attributeExists("reportData"))
                .andExpect(model().attributeExists("summary"))
                .andExpect(model().attributeExists("filterRequest"));
    }

    @Test
    @DisplayName("GET /manager/reports: Unauthorized Customer role receives 403 Forbidden")
    void testViewReports_Unauthorized() throws Exception {
        mockMvc.perform(get("/manager/reports").param("role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /manager/reports: Handles date validation error gracefully")
    void testViewReports_InvalidDateRange() throws Exception {
        when(revenueReportService.getReportData(any()))
                .thenThrow(new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc."))
                .thenReturn(new RevenueReportData());

        mockMvc.perform(get("/manager/reports")
                        .param("startDate", "2026-10-25")
                        .param("endDate", "2026-10-10"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/reports"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /manager/reports?tab=movies: Correctly selects Movie Performance tab")
    void testViewReports_MoviesTab() throws Exception {
        RevenueReportData data = new RevenueReportData();
        data.setSummary(new RevenueReportSummary());
        when(revenueReportService.getReportData(any())).thenReturn(data);

        mockMvc.perform(get("/manager/reports").param("tab", "movies"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/reports"))
                .andExpect(model().attribute("activeTab", "movies"))
                .andExpect(model().attribute("pageTitle", "Reports & Analytics"));
    }

    @Test
    @DisplayName("GET /manager/reports?tab=transactions: Correctly selects Transaction Ledger tab")
    void testViewReports_TransactionsTab() throws Exception {
        RevenueReportData data = new RevenueReportData();
        data.setSummary(new RevenueReportSummary());
        when(revenueReportService.getReportData(any())).thenReturn(data);

        mockMvc.perform(get("/manager/reports").param("tab", "transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/reports"))
                .andExpect(model().attribute("activeTab", "transactions"))
                .andExpect(model().attribute("pageTitle", "Reports & Analytics"));
    }

    @Test
    @DisplayName("GET /manager/reports/movies: Compatibility route redirects to tab=movies")
    void testRedirectMoviesTab() throws Exception {
        mockMvc.perform(get("/manager/reports/movies").param("preset", "thismonth"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/reports?tab=movies&preset=thismonth"));
    }

    @Test
    @DisplayName("GET /manager/reports/transactions: Compatibility route redirects to tab=transactions")
    void testRedirectTransactionsTab() throws Exception {
        mockMvc.perform(get("/manager/reports/transactions").param("preset", "thismonth"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/reports?tab=transactions&preset=thismonth"));
    }
}
