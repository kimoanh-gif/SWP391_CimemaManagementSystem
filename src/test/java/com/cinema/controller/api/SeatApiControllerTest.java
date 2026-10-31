package com.cinema.controller.api;

import com.cinema.model.Seat;
import com.cinema.service.SeatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeatApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeatApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeatService seatService;

    @Test
    @DisplayName("GET /api/manager/seats/room/1: Should return seat list for authenticated MANAGER")
    void testGetSeatsByRoom_Success() throws Exception {
        Seat s = new Seat(1, 1, "VIP", "B", 5, 2, 5, "Active");
        when(seatService.getSeatsByRoomId(1)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/manager/seats/room/1")
                        .sessionAttr("userRole", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].seatTypeName").value("VIP"))
                .andExpect(jsonPath("$.data[0].rowLabel").value("B"));
    }

    @Test
    @DisplayName("PATCH /api/manager/seats/1: Should update seat attributes for authenticated MANAGER")
    void testUpdateSeat_Success() throws Exception {
        Seat s = new Seat(1, 1, "Couple", "C", 2, 3, 2, "Maintenance");
        when(seatService.updateSeat(anyInt(), anyString(), anyString())).thenReturn(s);

        mockMvc.perform(patch("/api/manager/seats/1")
                        .sessionAttr("userRole", "MANAGER")
                        .param("seatTypeName", "Couple")
                        .param("status", "Maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("Maintenance"));
    }

    @Test
    @DisplayName("PATCH /api/manager/seats/1: Should return 400 when status is invalid or both params missing")
    void testUpdateSeat_InvalidInput_Returns400() throws Exception {
        mockMvc.perform(patch("/api/manager/seats/1")
                        .sessionAttr("userRole", "MANAGER")
                        .param("status", "BrokenStatus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(patch("/api/manager/seats/1")
                        .sessionAttr("userRole", "MANAGER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(seatService, never()).updateSeat(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("PUT /api/manager/seats/room/1/layout: Should validate SeatLayoutForm items and return 400 on invalid seatNumber")
    void testSaveLayout_ValidationAndSuccess() throws Exception {
        String invalidJson = "[{\"rowLabel\":\"\",\"seatNumber\":0,\"status\":\"Active\"}]";
        mockMvc.perform(put("/api/manager/seats/room/1/layout")
                        .sessionAttr("userRole", "MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        Seat saved = new Seat(1, 1, "Standard", "A", 1, 1, 1, "Active");
        when(seatService.saveRoomLayout(anyInt(), anyList(), isNull())).thenReturn(List.of(saved));

        String validJson = "[{\"rowLabel\":\"A\",\"seatNumber\":1,\"gridRow\":1,\"gridColumn\":1,\"seatTypeName\":\"Standard\",\"status\":\"Active\"}]";
        mockMvc.perform(put("/api/manager/seats/room/1/layout")
                        .sessionAttr("userRole", "MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rowLabel").value("A"));
    }

    @Test
    @DisplayName("P0-04: Unauthenticated request to /api/manager/seats/room/1 is rejected with 403 Forbidden")
    void testGetSeatsByRoom_Unauthenticated_Rejected() throws Exception {
        mockMvc.perform(get("/api/manager/seats/room/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P0-04: Forged X-User-Role header or ?role=MANAGER param does not grant Manager access")
    void testGetSeatsByRoom_ForgedHeaderOrParam_Rejected() throws Exception {
        mockMvc.perform(get("/api/manager/seats/room/1")
                        .header("X-User-Role", "MANAGER"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/manager/seats/room/1")
                        .param("role", "MANAGER"))
                .andExpect(status().isForbidden());
    }
}
