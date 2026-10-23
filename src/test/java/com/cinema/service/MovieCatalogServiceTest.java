package com.cinema.service;

import com.cinema.dao.CustomerDAO;
import com.cinema.dao.GenreDao;
import com.cinema.dao.MovieReviewDAO;
import com.cinema.dao.PublicMovieDAO;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieCatalogServiceTest {

    @Mock
    private PublicMovieDAO publicMovieDAO;

    @Mock
    private GenreDao genreDAO;

    @Mock
    private MovieReviewDAO movieReviewDAO;

    @Mock
    private CustomerDAO customerDAO;

    private MovieCatalogService movieCatalogService;

    @BeforeEach
    void setUp() {
        movieCatalogService = new MovieCatalogService(publicMovieDAO, genreDAO, movieReviewDAO, customerDAO);
    }

    @Test
    void testGetFeaturedMovies() {
        Movie movie = new Movie();
        movie.setMovieId(1);
        movie.setTitle("Test Movie");
        when(publicMovieDAO.findFeatured(5)).thenReturn(List.of(movie));

        List<Movie> result = movieCatalogService.getFeaturedMovies(5);
        assertEquals(1, result.size());
        assertEquals("Test Movie", result.get(0).getTitle());
    }

    @Test
    void testGetPublicMovieByIdFound() {
        Movie movie = new Movie();
        movie.setMovieId(10);
        movie.setTitle("Cyber Horizon");
        when(publicMovieDAO.findPublicById(10)).thenReturn(Optional.of(movie));

        Optional<Movie> result = movieCatalogService.getPublicMovieById(10);
        assertTrue(result.isPresent());
        assertEquals("Cyber Horizon", result.get().getTitle());
    }

    @Test
    void testGetPublicMovieByInvalidIdSkipsDatabase() {
        assertTrue(movieCatalogService.getPublicMovieById(0).isEmpty());
        verify(publicMovieDAO, never()).findPublicById(anyInt());
    }

    @Test
    void testSearchMoviesNormalisesInvalidFilters() {
        when(publicMovieDAO.search("x", null, null, "all", "newest")).thenReturn(List.of());

        movieCatalogService.searchMovies("x", null, "X99", "unknown", "drop table");

        verify(publicMovieDAO).search("x", null, null, "all", "newest");
    }

    @Test
    void testNormalizeAgeRating() {
        assertEquals("T13", movieCatalogService.normalizeAgeRating(" t13 "));
        assertNull(movieCatalogService.normalizeAgeRating("R"));
        assertNull(movieCatalogService.normalizeAgeRating(""));
    }

    @Test
    void testAddReviewSuccess() {
        Customer customer = new Customer();
        customer.setCustomerId(99);
        customer.setAccountId(1);

        when(customerDAO.findByAccountId(1)).thenReturn(Optional.of(customer));
        when(publicMovieDAO.findPublicById(10)).thenReturn(Optional.of(new Movie()));

        boolean ok = movieCatalogService.addReview(1, 10, 5, "Great movie!");
        assertTrue(ok);
        verify(movieReviewDAO).saveOrUpdateReview(eq(99), eq(10), eq(5), eq("Great movie!"));
    }

    @Test
    void testAddReviewFailsForNonPublicMovie() {
        Customer customer = new Customer();
        customer.setCustomerId(99);
        when(customerDAO.findByAccountId(1)).thenReturn(Optional.of(customer));
        when(publicMovieDAO.findPublicById(10)).thenReturn(Optional.empty());

        assertFalse(movieCatalogService.addReview(1, 10, 5, "Nice"));
        verify(movieReviewDAO, never()).saveOrUpdateReview(anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void testAddReviewFailsWhenCustomerNotFound() {
        when(customerDAO.findByAccountId(999)).thenReturn(Optional.empty());

        boolean ok = movieCatalogService.addReview(999, 10, 5, "Nice");
        assertFalse(ok);
    }
}
