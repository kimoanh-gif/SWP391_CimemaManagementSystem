package com.cinema.integration;

import com.cinema.dao.MovieDao;
import com.cinema.model.Movie;
import com.cinema.dto.MovieForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MovieDaoIntegrationTest {

    @Autowired
    private MovieDao movieDao;

    @Test
    @DisplayName("Verify movieDao.update() updates movie without SQL Server error 271 on computed PosterURL")
    void testUpdateMovieWithPosterUrl() {
        Optional<Movie> existingOpt = movieDao.findById(1);
        assertTrue(existingOpt.isPresent(), "Movie with ID 1 should exist");
        Movie existing = existingOpt.get();

        MovieForm form = new MovieForm();
        form.setTitle(existing.getTitle());
        form.setOriginalTitle(existing.getOriginalTitle());
        form.setDuration(existing.getDuration());
        form.setReleaseDate(existing.getReleaseDate());
        form.setEndDate(existing.getEndDate());
        form.setLanguage(existing.getLanguage());
        form.setDirector(existing.getDirector());
        form.setActors(existing.getActors());
        form.setAgeRating(existing.getAgeRating());
        form.setDescription(existing.getDescription());
        form.setPoster(existing.getPoster());
        form.setTrailer(existing.getTrailer());
        form.setStatus(existing.getStatus());
        form.setGenreIds(List.of());

        // This must not throw "The column 'PosterURL' cannot be modified because it is either a computed column..."
        assertDoesNotThrow(() -> movieDao.update(1, form));
    }
}
