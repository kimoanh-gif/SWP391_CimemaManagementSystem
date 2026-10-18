package com.cinema.service;

import com.cinema.dao.MovieDao;
import com.cinema.exception.InvalidMovieStatusException;
import com.cinema.exception.MovieHasActiveShowtimesException;
import com.cinema.exception.MovieNotFoundException;
import com.cinema.model.Movie;
import com.cinema.model.MovieForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieDao movieDao;

    @InjectMocks
    private MovieService movieService;

    private Movie sampleMovie;
    private MovieForm sampleForm;

    @BeforeEach
    void setUp() {
        sampleMovie = new Movie();
        sampleMovie.setId(1);
        sampleMovie.setTitle("Dune: Part Two");
        sampleMovie.setDuration(166);
        sampleMovie.setStatus("Upcoming");
        sampleMovie.setAgeRating("T16");
        sampleMovie.setReleaseDate(LocalDate.now().plusDays(5));

        sampleForm = new MovieForm();
        sampleForm.setTitle("Dune: Part Two");
        sampleForm.setDuration(166);
        sampleForm.setStatus("Upcoming");
        sampleForm.setAgeRating("T16");
        sampleForm.setReleaseDate(LocalDate.now().plusDays(5));
        sampleForm.setGenreIds(List.of(1, 2));
    }

    @Test
    @DisplayName("getAllMovies: Should return matching movies based on search criteria")
    void testGetAllMovies() {
        when(movieDao.findAll("Dune", 1, "T16", "Upcoming")).thenReturn(List.of(sampleMovie));

        List<Movie> result = movieService.getAllMovies("Dune", 1, "T16", "Upcoming");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Dune: Part Two");
        verify(movieDao).findAll("Dune", 1, "T16", "Upcoming");
    }

    @Test
    @DisplayName("getMovieById: Should return movie when ID exists")
    void testGetMovieById_Success() {
        when(movieDao.findById(1)).thenReturn(Optional.of(sampleMovie));

        Movie result = movieService.getMovieById(1);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Dune: Part Two");
    }

    @Test
    @DisplayName("getMovieById: Should throw MovieNotFoundException when ID does not exist")
    void testGetMovieById_NotFound() {
        when(movieDao.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.getMovieById(99))
                .isInstanceOf(MovieNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("createMovie: Should successfully insert movie and return saved instance")
    void testCreateMovie_Success() {
        when(movieDao.insert(any(MovieForm.class))).thenReturn(1);
        when(movieDao.findById(1)).thenReturn(Optional.of(sampleMovie));

        Movie result = movieService.createMovie(sampleForm);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        verify(movieDao).insert(sampleForm);
    }

    @Test
    @DisplayName("createMovie: Should reject empty or blank title")
    void testCreateMovie_BlankTitle() {
        sampleForm.setTitle("   ");

        assertThatThrownBy(() -> movieService.createMovie(sampleForm))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tên phim");
    }

    @Test
    @DisplayName("createMovie: Should reject duration <= 0")
    void testCreateMovie_InvalidDuration() {
        sampleForm.setDuration(0);

        assertThatThrownBy(() -> movieService.createMovie(sampleForm))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Thời lượng");
    }

    @Test
    @DisplayName("createMovie: Should reject when endDate is before releaseDate")
    void testCreateMovie_InvalidDates() {
        sampleForm.setReleaseDate(LocalDate.of(2026, 5, 20));
        sampleForm.setEndDate(LocalDate.of(2026, 5, 10));

        assertThatThrownBy(() -> movieService.createMovie(sampleForm))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngày kết thúc");
    }

    @Test
    @DisplayName("updateMovie: Should successfully update movie information")
    void testUpdateMovie_Success() {
        when(movieDao.findById(1)).thenReturn(Optional.of(sampleMovie));

        sampleForm.setTitle("Dune: Part Two (IMAX)");
        sampleForm.setStatus("Now Showing");

        Movie updated = new Movie();
        updated.setId(1);
        updated.setTitle("Dune: Part Two (IMAX)");
        updated.setStatus("Now Showing");

        when(movieDao.findById(1)).thenReturn(Optional.of(sampleMovie), Optional.of(updated));

        Movie result = movieService.updateMovie(1, sampleForm);

        assertThat(result.getTitle()).isEqualTo("Dune: Part Two (IMAX)");
        verify(movieDao).update(eq(1), any(MovieForm.class));
    }

    @Test
    @DisplayName("updateMovie: Should reject invalid status transition when movie has active showtimes (BR-07)")
    void testUpdateMovie_ActiveShowtimesConflict() {
        sampleMovie.setStatus("Now Showing");
        when(movieDao.findById(1)).thenReturn(Optional.of(sampleMovie));
        when(movieDao.countUpcomingShowtimesByMovieId(1)).thenReturn(3);

        sampleForm.setStatus("Ended");

        assertThatThrownBy(() -> movieService.updateMovie(1, sampleForm))
                .isInstanceOf(MovieHasActiveShowtimesException.class)
                .hasMessageContaining("3 suất chiếu");

        verify(movieDao, never()).update(anyInt(), any(MovieForm.class));
    }

    @Test
    @DisplayName("deactivateMovie: Should soft-delete by setting status to Archived (BR-37)")
    void testDeactivateMovie_Success() {
        when(movieDao.findById(1)).thenReturn(Optional.of(sampleMovie));
        when(movieDao.countUpcomingShowtimesByMovieId(1)).thenReturn(0);

        Movie archived = new Movie();
        archived.setId(1);
        archived.setTitle(sampleMovie.getTitle());
        archived.setStatus("Archived");

        when(movieDao.findById(1)).thenReturn(Optional.of(sampleMovie), Optional.of(archived));

        Movie result = movieService.deactivateMovie(1);

        assertThat(result.getStatus()).isEqualTo("Archived");
        verify(movieDao).updateStatus(1, "Archived");
        // Verify no physical deletion call exists in DAO
    }

    @Test
    @DisplayName("deactivateMovie: Should reject deactivation if movie has active showtimes")
    void testDeactivateMovie_HasActiveShowtimes() {
        when(movieDao.findById(1)).thenReturn(Optional.of(sampleMovie));
        when(movieDao.countUpcomingShowtimesByMovieId(1)).thenReturn(5);

        assertThatThrownBy(() -> movieService.deactivateMovie(1))
                .isInstanceOf(MovieHasActiveShowtimesException.class)
                .hasMessageContaining("5 suất chiếu");

        verify(movieDao, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    @DisplayName("getAvailableMoviesForShowtimes: BR-07 should return only Upcoming and Now Showing movies")
    void testGetAvailableMoviesForShowtimes() {
        when(movieDao.findMoviesAvailableForShowtimes()).thenReturn(List.of(sampleMovie));

        List<Movie> result = movieService.getAvailableMoviesForShowtimes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActiveForShowtimes()).isTrue();
        verify(movieDao).findMoviesAvailableForShowtimes();
    }
}
