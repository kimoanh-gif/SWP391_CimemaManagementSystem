package com.cinema.controller;

import com.cinema.model.SeatType;
import com.cinema.model.SeatTypeForm;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeatTypeController.class)
class SeatTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketPricingService pricingService;

    @Test
    @DisplayName("GET /manager/seat-types: Should return seat-types template with data")
    void testListSeatTypes_Success() throws Exception {
        SeatType st = new SeatType(1, "STD", "Standard", "Standard seat", new BigDecimal("1.00"), "#3B82F6", "Active");
        when(pricingService.getAllSeatTypes(anyString())).thenReturn(List.of(st));

        mockMvc.perform(get("/manager/seat-types"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/seat-types"))
                .andExpect(model().attributeExists("seatTypes"))
                .andExpect(model().attributeExists("totalCount"))
                .andExpect(model().attributeExists("activeCount"));
    }

    @Test
    @DisplayName("GET /manager/seat-types: Non-manager role should be rejected with 403 Forbidden")
    void testListSeatTypes_Forbidden() throws Exception {
        mockMvc.perform(get("/manager/seat-types").param("role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /manager/seat-types/multiplier: Should update multiplier and redirect")
    void testUpdateMultiplier_Success() throws Exception {
        SeatType st = new SeatType(1, "STD", "Standard", "Standard seat", new BigDecimal("1.10"), "#3B82F6", "Active");
        when(pricingService.updateSeatTypeMultiplier(anyInt(), any(BigDecimal.class))).thenReturn(st);

        mockMvc.perform(post("/manager/seat-types/multiplier")
                        .param("seatTypeId", "1")
                        .param("multiplier", "1.10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/seat-types"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
