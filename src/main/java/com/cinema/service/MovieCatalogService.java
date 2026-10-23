package com.cinema.service;

import com.cinema.dao.CustomerDAO;
import com.cinema.dao.GenreDao;
import com.cinema.dao.MovieReviewDAO;
import com.cinema.dao.PublicMovieDAO;
import com.cinema.model.Customer;
import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.model.MovieReview;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Business logic for the public movie catalog (Home Page, Movie List, Movie Details).
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Service
public class MovieCatalogService {

    /** Movie type filter values accepted on the Movie List screen. */
    public static final List<String> MOVIE_TYPES = List.of("all", "now-showing", "coming-soon");

    /** Sort options accepted on the Movie List screen. */
    public static final List<String> SORT_OPTIONS = List.of("newest", "oldest", "rating", "popular", "title", "duration");

    /** Age ratings supported by the system (BR-MI-03). */
    public static final List<String> AGE_RATINGS = List.of("P", "K", "T13", "T16", "T18");

    private static final int MAX_REVIEW_LENGTH = 1000;

    private final PublicMovieDAO publicMovieDAO;
    private final GenreDao genreDAO;
    private final MovieReviewDAO movieReviewDAO;
    private final CustomerDAO customerDAO;

    public MovieCatalogService(PublicMovieDAO publicMovieDAO, GenreDao genreDAO,
                               MovieReviewDAO movieReviewDAO, CustomerDAO customerDAO) {
        this.publicMovieDAO = publicMovieDAO;
        this.genreDAO = genreDAO;
        this.movieReviewDAO = movieReviewDAO;
        this.customerDAO = customerDAO;
    }

    public List<Movie> getFeaturedMovies(int limit) {
        return publicMovieDAO.findFeatured(limit);
    }

    public List<Movie> getNowShowingMovies(int limit) {
        return publicMovieDAO.findNowShowing(limit);
    }

    public List<Movie> getComingSoonMovies(int limit) {
        return publicMovieDAO.findComingSoon(limit);
    }

    /**
     * Searches public movies. Invalid filter values are ignored so the screen never fails
     * because of a tampered query string.
     */
    public List<Movie> searchMovies(String keyword, Integer genreId, String ageRating, String type, String sortBy) {
        return publicMovieDAO.search(
                keyword,
                genreId,
                normalizeAgeRating(ageRating),
                normalizeType(type),
                normalizeSort(sortBy));
    }

    /** All public movies (Now Showing + Coming Soon), used by dropdown filters. */
    public List<Movie> getAllPublicMovies() {
        return publicMovieDAO.search(null, null, null, "all", "title");
    }

    /** Returns the movie only when it is publicly visible (BR-MI-01). */
    public Optional<Movie> getPublicMovieById(int movieId) {
        if (movieId <= 0) {
            return Optional.empty();
        }
        return publicMovieDAO.findPublicById(movieId);
    }

    public List<Movie> getRelatedMovies(int movieId, int limit) {
        return publicMovieDAO.findRelated(movieId, limit);
    }

    /** Only ACTIVE genres are offered as filters. */
    public List<Genre> getActiveGenres() {
        return genreDAO.findAll(null, "ACTIVE");
    }

    public List<String> getAgeRatings() {
        return AGE_RATINGS;
    }

    public List<MovieReview> getMovieReviews(int movieId) {
        return movieReviewDAO.findByMovieId(movieId);
    }

    /**
     * Saves (or updates) the customer's review for a public movie.
     *
     * @return false when the account is not a customer or the movie is not public
     */
    public boolean addReview(int accountId, int movieId, int rating, String content) {
        Optional<Customer> customerOpt = customerDAO.findByAccountId(accountId);
        if (customerOpt.isEmpty()) {
            return false;
        }
        if (publicMovieDAO.findPublicById(movieId).isEmpty()) {
            return false;
        }
        int safeRating = Math.max(1, Math.min(5, rating));
        String safeContent = content == null ? "" : content.trim();
        if (safeContent.length() > MAX_REVIEW_LENGTH) {
            safeContent = safeContent.substring(0, MAX_REVIEW_LENGTH);
        }
        movieReviewDAO.saveOrUpdateReview(customerOpt.get().getCustomerId(), movieId, safeRating, safeContent);
        return true;
    }

    // ------------------------------------------------------------------ normalisation

    public String normalizeType(String type) {
        if (type == null) {
            return "all";
        }
        String value = type.trim().toLowerCase();
        return MOVIE_TYPES.contains(value) ? value : "all";
    }

    public String normalizeSort(String sortBy) {
        if (sortBy == null) {
            return "newest";
        }
        String value = sortBy.trim().toLowerCase();
        return SORT_OPTIONS.contains(value) ? value : "newest";
    }

    /** @return the upper-case age rating, or null when the value is empty or unsupported */
    public String normalizeAgeRating(String ageRating) {
        if (ageRating == null || ageRating.isBlank()) {
            return null;
        }
        String value = ageRating.trim().toUpperCase();
        return Set.copyOf(AGE_RATINGS).contains(value) ? value : null;
    }
}
