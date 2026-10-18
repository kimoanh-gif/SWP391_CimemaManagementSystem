package com.cinema.controller;

import com.cinema.model.Room;
import com.cinema.model.Seat;
import com.cinema.service.RoomService;
import com.cinema.service.SeatService;
import com.cinema.service.TicketPricingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeatController.class)
class SeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeatService seatService;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private TicketPricingService pricingService;

    @Test
    @DisplayName("GET /manager/seats: Should render layout template with model attributes")
    void testViewSeatLayout_Success() throws Exception {
        Room r = new Room();
        r.setRoomId(1);
        r.setRoomCode("ROOM-01");
        r.setRoomName("Phòng 01");
        r.setCapacity(96);

        Seat s = new Seat(1, 1, "Standard", "A", 1, 1, 1, "Active");

        when(roomService.getAll(any(), any())).thenReturn(List.of(r));
        when(seatService.getSeatsByRoomId(1)).thenReturn(List.of(s));
        when(pricingService.getAllSeatTypes(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/seats?roomId=1"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/seats"))
                .andExpect(model().attributeExists("rooms"))
                .andExpect(model().attributeExists("selectedRoom"))
                .andExpect(model().attributeExists("seats"))
                .andExpect(model().attributeExists("totalSeats"));
    }

    @Test
    @DisplayName("GET /manager/seats: Non-manager role should be rejected with 403 Forbidden")
    void testViewSeatLayout_Forbidden() throws Exception {
        mockMvc.perform(get("/manager/seats").param("role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /manager/seats/update-seat: Should update and redirect with success")
    void testUpdateSeat_Success() throws Exception {
        Seat s = new Seat(1, 1, "VIP", "A", 1, 1, 1, "Active");
        when(seatService.updateSeat(anyInt(), anyString(), anyString())).thenReturn(s);

        mockMvc.perform(post("/manager/seats/update-seat")
                        .param("seatId", "1")
                        .param("roomId", "1")
                        .param("seatTypeName", "VIP")
                        .param("status", "Active"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/seats?roomId=1"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
