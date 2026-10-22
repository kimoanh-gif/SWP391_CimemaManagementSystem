package com.cinema.service;

import com.cinema.dao.GenreDao;
import com.cinema.exception.DuplicateGenreException;
import com.cinema.exception.GenreAssociationException;
import com.cinema.exception.GenreNotFoundException;
import com.cinema.model.Genre;
import com.cinema.dto.GenreForm;
import com.cinema.model.MovieSummary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service handling business rules, validation, and lifecycle for Genre Management.
 * Enforces BR-Identity (uniqueness), BR-44, and movie association integrity.
 */
@Service
public class GenreService {

    private final GenreDao genreDao;

    public GenreService(GenreDao genreDao) {
        this.genreDao = genreDao;
    }

    /**
     * Retrieves all genres with optional search and status filtering.
     */
    public List<Genre> getAllGenres(String keyword, String status) {
        return genreDao.findAll(keyword, status);
    }

    /**
     * Retrieves a single genre by ID or throws GenreNotFoundException.
     */
    public Genre getGenreById(int genreId) {
        return genreDao.findById(genreId)
                .orElseThrow(() -> new GenreNotFoundException("Không tìm thấy thể loại với mã ID: " + genreId));
    }

    /**
     * Creates a new genre ensuring name uniqueness.
     */
    public Genre createGenre(GenreForm form) {
        validateForm(form);

        String trimmedName = form.getName().trim();
        if (genreDao.existsByNameIgnoreCase(trimmedName, null)) {
            throw new DuplicateGenreException("Thể loại '" + trimmedName + "' đã tồn tại trong hệ thống.");
        }

        form.setName(trimmedName);
        if (form.getStatus() == null || form.getStatus().trim().isEmpty()) {
            form.setStatus("ACTIVE");
        }

        int newId = genreDao.insert(form);
        return getGenreById(newId);
    }

    /**
     * Updates an existing genre, validating name uniqueness and movie association rules.
     */
    public Genre updateGenre(int genreId, GenreForm form) {
        validateForm(form);
        Genre existing = getGenreById(genreId);

        String trimmedName = form.getName().trim();
        if (genreDao.existsByNameIgnoreCase(trimmedName, genreId)) {
            throw new DuplicateGenreException("Tên thể loại '" + trimmedName + "' đã được sử dụng bởi một thể loại khác.");
        }

        form.setName(trimmedName);
        String requestedStatus = form.getStatus() != null ? form.getStatus().trim().toUpperCase() : "ACTIVE";

        // Business Rule: If transitioning from ACTIVE to INACTIVE (Deactivation),
        // check for active/scheduled movie associations to prevent inconsistent movie classifications.
        if ("ACTIVE".equalsIgnoreCase(existing.getStatus()) && "INACTIVE".equalsIgnoreCase(requestedStatus)) {
            validateDeactivationSafety(genreId, existing.getName());
        }

        form.setStatus(requestedStatus);
        genreDao.update(genreId, form);
        return getGenreById(genreId);
    }

    /**
     * Toggles the status between ACTIVE and INACTIVE (Screen 55 Deactivate action).
     */
    public Genre toggleStatus(int genreId) {
        Genre genre = getGenreById(genreId);
        String newStatus;

        if (genre.isActive()) {
            validateDeactivationSafety(genreId, genre.getName());
            newStatus = "INACTIVE";
        } else {
            newStatus = "ACTIVE";
        }

        genreDao.updateStatus(genreId, newStatus);
        return getGenreById(genreId);
    }

    /**
     * Deletes a genre only if no movies are associated with it.
     */
    public void deleteGenre(int genreId) {
        Genre genre = getGenreById(genreId);
        int movieCount = genreDao.countAssociatedMovies(genreId);

        if (movieCount > 0) {
            throw new GenreAssociationException(
                    "Không thể xóa thể loại '" + genre.getName() + "' vì đang có " + movieCount
                            + " phim liên kết. Vui lòng chuyển sang trạng thái Ngưng hoạt động (Deactivate) thay vì xóa."
            );
        }

        genreDao.delete(genreId);
    }

    /**
     * Retrieves movies classified under a specific genre.
     */
    public List<MovieSummary> getAssociatedMovies(int genreId) {
        getGenreById(genreId); // Ensure exists
        return genreDao.findAssociatedMovies(genreId);
    }

    private void validateForm(GenreForm form) {
        if (form == null || form.getName() == null || form.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống.");
        }
        if (form.getName().trim().length() > 100) {
            throw new IllegalArgumentException("Tên thể loại không được vượt quá 100 ký tự.");
        }
    }

    private void validateDeactivationSafety(int genreId, String genreName) {
        int activeMovies = genreDao.countActiveAssociatedMovies(genreId);
        if (activeMovies > 0) {
            throw new GenreAssociationException(
                    "Không thể hủy kích hoạt thể loại '" + genreName + "' vì hiện tại có "
                            + activeMovies + " phim đang chiếu hoặc sắp chiếu thuộc thể loại này. "
                            + "Vui lòng cập nhật phân loại phim trước khi hủy kích hoạt thể loại."
            );
        }
    }
}
