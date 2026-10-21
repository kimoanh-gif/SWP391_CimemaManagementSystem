package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.CustomerAccountService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SessionService sessionService;

    @Mock
    private CustomerAccountService customerAccountService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    private AuthenticatedUser createTestAdmin() {
        return new AuthenticatedUser(
                1,
                "admin",
                "admin@cinema.com",
                "System Admin",
                "0900000000",
                "ADMIN",
                Collections.emptySet(),
                true,
                "PASSWORD"
        );
    }

    @Test
    @DisplayName("GET /admin: Root mapping should resolve to admin/dashboard view")
    void testAdminRootMapping() throws Exception {
        when(sessionService.getCurrentUser(any(HttpSession.class))).thenReturn(createTestAdmin());

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("fullName", "System Admin"));
    }

    @Test
    @DisplayName("GET /admin/: Trailing slash should resolve to admin/dashboard view")
    void testAdminTrailingSlashMapping() throws Exception {
        when(sessionService.getCurrentUser(any(HttpSession.class))).thenReturn(createTestAdmin());

        mockMvc.perform(get("/admin/"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("GET /admin/dashboard: Explicit dashboard path should resolve to admin/dashboard view")
    void testAdminDashboardExplicitMapping() throws Exception {
        when(sessionService.getCurrentUser(any(HttpSession.class))).thenReturn(createTestAdmin());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("GET /admin/customers: Should render customers view with account stats")
    void testAdminCustomersMapping() throws Exception {
        when(sessionService.getCurrentUser(any(HttpSession.class))).thenReturn(createTestAdmin());
        when(customerAccountService.getTotalCount()).thenReturn(50L);
        when(customerAccountService.getActiveCount()).thenReturn(45L);
        when(customerAccountService.getSuspendedCount()).thenReturn(3L);
        when(customerAccountService.getPendingCount()).thenReturn(2L);

        mockMvc.perform(get("/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/customers"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("customerStats"));
    }

    @Test
    @DisplayName("GET /admin/staffs: Should render staffs view")
    void testAdminStaffsMapping() throws Exception {
        when(sessionService.getCurrentUser(any(HttpSession.class))).thenReturn(createTestAdmin());

        mockMvc.perform(get("/admin/staffs"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staffs"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("GET /admin/roles: Should render roles view")
    void testAdminRolesMapping() throws Exception {
        when(sessionService.getCurrentUser(any(HttpSession.class))).thenReturn(createTestAdmin());

        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/roles"))
                .andExpect(model().attributeExists("user"));
    }
}
