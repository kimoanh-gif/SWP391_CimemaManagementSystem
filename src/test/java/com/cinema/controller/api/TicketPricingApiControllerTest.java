package com.cinema.controller.api;

import com.cinema.model.SeatType;
import com.cinema.model.TicketPricing;
import com.cinema.service.TicketPricingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class TicketPricingApiControllerTest extends com.cinema.support.AuthenticatedMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketPricingService pricingService;

    @Test
    void testGetPricingData_Success() throws Exception {
        TicketPricing tp = new TicketPricing(1, "PRICE-WD-STD", "Ngày thường tiêu chuẩn",
                "WEEKDAY", "STANDARD", new BigDecimal("85000"), BigDecimal.ZERO,
                "2D Digital", "Active", "Mô tả");

        SeatType st = new SeatType(1, "STD", "Standard", "Standard",
                BigDecimal.ONE, "#3B82F6", "Active");

        when(pricingService.getAllPricingRules(any(), any())).thenReturn(List.of(tp));
        when(pricingService.getAllSeatTypes("Active")).thenReturn(List.of(st));
        when(pricingService.generatePricingMatrix()).thenReturn(Map.of(1, Map.of(1, new BigDecimal("85000"))));
        when(pricingService.getAverageTicketPrice()).thenReturn(new BigDecimal("85000"));

        mockMvc.perform(get("/api/manager/pricing")
                        .session(authSession("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pricingRules[0].pricingCode").value("PRICE-WD-STD"))
                .andExpect(jsonPath("$.data.averageTicketPrice").value(85000));
    }

    @Test
    void testCalculatePrice_BR50() throws Exception {
        when(pricingService.calculateTicketUnitPrice(eq(new BigDecimal("85000")), eq(new BigDecimal("1.25"))))
                .thenReturn(new BigDecimal("106000"));

        mockMvc.perform(get("/api/manager/pricing/calculate")
                        .session(authSession("MANAGER"))
                        .param("basePrice", "85000")
                        .param("multiplier", "1.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.calculatedPrice").value(106000));
    }

    @Test
    void testUnauthorizedRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/manager/pricing")
                        .session(authSession("CUSTOMER")))
                .andExpect(status().isForbidden());
    }
}
