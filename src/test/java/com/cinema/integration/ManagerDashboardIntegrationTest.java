package com.cinema.integration;

import com.cinema.CinemaManagementApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the Manager Dashboard endpoint.
 * Uses the "local" profile which points to the developer's local SQL Server instance.
 * The test runs inside a transaction that rolls back, ensuring no persistent changes.
 */
@SpringBootTest(classes = CinemaManagementApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
public class ManagerDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /manager/dashboard returns OK and required summary attributes")
    void testDashboardSummary() throws Exception {
        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/dashboard"))
                .andExpect(model().attributeExists("movieCount"))
                .andExpect(model().attributeExists("roomCount"))
                .andExpect(model().attributeExists("upcomingShowtimeCount"));
    }
}
