package com.cinema.controller.api;

import com.cinema.exception.DuplicateGenreException;
import com.cinema.exception.GenreAssociationException;
import com.cinema.model.Genre;
import com.cinema.model.GenreForm;
import com.cinema.service.GenreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GenreApiController.class)
class GenreApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GenreService genreService;

    @Test
    @DisplayName("GET /api/manager/genres: Should return list of genres")
    void testGetGenresApi() throws Exception {
        Genre genre = new Genre(1, "Drama", "Emotional stories", "ACTIVE");
        when(genreService.getAllGenres(any(), any())).thenReturn(List.of(genre));

        mockMvc.perform(get("/api/manager/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Drama"));
    }

    @Test
    @DisplayName("POST /api/manager/genres: Should return 201 Created on valid request")
    void testCreateGenreApi_Success() throws Exception {
        GenreForm form = new GenreForm(null, "Fantasy", "Magical worlds", "ACTIVE");
        Genre created = new Genre(5, "Fantasy", "Magical worlds", "ACTIVE");

        when(genreService.createGenre(any(GenreForm.class))).thenReturn(created);

        mockMvc.perform(post("/api/manager/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.genreId").value(5))
                .andExpect(jsonPath("$.data.name").value("Fantasy"));
    }

    @Test
    @DisplayName("POST /api/manager/genres: Should return 409 Conflict when name is duplicate")
    void testCreateGenreApi_DuplicateName_ReturnsConflict() throws Exception {
        GenreForm form = new GenreForm(null, "Action", "Duplicate", "ACTIVE");
        when(genreService.createGenre(any(GenreForm.class)))
                .thenThrow(new DuplicateGenreException("Thể loại 'Action' đã tồn tại trong hệ thống."));

        mockMvc.perform(post("/api/manager/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Thể loại 'Action' đã tồn tại trong hệ thống."));
    }

    @Test
    @DisplayName("POST /api/manager/genres: Should return 400 Bad Request when name is empty")
    void testCreateGenreApi_ValidationError() throws Exception {
        GenreForm form = new GenreForm(null, "", "Invalid", "ACTIVE");

        mockMvc.perform(post("/api/manager/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH /api/manager/genres/{id}/toggle-status: Should return 400 when genre has active movies")
    void testToggleStatusApi_ActiveMoviesConflict() throws Exception {
        when(genreService.toggleStatus(anyInt()))
                .thenThrow(new GenreAssociationException("Không thể hủy kích hoạt thể loại đang có phim đang chiếu."));

        mockMvc.perform(patch("/api/manager/genres/1/toggle-status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/manager/genres: Should return 403 Forbidden when caller is not Manager")
    void testUnauthorizedApiAccess() throws Exception {
        mockMvc.perform(get("/api/manager/genres")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
