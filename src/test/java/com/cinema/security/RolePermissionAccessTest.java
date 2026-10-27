package com.cinema.security;
import com.cinema.config.SecurityConfig;
import com.cinema.model.AuthenticatedUser;
import com.cinema.service.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class RolePermissionAccessTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    AuthorizationSnapshotService snapshots;
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext()); context.register(Config.class); context.refresh();
        snapshots = context.getBean(AuthorizationSnapshotService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }
    @AfterEach void close() { context.close(); }
    MockHttpSession login(String role, String... permissions) {
        var user = new AuthenticatedUser(1, "user", "u@example.com", "User", null, role, Set.of(permissions), true, "PASSWORD");
        var session = new MockHttpSession(); session.setAttribute(SessionService.ATTR_USER, user);
        when(snapshots.refresh(session)).thenReturn(user); return session;
    }
    @Test void customerCannotEnterAdminEvenWithForgedGrant() throws Exception {
        var session = login("CUSTOMER", "account:manage");
        mvc.perform(get("/admin/customers").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/customers").session(session)).andExpect(status().isForbidden());
    }
    @Test void bookingPermissionControlsGetAndPostWithoutBlockingPersonalFunctions() throws Exception {
        var session = login("CUSTOMER", "movie:read");
        mvc.perform(get("/customer/booking").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/customer/profile").session(session)).andExpect(status().isOk());
        mvc.perform(get("/customer/booking/history").session(session)).andExpect(status().isOk());
        session = login("CUSTOMER", "movie:read", "booking:create");
        var response = mvc.perform(get("/customer/booking").session(session)).andExpect(status().isOk()).andReturn();
        var csrf = (org.springframework.security.web.csrf.CsrfToken) response.getRequest().getAttribute("_csrf");
        String token = csrf.getToken();
        mvc.perform(post("/customer/booking/checkout").session(session).cookie(response.getResponse().getCookie("XSRF-TOKEN")).param("_csrf", token)).andExpect(status().isOk());
        when(snapshots.refresh(session)).thenReturn(new AuthenticatedUser(1, "user", "u@example.com", "User", null,
                "CUSTOMER", Set.of("movie:read"), true, "PASSWORD"));
        mvc.perform(post("/customer/booking/checkout").session(session).cookie(response.getResponse().getCookie("XSRF-TOKEN")).param("_csrf", token)).andExpect(status().isForbidden());
    }
    @Test void internalPermissionsAndRolesAreBothRequired() throws Exception {
        mvc.perform(get("/manager/showtimes").session(login("MANAGER", "report:view"))).andExpect(status().isForbidden());
        mvc.perform(get("/manager/showtimes").session(login("MANAGER", "showtime:manage"))).andExpect(status().isOk());
        mvc.perform(get("/api/manager/showtimes").session(login("MANAGER", "showtime:manage"))).andExpect(status().isOk());
        mvc.perform(get("/staff/check-in").session(login("CUSTOMER", "ticket:checkin"))).andExpect(status().isForbidden());
        mvc.perform(get("/staff/check-in").session(login("STAFF", "ticket:checkin"))).andExpect(status().isOk());
        mvc.perform(get("/staff/sales").session(login("STAFF"))).andExpect(status().isForbidden());
        mvc.perform(get("/staff/sales").session(login("STAFF", "booking:create"))).andExpect(status().isOk());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "movie:manage,/manager/movies,MANAGER", "movie:manage,/api/manager/movies,MANAGER",
        "genre:manage,/manager/genres,MANAGER", "genre:manage,/api/manager/genres,MANAGER",
        "room:manage,/manager/rooms,MANAGER", "room:manage,/api/manager/rooms,MANAGER",
        "seat:manage,/manager/seats,MANAGER", "seat:manage,/api/manager/seats,MANAGER",
        "seat:manage,/manager/seat-types,MANAGER", "seat:manage,/api/manager/seat-types,MANAGER",
        "pricing:manage,/manager/pricing,MANAGER", "pricing:manage,/api/manager/pricing,MANAGER",
        "fb:manage,/manager/fb,MANAGER", "fb:manage,/api/manager/fb,MANAGER",
        "voucher:manage,/manager/vouchers,MANAGER", "promotion:manage,/manager/promotions,MANAGER",
        "news:approve,/manager/news,MANAGER", "news:manage,/staff/news,STAFF",
        "news:manage,/api/staff/news,STAFF", "booking:read,/staff/bookings,STAFF"
    })
    void everyDetailedPermissionControlsItsPageOrApi(String permission, String path, String role) throws Exception {
        mvc.perform(get(path).session(login(role))).andExpect(status().isForbidden());
        mvc.perform(get(path).session(login(role, permission))).andExpect(status().isOk());
        mvc.perform(get(path).session(login("CUSTOMER", permission))).andExpect(status().isForbidden());
        mvc.perform(get(path).session(login("ADMIN", permission))).andExpect(status().isOk());
        if (path.startsWith("/api/")) {
            mvc.perform(post(path).session(login(role))).andExpect(status().isForbidden());
            mvc.perform(post(path).session(login(role, permission))).andExpect(status().isOk());
        }
    }
    @Test void writerCannotApproveNewsAndApproverCannotWriteStaffDrafts() throws Exception {
        mvc.perform(get("/manager/news").session(login("STAFF", "news:manage", "news:approve"))).andExpect(status().isForbidden());
        mvc.perform(get("/staff/news").session(login("MANAGER", "news:approve"))).andExpect(status().isForbidden());
    }
    @Test void noSessionDoesNotBecomeManagerFromHeaderOrDefault() throws Exception {
        mvc.perform(get("/manager/showtimes").header("X-User-Role", "MANAGER")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/movies")).andExpect(status().isOk());
    }
    @Configuration @EnableWebMvc @Import(SecurityConfig.class)
    static class Config {
        @Bean AuthorizationSnapshotService snapshots() { return mock(AuthorizationSnapshotService.class); }
        @Bean SessionAuthenticationFilter filter(AuthorizationSnapshotService snapshots) { return new SessionAuthenticationFilter(snapshots); }
        @Bean Endpoints endpoints() { return new Endpoints(); }
    }
    @RestController static class Endpoints {
        @RequestMapping({"/admin/customers", "/api/admin/customers", "/customer/booking", "/customer/profile",
                "/customer/booking/history", "/customer/booking/checkout", "/manager/showtimes", "/api/manager/showtimes",
                "/staff/check-in", "/staff/sales", "/movies", "/staff/news", "/staff/bookings", "/api/staff/news",
                "/manager/{module}", "/api/manager/{module}"})
        String ok() { return "ok"; }
    }
}
