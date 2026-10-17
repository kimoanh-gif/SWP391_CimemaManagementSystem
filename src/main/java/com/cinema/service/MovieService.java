package com.cinema.service;

import com.cinema.dao.MovieDao;
import com.cinema.exception.InvalidMovieStatusException;
import com.cinema.exception.MovieHasActiveShowtimesException;
import com.cinema.exception.MovieNotFoundException;
import com.cinema.model.Movie;
import com.cinema.model.MovieForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Service managing movies, enforcing business rules UC-26, BR-07, BR-37, and BR-44.
 */
@Service
public class MovieService {

    private static final Set<String> VALID_STATUSES = Set.of("Upcoming", "Now Showing", "Ended", "Archived");

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
                .orElseThrow(() -> new MovieNotFoundException("Không tìm thấy phim với mã ID: " + movieId));
    }

    /**
     * Creates a new movie ensuring valid fields and BR-44 genre uniqueness.
     */
    @Transactional(rollbackFor = Exception.class)
    public Movie createMovie(MovieForm form) {
        validateMovieForm(form);

        if (form.getStatus() == null || form.getStatus().trim().isEmpty()) {
            form.setStatus("Upcoming");
        } else {
            normalizeAndValidateStatus(form.getStatus());
        }

        int newId = movieDao.insert(form);
        return getMovieById(newId);
    }

    /**
     * Updates movie information enforcing lifecycle rules, showtime integrity, and BR-44.
     * P0-02: Enforces movie duration integrity against active upcoming showtimes.
     */
    @Transactional(rollbackFor = Exception.class)
    public Movie updateMovie(int movieId, MovieForm form) {
        validateMovieForm(form);
        Movie existing = getMovieById(movieId);

        String newStatus = normalizeAndValidateStatus(form.getStatus());

        // Check if transition to 'Ended' or 'Archived' violates active upcoming showtimes (BR-07)
        if (("Ended".equalsIgnoreCase(newStatus) || "Archived".equalsIgnoreCase(newStatus))
                && existing.isActiveForShowtimes()) {
            int upcomingShowtimes = movieDao.countUpcomingShowtimesByMovieId(movieId);
            if (upcomingShowtimes > 0) {
                throw new MovieHasActiveShowtimesException(
                        "Không thể chuyển trạng thái phim '" + existing.getTitle() + "' sang " + newStatus
                                + " vì hiện tại đang có " + upcomingShowtimes
                                + " suất chiếu sắp diễn ra. Vui lòng hủy các suất chiếu trước."
                );
            }
        }

        // P0-02: Movie Duration Integrity Check
        Integer oldDuration = existing.getDuration();
        Integer newDuration = form.getDuration();
        if (newDuration != null && oldDuration != null && !newDuration.equals(oldDuration)) {
            int upcomingShowtimes = movieDao.countUpcomingShowtimesByMovieId(movieId);
            if (upcomingShowtimes > 0) {
                throw new MovieHasActiveShowtimesException(
                        "Không thể thay đổi thời lượng phim '" + existing.getTitle() + "' (từ " + oldDuration
                                + " phút thành " + newDuration + " phút) vì phim đang có " + upcomingShowtimes
                                + " suất chiếu trong tương lai. Vui lòng hủy hoặc điều chỉnh các suất chiếu tương lai trước khi thay đổi thời lượng."
                );
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
                    "Không thể hủy kích hoạt phim '" + movie.getTitle()
                            + "' vì đang có " + upcomingShowtimes
                            + " suất chiếu sắp diễn ra. Vui lòng hủy hoặc dời các suất chiếu trước."
            );
        }

        movieDao.updateStatus(movieId, "Archived");
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
                        "Không thể chuyển trạng thái phim sang " + normalized
                                + " vì đang có " + upcomingShowtimes + " suất chiếu sắp diễn ra."
                );
            }
        }

        movieDao.updateStatus(movieId, normalized);
        return getMovieById(movieId);
    }

    /**
     * BR-07: Returns only movies available for showtime scheduling (Upcoming or Now Showing).
     */
    public List<Movie> getAvailableMoviesForShowtimes() {
        return movieDao.findMoviesAvailableForShowtimes();
    }

    /**
     * AC02 / BR-37: Deactivates or archives a movie while ensuring no active upcoming showtimes exist.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMovie(int movieId) {
        Movie movie = getMovieById(movieId);
        int upcomingShowtimes = movieDao.countUpcomingShowtimesByMovieId(movieId);
        if (upcomingShowtimes > 0) {
            throw new MovieHasActiveShowtimesException(
                    "Không thể xóa phim '" + movie.getTitle()
                            + "' vì đang có " + upcomingShowtimes
                            + " suất chiếu sắp diễn ra. Vui lòng hủy các suất chiếu trước."
            );
        }
        deactivateMovie(movieId);
    }

    /**
     * AC04: Retrieves active movies for customer-facing views (Now Showing and Upcoming only, excluding Ended and Archived).
     */
    public List<Movie> getCustomerMovies() {
        return movieDao.findMoviesAvailableForShowtimes();
    }

    private void validateMovieForm(MovieForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Dữ liệu phim không được để trống.");
        }
        if (form.getTitle() == null || form.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên phim không được để trống.");
        }
        if (form.getDuration() == null || form.getDuration() <= 0) {
            throw new IllegalArgumentException("Thời lượng phim phải lớn hơn 0 phút.");
        }
        if (form.getReleaseDate() != null && form.getEndDate() != null
                && form.getEndDate().isBefore(form.getReleaseDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được diễn ra trước ngày khởi chiếu.");
        }
    }

    private String normalizeAndValidateStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new InvalidMovieStatusException("Trạng thái phim không được để trống.");
        }
        String trimmed = status.trim();
        for (String valid : VALID_STATUSES) {
            if (valid.equalsIgnoreCase(trimmed)) {
                return valid;
            }
        }
        throw new InvalidMovieStatusException(
                "Trạng thái '" + status + "' không hợp lệ. Chỉ chấp nhận: " + String.join(", ", VALID_STATUSES)
        );
    }
}
