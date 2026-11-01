package com.cinema.controller;
import com.cinema.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SystemConfigRankApiTest {
    @org.junit.jupiter.api.BeforeEach void configurationPermissions() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin", "",
                java.util.stream.Stream.of("ROLE_ADMIN", "voucher:configure", "membership:configure", "system:configure")
                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new).toList()));
    }
    @org.junit.jupiter.api.AfterEach void clearConfigurationPermissions() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test void rejectsMissingNullFractionalStringsNegativeAndOverflowBeforeService() throws Exception {
        var service=mock(SystemConfigService.class);var controller=new SystemConfigApiController();
        ReflectionTestUtils.setField(controller,"service",service);var mapper=new ObjectMapper();
        for(String json:new String[]{"{}","{\"minPoints\":null}","{\"minPoints\":1.5}","{\"minPoints\":\"500\"}","{\"minPoints\":-1}","{\"minPoints\":2147483648}"}) {
            assertEquals(400,controller.updateRankThreshold(2,mapper.readTree(json)).getStatusCode().value());
        }
        verifyNoInteractions(service);
    }
    @Test void rejectsMissingOrMalformedMaximumBeforeService() throws Exception {
        var service = mock(SystemConfigService.class);
        var controller = new SystemConfigApiController();
        ReflectionTestUtils.setField(controller, "service", service);
        var mapper = new ObjectMapper();
        for (String json : new String[]{"null", "[]", "{\"minPoints\":500}",
                "{\"minPoints\":500,\"maxPoints\":-1}", "{\"minPoints\":500,\"maxPoints\":1.5}",
                "{\"minPoints\":500,\"maxPoints\":\"999\"}", "{\"minPoints\":500,\"maxPoints\":true}",
                "{\"minPoints\":500,\"maxPoints\":2147483648}"}) {
            assertEquals(400, controller.updateRankThreshold(2, mapper.readTree(json)).getStatusCode().value(), json);
        }
        verifyNoInteractions(service);
    }
    @Test void rejectsMalformedDiscountPolicyBeforeService() throws Exception {
        var service = mock(SystemConfigService.class);
        var controller = new SystemConfigApiController();
        ReflectionTestUtils.setField(controller, "service", service);
        var mapper = new ObjectMapper();
        String base = "{\"minPoints\":500,\"maxPoints\":1800,\"discountPercent\":10,\"discountUsageLimit\":3,\"minBookingAmount\":100000,\"maxDiscountAmount\":50000}";
        for (String field : new String[]{"discountPercent", "discountUsageLimit", "minBookingAmount", "maxDiscountAmount"}) {
            for (String value : new String[]{"null", "\"10\"", "true", "-1", "1e309", "1000000000000"}) {
                var body = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(base);
                body.set(field, mapper.readTree(value));
                assertEquals(400, controller.updateRankThreshold(2, body).getStatusCode().value(), field + value);
            }
            var missing = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(base);
            missing.remove(field);
            assertEquals(400, controller.updateRankThreshold(2, missing).getStatusCode().value());
        }
        for (String field : new String[]{"discountUsageLimit", "minBookingAmount", "maxDiscountAmount"}) {
            var body = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(base);
            body.put(field, 1.5);
            assertEquals(400, controller.updateRankThreshold(2, body).getStatusCode().value());
        }
        for (double percent : new double[]{100.01, 0.001}) {
            var body = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(base);
            body.put("discountPercent", percent);
            assertEquals(400, controller.updateRankThreshold(2, body).getStatusCode().value());
        }
        verifyNoInteractions(service);
    }

    @Test void acceptsBothPointBoundsAndExplicitUnlimited() throws Exception {
        var service = mock(SystemConfigService.class);
        var controller = new SystemConfigApiController();
        ReflectionTestUtils.setField(controller, "service", service);
        var mapper = new ObjectMapper();
        when(service.updateRankPolicy(2, 500, 1800, java.math.BigDecimal.TEN, 3, new java.math.BigDecimal("100000"), new java.math.BigDecimal("50000")))
                .thenReturn(SystemConfigService.OperationResult.success("Saved", "GOLD", "500-1999", "500-1800"));
        when(service.updateRankPolicy(3, 2000, null, java.math.BigDecimal.TEN, 3, new java.math.BigDecimal("100000"), new java.math.BigDecimal("50000")))
                .thenReturn(SystemConfigService.OperationResult.success("Saved", "PLATINUM", "2000-3000", "2000-null"));
        assertEquals(200, controller.updateRankThreshold(2, mapper.readTree("{\"minPoints\":500,\"maxPoints\":1800,\"discountPercent\":10,\"discountUsageLimit\":3,\"minBookingAmount\":100000,\"maxDiscountAmount\":50000}")).getStatusCode().value());
        assertEquals(200, controller.updateRankThreshold(3, mapper.readTree("{\"minPoints\":2000,\"maxPoints\":null,\"discountPercent\":10,\"discountUsageLimit\":3,\"minBookingAmount\":100000,\"maxDiscountAmount\":50000}")).getStatusCode().value());
        verify(service).updateRankPolicy(2, 500, 1800, java.math.BigDecimal.TEN, 3, new java.math.BigDecimal("100000"), new java.math.BigDecimal("50000"));
        verify(service).updateRankPolicy(3, 2000, null, java.math.BigDecimal.TEN, 3, new java.math.BigDecimal("100000"), new java.math.BigDecimal("50000"));
    }
}
