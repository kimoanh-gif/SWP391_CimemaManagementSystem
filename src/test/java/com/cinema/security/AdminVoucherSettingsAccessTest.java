package com.cinema.security;

import com.cinema.config.SecurityConfig;
import com.cinema.controller.SystemConfigApiController;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.SystemSetting;
import com.cinema.service.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Configuration permissions enforced through the real controller and security chain. */
class AdminVoucherSettingsAccessTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    SystemConfigService service;
    AuthorizationSnapshotService snapshots;
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.register(Config.class); context.refresh();
        service = context.getBean(SystemConfigService.class); snapshots = context.getBean(AuthorizationSnapshotService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build();
        var setting = new SystemSetting(); setting.setSettingId(1); setting.setCategory("VOUCHER");
        setting.setSettingKey("VOUCHER_MAX_DISCOUNT_PERCENT"); setting.setSettingName("Max discount"); setting.setValue("50");
        when(service.getAllSettingsGrouped()).thenReturn(Map.of("VOUCHER",List.of(setting)));
        when(service.getSettingById(1)).thenReturn(Optional.of(setting));
        when(service.updateSetting(1,"40")).thenReturn(SystemConfigService.OperationResult.success("Saved",setting.getSettingKey(),"50","40"));
    }
    @AfterEach void close() { context.close(); }
    AuthenticatedUser user(String role, Set<String> permissions) {
        return new AuthenticatedUser(1,"u","u@example.com","User",null,role,permissions,true,"PASSWORD");
    }
    @Test void revokingConfigurationPermissionBlocksWritesInTheSameSession() throws Exception {
        var session = new MockHttpSession(); var admin = user("ADMIN",Set.of("voucher:configure"));
        session.setAttribute(SessionService.ATTR_USER,admin); when(snapshots.refresh(session)).thenReturn(admin);
        mvc.perform(put("/api/admin/config/settings/1").session(session).contentType("application/json").content("{\"value\":\"40\"}"))
                .andExpect(status().isOk());
        // Same session, refreshed from the database with the voucher permission removed.
        when(snapshots.refresh(session)).thenReturn(user("ADMIN",Set.of()));
        mvc.perform(get("/api/admin/config/settings").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.VOUCHER").doesNotExist());
        mvc.perform(put("/api/admin/config/settings/1").session(session).contentType("application/json").content("{\"value\":\"40\"}"))
                .andExpect(status().isForbidden());
        verify(service,times(1)).updateSetting(1,"40");
    }
    @Test void voucherPermissionDoesNotAllowManagerIntoAdminConfiguration() throws Exception {
        var session = new MockHttpSession(); var manager = user("MANAGER",Set.of("voucher:configure"));
        session.setAttribute(SessionService.ATTR_USER,manager); when(snapshots.refresh(session)).thenReturn(manager);
        mvc.perform(put("/api/admin/config/settings/1").session(session).contentType("application/json").content("{\"value\":\"40\"}"))
                .andExpect(status().isForbidden());
        verify(service,never()).updateSetting(anyInt(),anyString());
    }
    @Test void managementPermissionCannotConfigureAndDirectEndpointsCannotBypassChecks() throws Exception {
        var session = new MockHttpSession(); var admin = user("ADMIN", Set.of("voucher:manage"));
        session.setAttribute(SessionService.ATTR_USER, admin); when(snapshots.refresh(session)).thenReturn(admin);
        mvc.perform(get("/api/admin/config/settings/1").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/config/settings/category/VOUCHER").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/config/settings/1/reset").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/config/settings/batch").session(session).contentType("application/json")
                .content("{\"settings\":{\"1\":\"40\"}}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/config/membership/ranks").session(session)).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/config/membership/ranks/1/threshold").session(session).contentType("application/json")
                .content("{}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/config/membership/ranks/1/benefits").session(session).contentType("application/json")
                .content("{}")).andExpect(status().isForbidden());
        verify(service, never()).updateSettings(anyMap());
        verify(service, never()).resetToDefault(anyInt());
    }
    @Test void eachCategoryRequiresItsOwnPermission() throws Exception {
        var session = new MockHttpSession(); var admin = user("ADMIN", Set.of("system:configure"));
        session.setAttribute(SessionService.ATTR_USER, admin); when(snapshots.refresh(session)).thenReturn(admin);
        when(service.getSettingsByCategory("SYSTEM")).thenReturn(List.of());
        mvc.perform(get("/api/admin/config/settings/category/SYSTEM").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/config/settings/category/VOUCHER").session(session)).andExpect(status().isForbidden());
        when(snapshots.refresh(session)).thenReturn(user("ADMIN", Set.of("membership:configure")));
        when(service.getAllRanks()).thenReturn(List.of());
        mvc.perform(get("/api/admin/config/membership/ranks").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/config/settings/category/SYSTEM").session(session)).andExpect(status().isForbidden());
    }
    @Configuration @EnableWebMvc @Import(SecurityConfig.class)
    static class Config {
        @Bean AuthorizationSnapshotService snapshots() { return mock(AuthorizationSnapshotService.class); }
        @Bean SessionAuthenticationFilter filter(AuthorizationSnapshotService snapshots) { return new SessionAuthenticationFilter(snapshots); }
        @Bean SystemConfigService service() { return mock(SystemConfigService.class); }
        @Bean SystemConfigApiController controller(SystemConfigService service) {
            var controller = new SystemConfigApiController(); ReflectionTestUtils.setField(controller,"service",service); return controller;
        }
    }
}
