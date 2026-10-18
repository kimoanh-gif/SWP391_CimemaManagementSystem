package com.cinema.controller.api;

import com.cinema.model.SeatType;
import com.cinema.service.TicketPricingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeatTypeApiController.class)
class SeatTypeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketPricingService pricingService;

    @Test
    @DisplayName("GET /api/manager/seat-types: Should return 200 with seat types list")
    void testGetAllSeatTypes_Success() throws Exception {
        SeatType st = new SeatType(1, "VIP", "VIP Amber", "Central row", new BigDecimal("1.25"), "#F59E0B", "Active");
        when(pricingService.getAllSeatTypes(anyString())).thenReturn(List.of(st));

        mockMvc.perform(get("/api/manager/seat-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].typeCode").value("VIP"));
    }

    @Test
    @DisplayName("PUT /api/manager/seat-types/1/multiplier: Should update multiplier successfully")
    void testUpdateMultiplier_Success() throws Exception {
        SeatType st = new SeatType(1, "VIP", "VIP Amber", "Central row", new BigDecimal("1.30"), "#F59E0B", "Active");
        when(pricingService.updateSeatTypeMultiplier(anyInt(), any(BigDecimal.class))).thenReturn(st);

        mockMvc.perform(put("/api/manager/seat-types/1/multiplier")
                        .param("multiplier", "1.30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.priceMultiplier").value(1.30));
    }
}
