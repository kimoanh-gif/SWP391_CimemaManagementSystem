package com.cinema.security;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.ManagerDashboardService;
import com.cinema.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ManagerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagerDashboardService dashboardService;

    private AuthenticatedUser createTestUser(String role) {
        return new AuthenticatedUser(
                100,
                role.toLowerCase(),
                role.toLowerCase() + "@cinema.com",
                role + " User",
                "0900000000",
                role,
                Set.of("ROLE_" + role),
                true,
                SessionService.AUTH_METHOD_PASSWORD
        );
    }

    @Test
    @DisplayName("SEC-01: Anonymous request to /manager/dashboard must redirect to /login")
    void testAnonymousAccessToManagerDashboard_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("SEC-01: Anonymous request to /api/manager/** must return 401 Unauthorized")
    void testAnonymousAccessToManagerApi_Returns401() throws Exception {
        mockMvc.perform(get("/api/manager/reports/data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SEC-01: Customer accessing /manager/dashboard must receive 403 Forbidden")
    void testCustomerAccessToManagerDashboard_Forbidden() throws Exception {
        mockMvc.perform(get("/manager/dashboard")
                        .sessionAttr(SessionService.ATTR_USER, createTestUser("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-01: Customer accessing /api/manager/** must receive 403 Forbidden")
    void testCustomerAccessToManagerApi_Forbidden() throws Exception {
        mockMvc.perform(get("/api/manager/reports/data")
                        .sessionAttr(SessionService.ATTR_USER, createTestUser("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-01: Staff accessing /manager/dashboard must receive 403 Forbidden")
    void testStaffAccessToManagerDashboard_Forbidden() throws Exception {
        mockMvc.perform(get("/manager/dashboard")
                        .sessionAttr(SessionService.ATTR_USER, createTestUser("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-01: Authenticated Manager can access /manager/dashboard with 200 OK")
    void testManagerAccessToManagerDashboard_Success() throws Exception {
        mockMvc.perform(get("/manager/dashboard")
                        .sessionAttr(SessionService.ATTR_USER, createTestUser("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SEC-01: Authenticated Admin can access /manager/dashboard with 200 OK")
    void testAdminAccessToManagerDashboard_Success() throws Exception {
        mockMvc.perform(get("/manager/dashboard")
                        .sessionAttr(SessionService.ATTR_USER, createTestUser("ADMIN")))
                .andExpect(status().isOk());
    }
}
