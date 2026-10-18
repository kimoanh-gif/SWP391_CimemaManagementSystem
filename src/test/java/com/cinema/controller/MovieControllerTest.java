package com.cinema.controller;

import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.model.MovieForm;
import com.cinema.service.GenreService;
import com.cinema.service.MovieService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private GenreService genreService;

    @Test
    @DisplayName("GET /manager/movies: Should load movie management view with manager role")
    void testViewMovieList_Success() throws Exception {
        Movie movie = new Movie();
        movie.setId(1);
        movie.setTitle("Avatar: The Way of Water");
        movie.setStatus("Now Showing");
        movie.setDuration(192);

        when(movieService.getAllMovies(any(), any(), anyString(), anyString())).thenReturn(List.of(movie));
        when(genreService.getAllGenres(any(), anyString())).thenReturn(List.of(new Genre(1, "Sci-Fi", "Desc", "ACTIVE")));

        mockMvc.perform(get("/manager/movies"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/movies"))
                .andExpect(model().attributeExists("movies"))
                .andExpect(model().attributeExists("allGenres"))
                .andExpect(model().attributeExists("totalCount"))
                .andExpect(model().attributeExists("nowShowingCount"))
                .andExpect(model().attributeExists("upcomingCount"))
                .andExpect(model().attributeExists("archivedCount"));
    }

    @Test
    @DisplayName("GET /manager/movies: Non-Manager role should be rejected with 403 Forbidden")
    void testViewMovieList_ForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/manager/movies").param("role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /manager/movies/save: Valid movie form submission redirects with success")
    void testSaveMovie_Success() throws Exception {
        Movie created = new Movie();
        created.setId(10);
        created.setTitle("Oppenheimer");

        when(movieService.createMovie(any(MovieForm.class))).thenReturn(created);

        mockMvc.perform(post("/manager/movies/save")
                        .param("title", "Oppenheimer")
                        .param("duration", "180")
                        .param("ageRating", "T18")
                        .param("status", "Upcoming"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/movies"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(movieService).createMovie(any(MovieForm.class));
    }

    @Test
    @DisplayName("POST /manager/movies/save: Invalid input should redirect with error message")
    void testSaveMovie_ValidationFailure() throws Exception {
        mockMvc.perform(post("/manager/movies/save")
                        .param("title", "") // Blank title violates @NotBlank
                        .param("duration", "0")) // Invalid duration
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/movies"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("POST /manager/movies/{id}/deactivate: Should soft deactivate and redirect with success")
    void testDeactivateMovie_Success() throws Exception {
        Movie movie = new Movie();
        movie.setId(5);
        movie.setTitle("Test Movie");
        movie.setStatus("Archived");

        when(movieService.deactivateMovie(5)).thenReturn(movie);

        mockMvc.perform(post("/manager/movies/5/deactivate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/movies"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(movieService).deactivateMovie(5);
    }
}
