package com.cinema.controller.api;

import com.cinema.model.ManagerDashboardStats;
import com.cinema.service.ManagerDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerDashboardApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManagerDashboardApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagerDashboardService dashboardService;

    @Test
    @DisplayName("GET /api/manager/dashboard/stats: Should return live stats json")
    void testGetStats_Success() throws Exception {
        ManagerDashboardStats stats = new ManagerDashboardStats();
        stats.setTotalMovies(15);
        stats.setNowShowingMovies(9);
        stats.setTotalRooms(6);
        stats.setActiveRooms(5);
        stats.setBaseTicketPrice(new BigDecimal("85000"));

        when(dashboardService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/manager/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalMovies").value(15))
                .andExpect(jsonPath("$.data.nowShowingMovies").value(9))
                .andExpect(jsonPath("$.data.activeRooms").value(5))
                .andExpect(jsonPath("$.data.baseTicketPrice").value(85000));
    }

    @Test
    @DisplayName("GET /api/manager/dashboard/stats: Non-manager role should be rejected with 403 Forbidden")
    void testGetStats_Forbidden() throws Exception {
        mockMvc.perform(get("/api/manager/dashboard/stats").param("role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }
}
