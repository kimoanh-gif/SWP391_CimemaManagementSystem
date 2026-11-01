package com.cinema.service;

import com.cinema.dao.CustomerDAO;
import com.cinema.dao.GenreDao;
import com.cinema.dao.MovieDao;
import com.cinema.dao.MovieReviewDAO;
import com.cinema.model.Customer;
import com.cinema.model.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieCatalogServiceTest {

    @Mock
    private MovieDao movieDAO;

    @Mock
    private GenreDao genreDAO;

    @Mock
    private MovieReviewDAO movieReviewDAO;

    @Mock
    private CustomerDAO customerDAO;

    private MovieCatalogService movieCatalogService;

    @BeforeEach
    void setUp() {
        movieCatalogService = new MovieCatalogService(movieDAO, genreDAO, movieReviewDAO, customerDAO);
    }

    @Test
    void testGetFeaturedMovies() {
        Movie movie = new Movie();
        movie.setMovieId(1);
        movie.setTitle("Test Movie");
        when(movieDAO.findFeatured(5)).thenReturn(List.of(movie));

        List<Movie> result = movieCatalogService.getFeaturedMovies(5);
        assertEquals(1, result.size());
        assertEquals("Test Movie", result.get(0).getTitle());
    }

    @Test
    void testGetMovieByIdFound() {
        Movie movie = new Movie();
        movie.setMovieId(10);
        movie.setTitle("Cyber Horizon");
        when(movieDAO.findById(10)).thenReturn(Optional.of(movie));
        when(genreDAO.findByMovieId(10)).thenReturn(List.of());

        Optional<Movie> result = movieCatalogService.getMovieById(10);
        assertTrue(result.isPresent());
        assertEquals("Cyber Horizon", result.get().getTitle());
    }

    @Test
    void moviesShowRealAverageRatingAndNoFakeFiveStars() {
        Movie reviewed = movie(1, "Reviewed", "T16", java.time.LocalDate.now().minusDays(5));
        Movie fresh = movie(2, "Fresh", "P", java.time.LocalDate.now().minusDays(1));
        when(movieDAO.findNowShowing(0)).thenReturn(List.of(reviewed, fresh));
        when(movieReviewDAO.findRatingSummaries()).thenReturn(java.util.Map.of(1, new double[]{4.3, 3}));

        List<Movie> result = movieCatalogService.getNowShowingMovies(0);
        assertEquals(4.3, result.get(0).getAverageRating());
        assertEquals(3, result.get(0).getReviewCount());
        assertEquals(0.0, result.get(1).getAverageRating());
        assertEquals(0, result.get(1).getReviewCount());
    }

    private static Movie movie(int id, String title, String ageRating, java.time.LocalDate releaseDate) {
        Movie m = new Movie();
        m.setMovieId(id);
        m.setTitle(title);
        m.setAgeRating(ageRating);
        m.setReleaseDate(releaseDate);
        return m;
    }

    @Test
    void testAddReviewSuccess() {
        Customer customer = new Customer();
        customer.setCustomerId(99);
        customer.setAccountId(1);

        when(customerDAO.findByAccountId(1)).thenReturn(Optional.of(customer));
        when(movieReviewDAO.findEligibleBookingId(1, 10, 77)).thenReturn(77);

        boolean ok = movieCatalogService.addReview(1, 10, 77, 5, "Great movie!");
        assertTrue(ok);
        verify(movieReviewDAO).saveOrUpdateReview(eq(99), eq(10), eq(77), eq(5), eq("Great movie!"));
    }

    @Test
    void testAddReviewFailsWhenCustomerNotFound() {
        when(customerDAO.findByAccountId(999)).thenReturn(Optional.empty());

        boolean ok = movieCatalogService.addReview(999, 10, 77, 5, "Nice");
        assertFalse(ok);
    }
}
