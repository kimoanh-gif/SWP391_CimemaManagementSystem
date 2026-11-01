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

/** Reproduces current role-only protection with the real controller and security chain. */
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
    @Test void revokingVoucherPermissionStillAllowsAdminToReadAndSaveVoucherConfiguration() throws Exception {
        var session = new MockHttpSession(); var admin = user("ADMIN",Set.of("voucher:manage"));
        session.setAttribute(SessionService.ATTR_USER,admin); when(snapshots.refresh(session)).thenReturn(admin);
        mvc.perform(put("/api/admin/config/settings/1").session(session).contentType("application/json").content("{\"value\":\"40\"}"))
                .andExpect(status().isOk());
        // Same session, refreshed from the database with the voucher permission removed.
        when(snapshots.refresh(session)).thenReturn(user("ADMIN",Set.of()));
        mvc.perform(get("/api/admin/config/settings").session(session)).andExpect(status().isOk());
        mvc.perform(put("/api/admin/config/settings/1").session(session).contentType("application/json").content("{\"value\":\"40\"}"))
                .andExpect(status().isOk());
        verify(service,times(2)).updateSetting(1,"40");
    }
    @Test void voucherPermissionDoesNotAllowManagerIntoAdminConfiguration() throws Exception {
        var session = new MockHttpSession(); var manager = user("MANAGER",Set.of("voucher:manage"));
        session.setAttribute(SessionService.ATTR_USER,manager); when(snapshots.refresh(session)).thenReturn(manager);
        mvc.perform(put("/api/admin/config/settings/1").session(session).contentType("application/json").content("{\"value\":\"40\"}"))
                .andExpect(status().isForbidden());
        verify(service,never()).updateSetting(anyInt(),anyString());
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
