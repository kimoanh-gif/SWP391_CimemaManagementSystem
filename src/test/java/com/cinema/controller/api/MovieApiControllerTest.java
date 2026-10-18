package com.cinema.controller.api;

import com.cinema.exception.MovieHasActiveShowtimesException;
import com.cinema.exception.MovieNotFoundException;
import com.cinema.model.Movie;
import com.cinema.model.MovieForm;
import com.cinema.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovieApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieService movieService;

    @Test
    @DisplayName("GET /api/manager/movies: Should return movie list as JSON")
    void testGetMoviesApi_Success() throws Exception {
        Movie movie = new Movie();
        movie.setId(1);
        movie.setTitle("Interstellar");
        movie.setStatus("Now Showing");

        when(movieService.getAllMovies(any(), any(), anyString(), anyString())).thenReturn(List.of(movie));

        mockMvc.perform(get("/api/manager/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Interstellar"));
    }

    @Test
    @DisplayName("GET /api/manager/movies/{id}: Should return single movie by ID")
    void testGetMovieByIdApi_Success() throws Exception {
        Movie movie = new Movie();
        movie.setId(1);
        movie.setTitle("Interstellar");

        when(movieService.getMovieById(1)).thenReturn(movie);

        mockMvc.perform(get("/api/manager/movies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Interstellar"));
    }

    @Test
    @DisplayName("GET /api/manager/movies/{id}: Should return 404 when movie not found")
    void testGetMovieByIdApi_NotFound() throws Exception {
        when(movieService.getMovieById(99)).thenThrow(new MovieNotFoundException("Movie not found"));

        mockMvc.perform(get("/api/manager/movies/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/manager/movies: Should return 201 Created on valid input")
    void testCreateMovieApi_Success() throws Exception {
        MovieForm form = new MovieForm();
        form.setTitle("Spider-Man");
        form.setDuration(130);
        form.setAgeRating("T13");
        form.setStatus("Upcoming");

        Movie created = new Movie();
        created.setId(8);
        created.setTitle("Spider-Man");

        when(movieService.createMovie(any(MovieForm.class))).thenReturn(created);

        mockMvc.perform(post("/api/manager/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.title").value("Spider-Man"));
    }

    @Test
    @DisplayName("POST /api/manager/movies: Should return 400 Bad Request when validation fails")
    void testCreateMovieApi_ValidationError() throws Exception {
        MovieForm form = new MovieForm();
        form.setTitle(""); // Blank title

        mockMvc.perform(post("/api/manager/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/manager/movies/{id}: Should return 200 OK on valid update")
    void testUpdateMovieApi_Success() throws Exception {
        MovieForm form = new MovieForm();
        form.setTitle("Spider-Man 2");
        form.setDuration(135);
        form.setAgeRating("T13");
        form.setStatus("Now Showing");

        Movie updated = new Movie();
        updated.setId(8);
        updated.setTitle("Spider-Man 2");

        when(movieService.updateMovie(eq(8), any(MovieForm.class))).thenReturn(updated);

        mockMvc.perform(put("/api/manager/movies/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Spider-Man 2"));
    }

    @Test
    @DisplayName("PATCH /api/manager/movies/{id}/deactivate: Should soft deactivate movie (BR-37)")
    void testDeactivateMovieApi_Success() throws Exception {
        Movie movie = new Movie();
        movie.setId(8);
        movie.setStatus("Archived");

        when(movieService.deactivateMovie(8)).thenReturn(movie);

        mockMvc.perform(patch("/api/manager/movies/8/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("Archived"));
    }

    @Test
    @DisplayName("PATCH /api/manager/movies/{id}/deactivate: Should return 409 Conflict when active showtimes exist")
    void testDeactivateMovieApi_HasActiveShowtimes() throws Exception {
        when(movieService.deactivateMovie(8))
                .thenThrow(new MovieHasActiveShowtimesException("Đang có suất chiếu"));

        mockMvc.perform(patch("/api/manager/movies/8/deactivate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/manager/movies/available-for-showtimes: BR-07 returns available movies")
    void testGetAvailableMoviesForShowtimesApi() throws Exception {
        Movie movie = new Movie();
        movie.setId(1);
        movie.setTitle("Now Showing Movie");
        movie.setStatus("Now Showing");

        when(movieService.getAvailableMoviesForShowtimes()).thenReturn(List.of(movie));

        mockMvc.perform(get("/api/manager/movies/available-for-showtimes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("PATCH /api/manager/movies/{id}/reactivate: Should reactivate and return 200 OK")
    void testReactivateMovieApi_Success() throws Exception {
        Movie movie = new Movie();
        movie.setId(5);
        movie.setTitle("Test Movie");
        movie.setStatus("Now Showing");

        when(movieService.reactivateMovie(5)).thenReturn(movie);

        mockMvc.perform(patch("/api/manager/movies/5/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("Now Showing"));
    }

    @Test
    @DisplayName("GET /api/manager/movies: Customer role rejected with 403 Forbidden")
    void testApiAuthorization_ForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/manager/movies").param("role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }
}
