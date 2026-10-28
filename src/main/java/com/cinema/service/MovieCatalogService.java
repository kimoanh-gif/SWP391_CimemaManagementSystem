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

import java.util.List;
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
        return movieDAO.findFeatured(limit);
    }

    public List<Movie> getNowShowingMovies(int limit) {
        return movieDAO.findNowShowing(limit);
    }

    public List<Movie> getComingSoonMovies(int limit) {
        return movieDAO.findComingSoon(limit);
    }

    public List<Movie> getUpcomingMovies(int limit) {
        return movieDAO.findUpcomingByReleaseDate(limit);
    }

    public List<Movie> searchMovies(String keyword, Integer genreId, String status, String sortBy) {
        return movieDAO.searchMovies(keyword, genreId, status, sortBy);
    }

    public Optional<Movie> getMovieById(int movieId) {
        Optional<Movie> movieOpt = movieDAO.findById(movieId);
        movieOpt.ifPresent(m -> m.setGenreList(genreDAO.findByMovieId(movieId)));
        return movieOpt;
    }

    public List<Movie> getRelatedMovies(int movieId, int limit) {
        return movieDAO.findRelated(movieId, limit);
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
