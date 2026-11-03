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
                .orElseThrow(() -> new GenreNotFoundException("Genre not found with ID: " + genreId));
    }

    /**
     * Creates a new genre ensuring name uniqueness.
     */
    public Genre createGenre(GenreForm form) {
        validateForm(form);

        String trimmedName = form.getName().trim();
        if (genreDao.existsByNameIgnoreCase(trimmedName, null)) {
            throw new DuplicateGenreException("Genre '" + trimmedName + "' already exists in the system.");
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
            throw new DuplicateGenreException("Genre name '" + trimmedName + "' is already in use by another genre.");
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
                    "Cannot delete genre '" + genre.getName() + "' because it is linked to " + movieCount
                            + " movie(s). Please deactivate the genre instead of deleting it."
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
            throw new IllegalArgumentException("Genre name cannot be empty.");
        }
        if (form.getName().trim().length() < 2 || form.getName().trim().length() > 100) {
            throw new IllegalArgumentException("Genre name cannot exceed 100 characters.");
        }
    }

    private void validateDeactivationSafety(int genreId, String genreName) {
        int activeMovies = genreDao.countActiveAssociatedMovies(genreId);
        if (activeMovies > 0) {
            throw new GenreAssociationException(
                    "Cannot deactivate genre '" + genreName + "' because there are currently "
                            + activeMovies + " now showing or upcoming movie(s) in this genre. "
                            + "Please update movie categorizations before deactivating."
            );
        }
    }
}
