package com.cinema.controller.api;

import com.cinema.model.Seat;
import com.cinema.service.SeatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    @DisplayName("GET /api/manager/seats/room/1: Should return seat list")
    void testGetSeatsByRoom_Success() throws Exception {
        Seat s = new Seat(1, 1, "VIP", "B", 5, 2, 5, "Active");
        when(seatService.getSeatsByRoomId(1)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/manager/seats/room/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].seatTypeName").value("VIP"))
                .andExpect(jsonPath("$.data[0].rowLabel").value("B"));
    }

    @Test
    @DisplayName("PATCH /api/manager/seats/1: Should update seat attributes")
    void testUpdateSeat_Success() throws Exception {
        Seat s = new Seat(1, 1, "Couple", "C", 2, 3, 2, "Maintenance");
        when(seatService.updateSeat(anyInt(), anyString(), anyString())).thenReturn(s);

        mockMvc.perform(patch("/api/manager/seats/1")
                        .param("seatTypeName", "Couple")
                        .param("status", "Maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("Maintenance"));
    }
}
