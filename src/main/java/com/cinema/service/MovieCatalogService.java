package com.cinema.service;

import com.cinema.dao.CustomerDAO;
import com.cinema.dao.GenreDao;
import com.cinema.dao.MovieDao;
import com.cinema.dao.MovieReviewDAO;
import com.cinema.model.Customer;
import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.model.MovieReview;
import com.cinema.model.ReviewableMovie;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MovieCatalogService {

    private final MovieDao movieDAO;
    private final GenreDao genreDAO;
    private final MovieReviewDAO movieReviewDAO;
    private final CustomerDAO customerDAO;

    public MovieCatalogService(MovieDao movieDAO, GenreDao genreDAO,
                               MovieReviewDAO movieReviewDAO, CustomerDAO customerDAO) {
        this.movieDAO = movieDAO;
        this.genreDAO = genreDAO;
        this.movieReviewDAO = movieReviewDAO;
        this.customerDAO = customerDAO;
    }

    public List<Movie> getFeaturedMovies(int limit) {
        return withRatings(movieDAO.findFeatured(limit));
    }

    public List<Movie> getNowShowingMovies(int limit) {
        return withRatings(movieDAO.findNowShowing(limit));
    }

    public List<Movie> getComingSoonMovies(int limit) {
        return withRatings(movieDAO.findComingSoon(limit));
    }

    public List<Movie> getUpcomingMovies(int limit) {
        return withRatings(movieDAO.findUpcomingByReleaseDate(limit));
    }

    public List<Movie> getUpcomingMovies(int limit) {
        return movieDAO.findUpcomingByReleaseDate(limit);
    }

    public List<Movie> searchMovies(String keyword, Integer genreId, String status, String sortBy) {
        return searchMovies(keyword, genreId, status, sortBy, null);
    }

    /** Movie List (Screen 22): keyword, genre, age rating, now showing / coming soon, sort. */
    public List<Movie> searchMovies(String keyword, Integer genreId, String status, String sortBy, String ageRating) {
        List<Movie> movies = withRatings(movieDAO.searchMovies(keyword, genreId, status, sortBy));
        if (ageRating != null && !ageRating.isBlank() && !"ALL".equalsIgnoreCase(ageRating.trim())) {
            String wanted = ageRating.trim();
            movies = new ArrayList<>(movies.stream()
                    .filter(m -> m.getAgeRating() != null && m.getAgeRating().trim().equalsIgnoreCase(wanted))
                    .toList());
        }
        if ("rating".equalsIgnoreCase(sortBy)) {
            movies = new ArrayList<>(movies);
            movies.sort(Comparator.comparing((Movie m) -> m.getReviewCount() != null && m.getReviewCount() > 0
                            ? m.getAverageRating() : 0.0).reversed()
                    .thenComparing(m -> m.getReviewCount() == null ? 0 : m.getReviewCount(), Comparator.reverseOrder()));
        }
        return movies;
    }

    public Optional<Movie> getMovieById(int movieId) {
        Optional<Movie> movieOpt = movieDAO.findById(movieId);
        movieOpt.ifPresent(m -> {
            m.setGenreList(genreDAO.findByMovieId(movieId));
            withRatings(List.of(m));
        });
        return movieOpt;
    }

    /** "More Movies Now Showing" on Movie Details: only movies already released, never the current one. */
    public List<Movie> getRelatedMovies(int movieId, int limit) {
        LocalDate today = LocalDate.now();
        List<Movie> nowShowing = movieDAO.findNowShowing(0).stream()
                .filter(m -> m.getMovieId() != null && m.getMovieId() != movieId)
                .filter(m -> m.getReleaseDate() == null || !m.getReleaseDate().isAfter(today))
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .toList();
        return withRatings(nowShowing);
    }

    /** Fills the real average rating and review count (ACTIVE reviews only). */
    private List<Movie> withRatings(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) return movies;
        Map<Integer, double[]> summaries = movieReviewDAO.findRatingSummaries();
        for (Movie m : movies) {
            double[] summary = summaries == null || m.getMovieId() == null ? null : summaries.get(m.getMovieId());
            m.setAverageRating(summary != null ? summary[0] : 0.0);
            m.setReviewCount(summary != null ? (int) summary[1] : 0);
        }
        return movies;
    }

    public List<Genre> getAllGenres() {
        return genreDAO.findAll();
    }

    public List<MovieReview> getMovieReviews(int movieId) {
        return movieReviewDAO.findByMovieId(movieId);
    }

    public List<ReviewableMovie> getReviewableMovies(int accountId) {
        return movieReviewDAO.findReviewableMovies(accountId);
    }

    public boolean addReview(int accountId, int movieId, int bookingId, int rating, String content) {
        Optional<Customer> customerOpt = customerDAO.findByAccountId(accountId);
        if (customerOpt.isEmpty()) {
            return false;
        }
        if (rating < 1) rating = 1;
        if (rating > 5) rating = 5;
        Integer eligibleBookingId = movieReviewDAO.findEligibleBookingId(accountId, movieId, bookingId);
        if (eligibleBookingId == null || eligibleBookingId != bookingId) return false;
        movieReviewDAO.saveOrUpdateReview(customerOpt.get().getCustomerId(), movieId, bookingId, rating, content);
        return true;
    }

    public Integer getReviewBookingId(int accountId, int movieId, Integer bookingId) {
        return movieReviewDAO.findEligibleBookingId(accountId, movieId, bookingId);
    }

    public MovieReview getReviewForBooking(int bookingId) {
        return movieReviewDAO.findByBookingId(bookingId);
    }

    public boolean reportReview(int accountId, int movieId, int reviewId, String reason, String details) {
        if (reason == null || !List.of("SPOILER", "FALSE_INFORMATION", "OFFENSIVE", "SPAM", "OTHER").contains(reason)) {
            return false;
        }
        String cleanDetails = details == null ? "" : details.trim();
        if (cleanDetails.length() > 500) cleanDetails = cleanDetails.substring(0, 500);
        return movieReviewDAO.reportReview(reviewId, movieId, accountId, reason, cleanDetails);
    }
}
