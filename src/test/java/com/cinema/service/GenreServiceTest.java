package com.cinema.service;

import com.cinema.dao.GenreDao;
import com.cinema.exception.DuplicateGenreException;
import com.cinema.exception.GenreAssociationException;
import com.cinema.exception.GenreNotFoundException;
import com.cinema.model.Genre;
import com.cinema.model.GenreForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreDao genreDao;

    @InjectMocks
    private GenreService genreService;

    private Genre actionGenre;
    private Genre comedyGenre;

    @BeforeEach
    void setUp() {
        actionGenre = new Genre(1, "Action", "Exciting action sequences", "ACTIVE");
        comedyGenre = new Genre(2, "Comedy", "Funny moments", "ACTIVE");
    }

    @Test
    @DisplayName("getAllGenres: Should return all genres matching criteria")
    void testGetAllGenres() {
        when(genreDao.findAll("act", "ACTIVE")).thenReturn(List.of(actionGenre));

        List<Genre> result = genreService.getAllGenres("act", "ACTIVE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Action");
        verify(genreDao).findAll("act", "ACTIVE");
    }

    @Test
    @DisplayName("getGenreById: Should return genre when ID exists")
    void testGetGenreById_Success() {
        when(genreDao.findById(1)).thenReturn(Optional.of(actionGenre));

        Genre result = genreService.getGenreById(1);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Action");
    }

    @Test
    @DisplayName("getGenreById: Should throw GenreNotFoundException when ID does not exist")
    void testGetGenreById_NotFound() {
        when(genreDao.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.getGenreById(99))
                .isInstanceOf(GenreNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("createGenre: Should successfully create when name is unique")
    void testCreateGenre_Success() {
        GenreForm form = new GenreForm(null, "Horror", "Scary movie genre", "ACTIVE");

        when(genreDao.existsByNameIgnoreCase("Horror", null)).thenReturn(false);
        when(genreDao.insert(form)).thenReturn(3);
        when(genreDao.findById(3)).thenReturn(Optional.of(new Genre(3, "Horror", "Scary movie genre", "ACTIVE")));

        Genre result = genreService.createGenre(form);

        assertThat(result).isNotNull();
        assertThat(result.getGenreId()).isEqualTo(3);
        assertThat(result.getName()).isEqualTo("Horror");
        verify(genreDao).insert(form);
    }

    @Test
    @DisplayName("createGenre: Should throw DuplicateGenreException when genre name already exists")
    void testCreateGenre_DuplicateName() {
        GenreForm form = new GenreForm(null, "Action", "Another action", "ACTIVE");

        when(genreDao.existsByNameIgnoreCase("Action", null)).thenReturn(true);

        assertThatThrownBy(() -> genreService.createGenre(form))
                .isInstanceOf(DuplicateGenreException.class)
                .hasMessageContaining("Action");

        verify(genreDao, never()).insert(any());
    }

    @Test
    @DisplayName("updateGenre: Should update successfully when valid")
    void testUpdateGenre_Success() {
        GenreForm form = new GenreForm(1, "Action & Adventure", "Updated description", "ACTIVE");

        when(genreDao.findById(1)).thenReturn(Optional.of(actionGenre));
        when(genreDao.existsByNameIgnoreCase("Action & Adventure", 1)).thenReturn(false);
        when(genreDao.update(eq(1), any(GenreForm.class))).thenReturn(1);

        Genre updatedGenre = new Genre(1, "Action & Adventure", "Updated description", "ACTIVE");
        when(genreDao.findById(1)).thenReturn(Optional.of(actionGenre)).thenReturn(Optional.of(updatedGenre));

        Genre result = genreService.updateGenre(1, form);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Action & Adventure");
        verify(genreDao).update(eq(1), any(GenreForm.class));
    }

    @Test
    @DisplayName("updateGenre: Should throw DuplicateGenreException if new name conflicts with other genre")
    void testUpdateGenre_DuplicateName() {
        GenreForm form = new GenreForm(1, "Comedy", "Trying to rename Action to Comedy", "ACTIVE");

        when(genreDao.findById(1)).thenReturn(Optional.of(actionGenre));
        when(genreDao.existsByNameIgnoreCase("Comedy", 1)).thenReturn(true);

        assertThatThrownBy(() -> genreService.updateGenre(1, form))
                .isInstanceOf(DuplicateGenreException.class)
                .hasMessageContaining("Comedy");

        verify(genreDao, never()).update(anyInt(), any());
    }

    @Test
    @DisplayName("toggleStatus: Deactivating genre with active movies should throw GenreAssociationException")
    void testToggleStatus_WithActiveMovies_ThrowsException() {
        when(genreDao.findById(1)).thenReturn(Optional.of(actionGenre));
        when(genreDao.countActiveAssociatedMovies(1)).thenReturn(3);

        assertThatThrownBy(() -> genreService.toggleStatus(1))
                .isInstanceOf(GenreAssociationException.class)
                .hasMessageContaining("Action")
                .hasMessageContaining("3");

        verify(genreDao, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    @DisplayName("toggleStatus: Deactivating genre without active movies should succeed")
    void testToggleStatus_WithoutActiveMovies_Succeeds() {
        when(genreDao.findById(1)).thenReturn(Optional.of(actionGenre));
        when(genreDao.countActiveAssociatedMovies(1)).thenReturn(0);
        when(genreDao.updateStatus(1, "INACTIVE")).thenReturn(1);

        Genre inactiveAction = new Genre(1, "Action", "Exciting action sequences", "INACTIVE");
        when(genreDao.findById(1)).thenReturn(Optional.of(actionGenre)).thenReturn(Optional.of(inactiveAction));

        Genre result = genreService.toggleStatus(1);

        assertThat(result.getStatus()).isEqualTo("INACTIVE");
        verify(genreDao).updateStatus(1, "INACTIVE");
    }

    @Test
    @DisplayName("deleteGenre: Should throw GenreAssociationException if movies are linked")
    void testDeleteGenre_WithAssociatedMovies_ThrowsException() {
        when(genreDao.findById(1)).thenReturn(Optional.of(actionGenre));
        when(genreDao.countAssociatedMovies(1)).thenReturn(5);

        assertThatThrownBy(() -> genreService.deleteGenre(1))
                .isInstanceOf(GenreAssociationException.class)
                .hasMessageContaining("5");

        verify(genreDao, never()).delete(anyInt());
    }

    @Test
    @DisplayName("deleteGenre: Should delete if no movie associations exist")
    void testDeleteGenre_Success() {
        when(genreDao.findById(2)).thenReturn(Optional.of(comedyGenre));
        when(genreDao.countAssociatedMovies(2)).thenReturn(0);
        when(genreDao.delete(2)).thenReturn(1);

        genreService.deleteGenre(2);

        verify(genreDao).delete(2);
    }
}
