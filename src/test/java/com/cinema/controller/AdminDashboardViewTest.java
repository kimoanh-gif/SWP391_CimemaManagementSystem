package com.cinema.controller;

import com.cinema.service.AdminDashboardService;
import com.cinema.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDashboardViewTest {
    private String render(Map<String,Object> variables) {
        MockServletContext servlet=new MockServletContext();
        MockHttpServletRequest request=new MockHttpServletRequest(servlet);
        request.setRequestURI("/admin/dashboard");
        var exchange=JakartaServletWebApplication.buildApplication(servlet)
                .buildExchange(request,new MockHttpServletResponse());
        FileTemplateResolver resolver=new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine=new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        variables.put("_csrf",Map.of("token","test-token"));
        return engine.process("admin/dashboard",new WebContext(exchange,Locale.ENGLISH,variables));
    }
    @Test void dashboardRendersRealValuesAndEmptyStates() throws Exception {
        Map<String,Object> d=new HashMap<>();
        for(String key:List.of("revenue","tickets","pending","activeCustomers","newCustomers","customers","staff","roles","locked","chartTotal"))d.put(key,0);
        d.put("period","day"); d.put("chartDescription","Last 7 days"); d.put("revenue",new BigDecimal("1234567"));
        d.put("today", LocalDate.of(2026,10,23));
        d.put("range", "quick");
        d.put("rangeStarts", Map.of("quick", LocalDate.of(2026,10,17),
                "last30", LocalDate.of(2026,9,24), "month", LocalDate.of(2026,10,1),
                "quarter", LocalDate.of(2026,10,1), "year", LocalDate.of(2026,1,1)));
        d.put("updatedAt",LocalDateTime.of(2026,10,23,18,0));
        d.put("chartEnd",LocalDate.of(2026,10,23));d.put("chartStart",LocalDate.of(2026,10,17));
        Map<String, Object> quickRanges = new HashMap<>();
        for (String period : List.of("day", "week", "month", "quarter", "year")) {
            quickRanges.put(period, Map.of("start", d.get("chartStart"), "end", d.get("chartEnd")));
        }
        d.put("quickRanges", quickRanges);
        d.put("chartBars",List.of(Map.of("label","23/10","start",LocalDate.of(2026,10,23),"end",LocalDate.of(2026,10,23),"height",BigDecimal.ZERO,"amount",BigDecimal.ZERO,"peak",false)));
        d.put("topMovies",List.of());d.put("recent",List.of());d.put("upcoming",List.of());
        String html=render(new HashMap<>(Map.of("dashboard",d)));
        assertTrue(html.contains("1.234.567"));
        assertTrue(html.contains("No paid movie bookings"));
        assertTrue(html.contains("No bookings yet"));
        assertTrue(html.contains("No more screenings"));
        assertTrue(html.contains("action=\"/logout\""));
        assertFalse(html.matches("(?s).*id=\"revenueFrom\"[^>]*disabled[^>]*>.*"));
        assertFalse(html.matches("(?s).*id=\"revenueTo\"[^>]*disabled[^>]*>.*"));
        assertFalse(html.contains("id=\"revenuePeriod\""));
        assertFalse(html.contains("id=\"revenueRange\""));
        html=render(new HashMap<>(Map.of("dashboard",d,"user",Map.of("fullName","Quản trị viên","role","ADMIN"))));
        assertTrue(html.contains(">Administrator</span>"));
        assertTrue(html.contains(">Administrator</div>"));
        assertFalse(html.contains(">Quản trị viên<"));
        html=render(new HashMap<>(Map.of("dashboard",d,"user",Map.of("fullName","Lê Thị Kim Oanh","role","ADMIN"))));
        assertTrue(html.contains(">Lê Thị Kim Oanh</span>"));
        // Render timestamp-bearing rows as well, and ensure titles are escaped.
        d.put("recent",List.of(Map.of("code","BK-1","title","<script>bad</script>","startTime",Timestamp.valueOf("2026-10-23 19:00:00"),"amount",BigDecimal.TEN,"status","CONFIRMED","channel","ONLINE")));
        d.put("upcoming",List.of(Map.of("title","Movie","room","Room 1","startTime",Timestamp.valueOf("2026-10-23 19:00:00"),"capacity",100,"reserved",12)));
        d.put("topMovies",List.of(Map.of("title","Movie","bookings",2,"amount",BigDecimal.TEN)));
        html=render(new HashMap<>(Map.of("dashboard",d)));
        assertTrue(html.contains("88 / 100 seats available"));
        assertFalse(html.contains("<script>bad</script>"));
        assertTrue(html.contains("BK-1"));
        d.put("range", "custom");
        String customHtml = render(new HashMap<>(Map.of("dashboard", d)));
        assertFalse(customHtml.matches("(?s).*id=\"revenueFrom\"[^>]*disabled[^>]*>.*"));
        assertFalse(customHtml.matches("(?s).*id=\"revenueTo\"[^>]*disabled[^>]*>.*"));
        assertFalse(customHtml.contains("name=\"range\""));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/admin-dashboard-test-preview.html"),
                html.replace("Database snapshot", "TEST PREVIEW - simulated values"));
    }
    @Test void dateRangeIsPassedToService() {
        AdminController controller = new AdminController();
        AdminDashboardService service = mock(AdminDashboardService.class);
        ReflectionTestUtils.setField(controller, "adminDashboardService", service);
        ReflectionTestUtils.setField(controller, "sessionService", mock(SessionService.class));
        var data = Map.<String, Object>of("period", "month");
        when(service.loadDateRange("2023-01-01", "2025-12-31")).thenReturn(data);
        var model = new ExtendedModelMap();
        assertEquals("admin/dashboard", controller.dashboard(model, new MockHttpSession(), "2023-01-01", "2025-12-31"));
        assertSame(data, model.get("dashboard"));
        verify(service).loadDateRange("2023-01-01", "2025-12-31");
        verifyNoMoreInteractions(service);
    }
    @Test void invalidDatesKeepInputAndShowErrorWithDefaultChart() {
        AdminController controller = new AdminController();
        AdminDashboardService service = mock(AdminDashboardService.class);
        ReflectionTestUtils.setField(controller, "adminDashboardService", service);
        ReflectionTestUtils.setField(controller, "sessionService", mock(SessionService.class));
        when(service.loadDateRange("2026-10-28", "2026-10-01"))
                .thenThrow(new IllegalArgumentException("Invalid date range"));
        var data = Map.<String, Object>of("period", "day");
        when(service.loadDateRange(null, null)).thenReturn(data);
        var model = new ExtendedModelMap();
        controller.dashboard(model, new MockHttpSession(), "2026-10-28", "2026-10-01");
        assertEquals("Invalid date range", model.get("revenueFilterError"));
        assertEquals("2026-10-28", model.get("filterFrom"));
        assertEquals("2026-10-01", model.get("filterTo"));
        assertSame(data, model.get("dashboard"));
    }
    @Test void controllerShowsFailureRatherThanInventedStatistics() {
        AdminController controller = new AdminController();
        AdminDashboardService service = mock(AdminDashboardService.class);
        ReflectionTestUtils.setField(controller, "adminDashboardService", service);
        ReflectionTestUtils.setField(controller, "sessionService", mock(SessionService.class));
        when(service.loadDateRange(null, null)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        var model = new ExtendedModelMap();
        assertEquals("admin/dashboard", controller.dashboard(model, new MockHttpSession(), null, null));
        assertTrue(model.containsAttribute("dashboardError"));
        assertFalse(model.containsAttribute("dashboard"));
        String html = render(new HashMap<>(model));
        assertTrue(html.contains("Unable to load dashboard data"));
        assertFalse(html.contains("Revenue today"));
    }
}