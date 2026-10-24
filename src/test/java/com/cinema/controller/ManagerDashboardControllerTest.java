package com.cinema.controller;

import com.cinema.model.ManagerDashboardStats;
import com.cinema.service.ManagerDashboardInsightsService;
import com.cinema.service.ManagerDashboardService;
import com.cinema.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManagerDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagerDashboardService dashboardService;

    @MockitoBean
    private ManagerDashboardInsightsService insightsService;

    @MockitoBean
    private SessionService sessionService;

    @Test
    @DisplayName("GET /manager/dashboard: Should render dashboard view with aggregated stats")
    void testShowDashboard_Success() throws Exception {
        ManagerDashboardStats stats = new ManagerDashboardStats();
        stats.setTotalMovies(10);
        stats.setNowShowingMovies(6);
        stats.setTotalRooms(5);
        stats.setBaseTicketPrice(new BigDecimal("85000"));

        when(dashboardService.getDashboardStats()).thenReturn(stats);
        when(dashboardService.getTodayShowtimes()).thenReturn(Collections.emptyList());
        when(dashboardService.getActiveMovies()).thenReturn(Collections.emptyList());
        when(dashboardService.getRoomsSummary()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/dashboard"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("todayShowtimes"))
                .andExpect(model().attributeExists("activeMovies"))
                .andExpect(model().attributeExists("rooms"))
                .andExpect(model().attributeExists("weeklyRevenue"))
                .andExpect(model().attributeExists("recentBookings"))
                .andExpect(model().attribute("userRole", "MANAGER"));
    }

    @Test
    @DisplayName("GET /manager: Should also map to dashboard view")
    void testShowDashboard_RootManagerPath_Success() throws Exception {
        ManagerDashboardStats stats = new ManagerDashboardStats();
        when(dashboardService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/manager"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/dashboard"));
    }

    @Test
    @DisplayName("GET /manager/dashboard: Customer role should be blocked with 403 Forbidden")
    void testShowDashboard_UnauthorizedRole_Forbidden() throws Exception {
        mockMvc.perform(get("/manager/dashboard").param("role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }
}
