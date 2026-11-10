package com.cinema.service;

import com.cinema.dao.MovieDao;
import com.cinema.exception.InvalidMovieStatusException;
import com.cinema.exception.MovieHasActiveShowtimesException;
import com.cinema.exception.MovieNotFoundException;
import com.cinema.model.Movie;
import com.cinema.dto.MovieForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service managing movies, enforcing business rules UC-26, BR-07, BR-37, and
 * BR-44.
 */
@Service
public class MovieService {

    private static final Set<String> VALID_STATUSES = Set.of("Upcoming", "Now Showing", "Ended", "Archived");
    private static final Set<String> VALID_AGE_RATINGS = Set.of("P", "K", "T13", "T16", "T18");

    private final MovieDao movieDao;

    public MovieService(MovieDao movieDao) {
        this.movieDao = movieDao;
    }

    /**
     * Retrieves all movies matching search criteria.
     */
    public List<Movie> getAllMovies(String keyword, Integer genreId, String ageRating, String status) {
        return movieDao.findAll(keyword, genreId, ageRating, status);
    }

    /**
     * Retrieves a single movie by ID or throws MovieNotFoundException.
     */
    public Movie getMovieById(int movieId) {
        return movieDao.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with ID: " + movieId));
    }

    /**
     * Creates a new movie ensuring valid fields and BR-44 genre uniqueness.
     */
    @Transactional(rollbackFor = Exception.class)
    public Movie createMovie(MovieForm form) {
        validateMovieForm(form, true);

        if (form.getStatus() == null || form.getStatus().trim().isEmpty()) {
            form.setStatus("Upcoming");
        } else {
            form.setStatus(normalizeAndValidateStatus(form.getStatus()));
        }

        int newId = movieDao.insert(form);
        return getMovieById(newId);
    }

    /**
     * Updates movie information enforcing lifecycle rules, showtime integrity, and
     * BR-44.
     * P0-02: Enforces movie duration integrity against active upcoming showtimes.
     */
    @Transactional(rollbackFor = Exception.class)
    public Movie updateMovie(int movieId, MovieForm form) {
        validateMovieForm(form, false);
        Movie existing = getMovieById(movieId);

        LocalDate today = LocalDate.now();
        if (form.getReleaseDate() != null && form.getReleaseDate().isBefore(today)) {
            if (existing.getReleaseDate() == null || !existing.getReleaseDate().equals(form.getReleaseDate())) {
                throw new IllegalArgumentException(
                        "New release date must be from today onward, cannot be in the past.");
            }
        }
        if (form.getEndDate() != null && form.getEndDate().isBefore(today)) {
            if (existing.getEndDate() == null || !existing.getEndDate().equals(form.getEndDate())) {
                throw new IllegalArgumentException(
                        "New planned end date must be from today onward, cannot be in the past.");
            }
        }

        String newStatus = normalizeAndValidateStatus(form.getStatus());

        // Check if transition to 'Ended' or 'Archived' violates active upcoming
        // showtimes (BR-07)
        if (("Ended".equalsIgnoreCase(newStatus) || "Archived".equalsIgnoreCase(newStatus))
                && existing.isActiveForShowtimes()) {
            int upcomingShowtimes = movieDao.countUpcomingShowtimesByMovieId(movieId);
            if (upcomingShowtimes > 0) {
                throw new MovieHasActiveShowtimesException(
                        "Cannot change status of movie '" + existing.getTitle() + "' to " + newStatus
                                + " because there are currently " + upcomingShowtimes
                                + " upcoming showtime(s). Please cancel the showtimes first.");
            }
        }

        // P0-02: Movie Duration Integrity Check
        Integer oldDuration = existing.getDuration();
        Integer newDuration = form.getDuration();
        if (newDuration != null && oldDuration != null && !newDuration.equals(oldDuration)) {
            int upcomingShowtimes = movieDao.countUpcomingShowtimesByMovieId(movieId);
            if (upcomingShowtimes > 0) {
                throw new MovieHasActiveShowtimesException(
                        "Cannot change duration of movie '" + existing.getTitle() + "' (from " + oldDuration
                                + " min to " + newDuration + " min) because it has " + upcomingShowtimes
                                + " future showtime(s). Please adjust or cancel future showtimes before changing duration.");
            }
        }

        form.setStatus(newStatus);
        movieDao.update(movieId, form);
        return getMovieById(movieId);
    }

    /**
     * Deactivates a movie by setting status to 'Archived' (soft deactivation).
     * Enforces BR-37: Movies are NEVER physically deleted from the database.
     */
    public Movie deactivateMovie(int movieId) {
        Movie movie = getMovieById(movieId);

        if ("Archived".equalsIgnoreCase(movie.getStatus())) {
            return movie; // Already deactivated
        }

        int upcomingShowtimes = movieDao.countUpcomingShowtimesByMovieId(movieId);
        if (upcomingShowtimes > 0) {
            throw new MovieHasActiveShowtimesException(
                    "Cannot deactivate movie '" + movie.getTitle()
                            + "' because there are currently " + upcomingShowtimes
                            + " upcoming showtime(s). Please cancel or reschedule showtimes first.");
        }

        movieDao.updateStatus(movieId, "Archived");
        return getMovieById(movieId);
    }

    /**
     * Reactivates an Archived or Ended movie back to active status (Now Showing or
     * Upcoming).
     */
    public Movie reactivateMovie(int movieId) {
        Movie movie = getMovieById(movieId);
        String targetStatus = (movie.getReleaseDate() != null
                && movie.getReleaseDate().isAfter(java.time.LocalDate.now()))
                        ? "Upcoming"
                        : "Now Showing";
        movieDao.updateStatus(movieId, targetStatus);
        return getMovieById(movieId);
    }

    /**
     * Updates only the movie status with validation.
     */
    public Movie updateStatus(int movieId, String newStatus) {
        String normalized = normalizeAndValidateStatus(newStatus);
        Movie movie = getMovieById(movieId);

        if (("Ended".equalsIgnoreCase(normalized) || "Archived".equalsIgnoreCase(normalized))
                && movie.isActiveForShowtimes()) {
            int upcomingShowtimes = movieDao.countUpcomingShowtimesByMovieId(movieId);
            if (upcomingShowtimes > 0) {
                throw new MovieHasActiveShowtimesException(
                        "Cannot change movie status to " + normalized
                                + " because there are " + upcomingShowtimes + " upcoming showtime(s).");
            }
        }

        movieDao.updateStatus(movieId, normalized);
        return getMovieById(movieId);
    }

    /**
     * BR-07: Returns only movies available for showtime scheduling (Upcoming or Now
     * Showing).
     */
    public List<Movie> getAvailableMoviesForShowtimes() {
        return movieDao.findMoviesAvailableForShowtimes();
    }

    /**
     * AC02 / BR-37: Deletes or archives a movie while ensuring no active upcoming
     * showtimes exist.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMovie(int movieId) {
        Movie movie = getMovieById(movieId);
        int upcomingShowtimes = movieDao.countUpcomingShowtimesByMovieId(movieId);
        if (upcomingShowtimes > 0) {
            throw new MovieHasActiveShowtimesException(
                    "Cannot delete movie '" + movie.getTitle()
                            + "' because there are currently " + upcomingShowtimes
                            + " upcoming showtime(s). Please cancel the showtimes first.");
        }
        int totalShowtimes = movieDao.countShowtimesByMovieId(movieId);
        if (totalShowtimes > 0) {
            throw new MovieHasActiveShowtimesException(
                    "Cannot delete movie '" + movie.getTitle()
                            + "' because there are " + totalShowtimes
                            + " showtime(s) in system history. You can archive the movie instead.");
        }
        movieDao.delete(movieId);
    }

    /**
     * AC04: Retrieves active movies for customer-facing views (Now Showing and
     * Upcoming only, excluding Ended and Archived).
     * Delegates to getAvailableMoviesForShowtimes() to avoid duplicate DAO logic.
     */
    public List<Movie> getCustomerMovies() {
        return getAvailableMoviesForShowtimes();
    }

    private void validateMovieForm(MovieForm form, boolean isNew) {
        if (form == null) {
            throw new IllegalArgumentException("Movie data cannot be empty.");
        }
        if (form.getTitle() == null || form.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Movie title cannot be empty.");
        }
        if (form.getDuration() == null || form.getDuration() <= 0 || form.getDuration() > 600) {
            throw new IllegalArgumentException("Movie duration must be between 1 and 600 minutes.");
        }
        if (form.getAgeRating() != null && !form.getAgeRating().isBlank()) {
            String rating = form.getAgeRating().trim().toUpperCase();
            if (!VALID_AGE_RATINGS.contains(rating)) {
                throw new IllegalArgumentException("Invalid age rating: " + form.getAgeRating());
            }
            form.setAgeRating(rating);
        }
        LocalDate today = LocalDate.now();
        if (isNew) {
            if (form.getReleaseDate() != null && !form.getReleaseDate().isAfter(today)) {
                throw new IllegalArgumentException("Release date must be in the future (from tomorrow onward).");
            }
            if (form.getEndDate() != null && !form.getEndDate().isAfter(today)) {
                throw new IllegalArgumentException("End date must be in the future (from tomorrow onward).");
            }
        }
        if (form.getReleaseDate() != null && form.getEndDate() != null
                && form.getEndDate().isBefore(form.getReleaseDate())) {
            throw new IllegalArgumentException("End date must be after release date.");
        }
        if (form.getGenreIds() != null && !form.getGenreIds().isEmpty()) {
            Set<Integer> distinctGenreIds = new LinkedHashSet<>();
            for (Integer gid : form.getGenreIds()) {
                if (gid == null || gid <= 0) {
                    throw new IllegalArgumentException("Invalid movie genre ID.");
                }
                distinctGenreIds.add(gid);
            }
            int invalidGenreCount = movieDao.countInactiveOrMissingGenres(new ArrayList<>(distinctGenreIds));
            if (invalidGenreCount > 0) {
                throw new IllegalArgumentException(
                        "One or more selected genres do not exist or are inactive.");
            }
            form.setGenreIds(new ArrayList<>(distinctGenreIds));
        }
    }

    private String normalizeAndValidateStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new InvalidMovieStatusException("Movie status cannot be empty.");
        }
        String trimmed = status.trim();
        for (String valid : VALID_STATUSES) {
            if (valid.equalsIgnoreCase(trimmed)) {
                return valid;
            }
        }
        throw new InvalidMovieStatusException(
                "Status '" + status + "' is invalid. Allowed: " + String.join(", ", VALID_STATUSES));
    }
}
