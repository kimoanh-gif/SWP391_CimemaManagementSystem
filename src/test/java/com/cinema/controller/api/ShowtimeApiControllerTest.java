package com.cinema.controller.api;

import com.cinema.model.Showtime;
import com.cinema.service.ShowtimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ShowtimeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShowtimeService showtimeService;

    @Test
    void testGetShowtimes_AuthorizedManager() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Showtime st = new Showtime(1, 1, 1, start, start.plusMinutes(120), new BigDecimal("85000"), "Scheduled");
        st.setMovieTitle("Avatar 2");
        st.setRoomName("Phòng 1");

        when(showtimeService.getAllShowtimes(any(), any(), any(), any())).thenReturn(List.of(st));

        mockMvc.perform(get("/api/manager/showtimes")
                        .header("X-User-Role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].movieTitle").value("Avatar 2"));
    }

    @Test
    void testGetShowtimes_ForbiddenNonManager() throws Exception {
        mockMvc.perform(get("/api/manager/showtimes")
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isForbidden());
    }
}
