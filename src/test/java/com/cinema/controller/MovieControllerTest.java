package com.cinema.controller;

import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.dto.MovieForm;
import com.cinema.service.GenreService;
import com.cinema.service.MovieService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
@AutoConfigureMockMvc(addFilters = false)
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

        mockMvc.perform(get("/manager/movies")
                        .sessionAttr("userRole", "MANAGER"))
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
        mockMvc.perform(get("/manager/movies").sessionAttr("userRole", "CUSTOMER"))
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
                        .sessionAttr("userRole", "MANAGER")
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
    @DisplayName("POST /manager/movies/save: Invalid input or endDate < releaseDate should redirect with error message")
    void testSaveMovie_ValidationFailure() throws Exception {
        mockMvc.perform(post("/manager/movies/save")
                        .sessionAttr("userRole", "MANAGER")
                        .param("title", "") // Blank title violates @NotBlank
                        .param("duration", "0")) // Invalid duration
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/movies"))
                .andExpect(flash().attributeExists("errorMessage"));

        mockMvc.perform(post("/manager/movies/save")
                        .sessionAttr("userRole", "MANAGER")
                        .param("title", "Test Movie")
                        .param("duration", "120")
                        .param("ageRating", "T13")
                        .param("status", "Upcoming")
                        .param("releaseDate", "2027-06-20")
                        .param("endDate", "2027-06-10")) // endDate < releaseDate violates @AssertTrue
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/movies"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(movieService, never()).createMovie(any(MovieForm.class));
    }

    @Test
    @DisplayName("POST /manager/movies/{id}/deactivate: Should soft deactivate and redirect with success")
    void testDeactivateMovie_Success() throws Exception {
        Movie movie = new Movie();
        movie.setId(5);
        movie.setTitle("Test Movie");
        movie.setStatus("Archived");

        when(movieService.deactivateMovie(5)).thenReturn(movie);

        mockMvc.perform(post("/manager/movies/5/deactivate")
                        .sessionAttr("userRole", "MANAGER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/movies"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(movieService).deactivateMovie(5);
    }

    @Test
    @DisplayName("POST /manager/movies/{id}/reactivate: Should reactivate and redirect with success")
    void testReactivateMovie_Success() throws Exception {
        Movie movie = new Movie();
        movie.setId(5);
        movie.setTitle("Test Movie");
        movie.setStatus("Now Showing");

        when(movieService.reactivateMovie(5)).thenReturn(movie);

        mockMvc.perform(post("/manager/movies/5/reactivate")
                        .sessionAttr("userRole", "MANAGER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/movies"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(movieService).reactivateMovie(5);
    }

    @Test
    @DisplayName("GET /manager/movies: Supports real pagination parameters (page & size) while keeping system KPI counts")
    void testViewMovieList_Pagination() throws Exception {
        Movie m1 = new Movie(1, "Movie 1", 120, "Now Showing");
        Movie m2 = new Movie(2, "Movie 2", 130, "Upcoming");
        Movie m3 = new Movie(3, "Movie 3", 140, "Archived");

        when(movieService.getAllMovies(any(), any(), anyString(), anyString())).thenReturn(List.of(m1, m2, m3));
        when(genreService.getAllGenres(any(), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/manager/movies")
                        .sessionAttr("userRole", "MANAGER")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/movies"))
                .andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("totalFilteredCount", 3))
                .andExpect(model().attribute("startItem", 3))
                .andExpect(model().attribute("endItem", 3))
                .andExpect(model().attribute("totalCount", 3L))
                .andExpect(model().attribute(" movies".trim(), List.of(m3)));
    }
}
