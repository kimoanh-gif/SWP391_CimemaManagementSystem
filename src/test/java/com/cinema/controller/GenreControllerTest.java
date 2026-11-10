package com.cinema.controller;

import com.cinema.model.Genre;
import com.cinema.dto.GenreForm;
import com.cinema.service.GenreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GenreController.class)
@AutoConfigureMockMvc(addFilters = false)
class GenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenreService genreService;

    @Test
    @DisplayName("GET /manager/genres: Should load genre list page with manager role")
    void testViewGenreList_Success() throws Exception {
        Genre genre = new Genre(1, "Sci-Fi", "Science fiction", "ACTIVE");
        genre.setMovieCount(3);
        when(genreService.getAllGenres(any(), anyString())).thenReturn(List.of(genre));

        mockMvc.perform(get("/manager/genres")
                        .sessionAttr("userRole", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/genres"))
                .andExpect(model().attributeExists("genres"))
                .andExpect(model().attributeExists("totalCount"))
                .andExpect(model().attributeExists("activeCount"));
    }

    @Test
    @DisplayName("GET /manager/genres: Non-Manager role should be denied with 403 Forbidden")
    void testViewGenreList_Unauthorized() throws Exception {
        mockMvc.perform(get("/manager/genres").sessionAttr("userRole", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /manager/genres/save: Valid creation should redirect with success")
    void testSaveGenre_Success() throws Exception {
        Genre created = new Genre(10, "Thriller", "Suspenseful stories", "ACTIVE");
        when(genreService.createGenre(any(GenreForm.class))).thenReturn(created);

        mockMvc.perform(post("/manager/genres/save")
                        .sessionAttr("userRole", "MANAGER")
                        .param("name", "Thriller")
                        .param("description", "Suspenseful stories")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/genres"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(genreService).createGenre(any(GenreForm.class));
    }

    @Test
    @DisplayName("POST /manager/genres/save: Blank name should fail validation and redirect with error")
    void testSaveGenre_InvalidForm_RedirectsWithError() throws Exception {
        mockMvc.perform(post("/manager/genres/save")
                        .sessionAttr("userRole", "MANAGER")
                        .param("name", "") // Blank
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/genres"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /manager/genres: Supports real pagination parameters (page & size) while keeping KPI counts")
    void testViewGenreList_Pagination() throws Exception {
        Genre g1 = new Genre(1, "Action", "Desc 1", "ACTIVE");
        Genre g2 = new Genre(2, "Comedy", "Desc 2", "ACTIVE");
        Genre g3 = new Genre(3, "Drama", "Desc 3", "INACTIVE");
        when(genreService.getAllGenres(any(), anyString())).thenReturn(List.of(g1, g2, g3));

        mockMvc.perform(get("/manager/genres")
                        .sessionAttr("userRole", "MANAGER")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/genres"))
                .andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("totalFilteredCount", 3))
                .andExpect(model().attribute("startItem", 3))
                .andExpect(model().attribute("endItem", 3))
                .andExpect(model().attribute("totalCount", 3L))
                .andExpect(model().attribute("activeCount", 2L))
                .andExpect(model().attribute("genres", List.of(g3)));
    }
}
