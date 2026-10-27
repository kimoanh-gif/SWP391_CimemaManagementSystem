package com.cinema.dao;

import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.dto.MovieForm;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Data Access Object for Movie and MovieGenre operations.
 * Screen 54 - Movie Management (UC-26 / SWP391).
 */
@Repository
public class MovieDao {

    private final JdbcTemplate jdbcTemplate;

    public MovieDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Movie> movieRowMapper = (rs, rowNum) -> {
        Movie movie = new Movie();
        movie.setMovieId(rs.getInt("MovieID"));
        movie.setTitle(rs.getString("Title"));

        try { movie.setOriginalTitle(rs.getString("OriginalTitle")); } catch (Exception ignored) {}
        movie.setDuration(rs.getInt("Duration"));

        Date releaseDate = rs.getDate("ReleaseDate");
        if (releaseDate != null) {
            movie.setReleaseDate(releaseDate.toLocalDate());
        }

        try {
            Date endDate = rs.getDate("EndDate");
            if (endDate != null) {
                movie.setEndDate(endDate.toLocalDate());
            }
        } catch (Exception ignored) {}

        try { movie.setLanguage(rs.getString("Language")); } catch (Exception ignored) {}
        try { movie.setDirector(rs.getString("Director")); } catch (Exception ignored) {}
        try { movie.setActors(rs.getString("Actors")); } catch (Exception ignored) {}
        try { movie.setAgeRating(rs.getString("AgeRating")); } catch (Exception ignored) {}
        try { movie.setDescription(rs.getString("Description")); } catch (Exception ignored) {}
        try { movie.setPoster(rs.getString("Poster")); } catch (Exception ignored) {}
        try { movie.setBanner(rs.getString("Banner")); } catch (Exception ignored) {}
        try { movie.setTrailer(rs.getString("Trailer")); } catch (Exception ignored) {}

        movie.setStatus(rs.getString("Status"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            movie.setCreatedAt(createdAt.toLocalDateTime());
        }

        try {
            Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
            if (updatedAt != null) {
                movie.setUpdatedAt(updatedAt.toLocalDateTime());
            }
        } catch (Exception ignored) {}

        return movie;
    };

    private final RowMapper<Genre> genreRowMapper = (rs, rowNum) -> {
        Genre genre = new Genre();
        genre.setGenreId(rs.getInt("GenreID"));
        genre.setName(rs.getString("Name"));
        genre.setDescription(rs.getString("Description"));
        genre.setStatus(rs.getString("Status"));
        return genre;
    };

    /**
     * Retrieves movies with multi-criteria filtering: search keyword, genre, age rating, and status.
     */
    public List<Movie> findAll(String keyword, Integer genreId, String ageRating, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    m.MovieID,
                    m.Title,
                    m.OriginalTitle,
                    m.Duration,
                    m.ReleaseDate,
                    m.EndDate,
                    m.[Language],
                    m.Director,
                    m.Actors,
                    m.AgeRating,
                    m.[Description],
                    m.PosterURL AS Poster,
                    m.BannerURL AS Banner,
                    m.TrailerURL AS Trailer,
                    m.[Status],
                    m.CreatedAt,
                    m.UpdatedAt
                FROM Movie m
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("""
                    AND (
                        LOWER(m.Title) LIKE LOWER(?)
                        OR LOWER(ISNULL(m.OriginalTitle, '')) LIKE LOWER(?)
                        OR LOWER(ISNULL(m.Director, '')) LIKE LOWER(?)
                        OR LOWER(ISNULL(m.Actors, '')) LIKE LOWER(?)
                    )
                    """);
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (genreId != null && genreId > 0) {
            sql.append(" AND EXISTS (SELECT 1 FROM MovieGenre mg WHERE mg.MovieID = m.MovieID AND mg.GenreID = ?)");
            params.add(genreId);
        }

        if (ageRating != null && !ageRating.trim().isEmpty() && !"ALL".equalsIgnoreCase(ageRating.trim())) {
            sql.append(" AND UPPER(m.AgeRating) = UPPER(?)");
            params.add(ageRating.trim());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append(" AND UPPER(m.[Status]) = UPPER(?)");
            params.add(status.trim());
        }

        sql.append(" ORDER BY m.MovieID DESC");

        List<Movie> movies = jdbcTemplate.query(sql.toString(), movieRowMapper, params.toArray());

        // Hydrate associated genres and showtimes count for each movie
        for (Movie movie : movies) {
            movie.setGenres(findGenresByMovieId(movie.getMovieId()));
            movie.setShowtimeCount(countShowtimesByMovieId(movie.getMovieId()));
        }

        return movies;
    }

    /**
     * Finds a single movie by ID with genres and showtimes count.
     */
    public Optional<Movie> findById(int movieId) {
        String sql = """
                SELECT
                    m.MovieID,
                    m.Title,
                    m.OriginalTitle,
                    m.Duration,
                    m.ReleaseDate,
                    m.EndDate,
                    m.[Language],
                    m.Director,
                    m.Actors,
                    m.AgeRating,
                    m.[Description],
                    m.PosterURL AS Poster,
                    m.BannerURL AS Banner,
                    m.TrailerURL AS Trailer,
                    m.[Status],
                    m.CreatedAt,
                    m.UpdatedAt
                FROM Movie m
                WHERE m.MovieID = ?
                """;

        List<Movie> results = jdbcTemplate.query(sql, movieRowMapper, movieId);
        if (results.isEmpty()) {
            return Optional.empty();
        }

        Movie movie = results.get(0);
        movie.setGenres(findGenresByMovieId(movieId));
        movie.setShowtimeCount(countShowtimesByMovieId(movieId));
        return Optional.of(movie);
    }

    /**
     * Inserts a new Movie record and returns the generated MovieID.
     */
    public int insert(MovieForm form) {
        String sql = """
                INSERT INTO Movie (
                    Title, OriginalTitle, Duration, ReleaseDate, EndDate,
                    [Language], Director, Actors, AgeRating, [Description],
                    PosterURL, BannerURL, TrailerURL, [Status], CreatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME())
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, form.getTitle().trim());
            ps.setString(2, emptyToNull(form.getOriginalTitle()));
            ps.setInt(3, form.getDuration() != null ? form.getDuration() : 120);
            ps.setDate(4, form.getReleaseDate() != null ? Date.valueOf(form.getReleaseDate()) : null);
            ps.setDate(5, form.getEndDate() != null ? Date.valueOf(form.getEndDate()) : null);
            ps.setString(6, emptyToNull(form.getLanguage()));
            ps.setString(7, emptyToNull(form.getDirector()));
            ps.setString(8, emptyToNull(form.getActors()));
            ps.setString(9, form.getAgeRating() != null ? form.getAgeRating().trim() : "P");
            ps.setString(10, emptyToNull(form.getDescription()));
            ps.setString(11, emptyToNull(form.getPoster()));
            ps.setString(12, emptyToNull(form.getBanner()));
            ps.setString(13, emptyToNull(form.getTrailer()));
            ps.setString(14, form.getStatus() != null ? form.getStatus().trim() : "Upcoming");
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        int newId = key != null ? key.intValue() : 0;

        // Assign movie genres
        if (newId > 0 && form.getGenreIds() != null && !form.getGenreIds().isEmpty()) {
            assignGenres(newId, form.getGenreIds());
        }

        return newId;
    }

    /**
     * Updates an existing movie's information.
     */
    public void update(int movieId, MovieForm form) {
        String sql = """
                UPDATE Movie
                SET Title = ?,
                    OriginalTitle = ?,
                    Duration = ?,
                    ReleaseDate = ?,
                    EndDate = ?,
                    [Language] = ?,
                    Director = ?,
                    Actors = ?,
                    AgeRating = ?,
                    [Description] = ?,
                    PosterURL = ?,
                    BannerURL = ?,
                    TrailerURL = ?,
                    [Status] = ?,
                    UpdatedAt = SYSUTCDATETIME()
                WHERE MovieID = ?
                """;

        jdbcTemplate.update(
                sql,
                form.getTitle().trim(),
                emptyToNull(form.getOriginalTitle()),
                form.getDuration(),
                form.getReleaseDate() != null ? Date.valueOf(form.getReleaseDate()) : null,
                form.getEndDate() != null ? Date.valueOf(form.getEndDate()) : null,
                emptyToNull(form.getLanguage()),
                emptyToNull(form.getDirector()),
                emptyToNull(form.getActors()),
                form.getAgeRating() != null ? form.getAgeRating().trim() : "P",
                emptyToNull(form.getDescription()),
                emptyToNull(form.getPoster()),
                emptyToNull(form.getBanner()),
                emptyToNull(form.getTrailer()),
                form.getStatus() != null ? form.getStatus().trim() : "Upcoming",
                movieId
        );

        // Update assigned genres
        assignGenres(movieId, form.getGenreIds());
    }

    /**
     * Updates only the movie status and updates timestamp.
     */
    public void updateStatus(int movieId, String status) {
        String sql = """
                UPDATE Movie
                SET [Status] = ?,
                    UpdatedAt = SYSUTCDATETIME()
                WHERE MovieID = ?
                """;
        jdbcTemplate.update(sql, status.trim(), movieId);
    }

    /**
     * Assigns genres to a movie while strictly enforcing BR-44:
     * The same Genre must not be assigned to the same Movie more than once.
     */
    public void assignGenres(int movieId, List<Integer> genreIds) {
        if (genreIds == null) {
            return; // Preserve existing genres if null
        }

        // Fetch existing genre IDs
        String currentSql = "SELECT GenreID FROM MovieGenre WHERE MovieID = ?";
        List<Integer> currentGenres = jdbcTemplate.queryForList(currentSql, Integer.class, movieId);
        Set<Integer> currentSet = new HashSet<>(currentGenres != null ? currentGenres : List.of());

        Set<Integer> newSet = new HashSet<>();
        for (Integer g : genreIds) {
            if (g != null && g > 0) {
                newSet.add(g);
            }
        }

        // Delete genres removed from movie
        for (Integer existingId : currentSet) {
            if (!newSet.contains(existingId)) {
                jdbcTemplate.update("DELETE FROM MovieGenre WHERE MovieID = ? AND GenreID = ?", movieId, existingId);
            }
        }

        // Insert new genres added to movie
        String insertSql = "INSERT INTO MovieGenre (MovieID, GenreID, AssignedAt) VALUES (?, ?, SYSUTCDATETIME())";
        for (Integer newId : newSet) {
            if (!currentSet.contains(newId)) {
                try {
                    jdbcTemplate.update(insertSql, movieId, newId);
                } catch (DataAccessException ignored) {
                }
            }
        }
    }

    /**
     * Retrieves all genres associated with a movie.
     */
    public List<Genre> findGenresByMovieId(int movieId) {
        String sql = """
                SELECT g.GenreID, g.[Name], g.[Description], g.[Status]
                FROM Genre g
                JOIN MovieGenre mg ON mg.GenreID = g.GenreID
                WHERE mg.MovieID = ?
                ORDER BY g.[Name] ASC
                """;
        try {
            return jdbcTemplate.query(sql, genreRowMapper, movieId);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    /**
     * BR-07: Retrieves movies available for scheduling showtimes (Upcoming or Now Showing only).
     */
    public List<Movie> findMoviesAvailableForShowtimes() {
        String sql = """
                SELECT m.MovieID, m.Title, m.OriginalTitle, m.Duration, m.ReleaseDate, m.EndDate,
                       m.[Language], m.Director, m.Actors, m.AgeRating, m.[Description],
                       m.PosterURL AS Poster, m.BannerURL AS Banner, m.TrailerURL AS Trailer,
                       m.[Status], m.CreatedAt, m.UpdatedAt
                FROM Movie m
                WHERE m.[Status] IN ('Upcoming', 'Now Showing')
                ORDER BY m.Title ASC
                """;
        List<Movie> movies = jdbcTemplate.query(sql, movieRowMapper);
        for (Movie m : movies) {
            m.setGenres(findGenresByMovieId(m.getMovieId()));
        }
        return movies;
    }

    /**
     * Counts how many showtimes are linked to this movie.
     */
    public int countShowtimesByMovieId(int movieId) {
        String sql = "SELECT COUNT(1) FROM Showtime WHERE MovieID = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, movieId);
            return count != null ? count : 0;
        } catch (DataAccessException ex) {
            return 0;
        }
    }

    /**
     * Counts how many upcoming/active showtimes are scheduled for this movie.
     * Considers all active showtimes (Scheduled, Selling, etc.) except Cancelled or Stopped.
     */
    public int countUpcomingShowtimesByMovieId(int movieId) {
        String sql = """
                SELECT COUNT(1)
                FROM Showtime
                WHERE MovieID = ?
                  AND StartTime >= SYSUTCDATETIME()
                  AND [Status] NOT IN ('Cancelled', 'Stopped')
                """;
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, movieId);
            return count != null ? count : 0;
        } catch (DataAccessException ex) {
            return 0;
        }
    }

    public List<Movie> findNowShowing(int limit) {
        String sql = """
                SELECT m.MovieID, m.Title, m.OriginalTitle, m.Duration, m.ReleaseDate, m.EndDate,
                       m.[Language], m.Director, m.Actors, m.AgeRating, m.[Description],
                       m.PosterURL AS Poster, m.BannerURL AS Banner, m.TrailerURL AS Trailer,
                       m.[Status], m.CreatedAt, m.UpdatedAt
                FROM dbo.Movie m
                WHERE (UPPER(m.[Status]) IN ('NOW_SHOWING', 'NOW SHOWING', 'ACTIVE')
                       OR EXISTS (
                           SELECT 1 FROM dbo.Showtime st
                           WHERE st.MovieID = m.MovieID
                             AND st.StartTime > SYSDATETIME()
                             AND UPPER(st.[Status]) IN ('SCHEDULED', 'ACTIVE')
                       ))
                  AND UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                ORDER BY m.ReleaseDate DESC
                """;
        if (limit > 0) {
            sql = sql.replaceFirst("SELECT ", "SELECT TOP (" + limit + ") ");
        }
        List<Movie> list = jdbcTemplate.query(sql, movieRowMapper);
        for (Movie m : list) {
            m.setGenres(findGenresByMovieId(m.getMovieId()));
        }
        return list;
    }

    public List<Movie> findComingSoon(int limit) {
        String sql = """
                SELECT m.MovieID, m.Title, m.OriginalTitle, m.Duration, m.ReleaseDate, m.EndDate,
                       m.[Language], m.Director, m.Actors, m.AgeRating, m.[Description],
                       m.PosterURL AS Poster, m.BannerURL AS Banner, m.TrailerURL AS Trailer,
                       m.[Status], m.CreatedAt, m.UpdatedAt
                FROM dbo.Movie m
                WHERE UPPER(m.[Status]) IN ('COMING_SOON', 'COMING SOON', 'UPCOMING')
                  AND UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                ORDER BY m.ReleaseDate ASC
                """;
        if (limit > 0) {
            sql = sql.replaceFirst("SELECT ", "SELECT TOP (" + limit + ") ");
        }
        List<Movie> list = jdbcTemplate.query(sql, movieRowMapper);
        for (Movie m : list) {
            m.setGenres(findGenresByMovieId(m.getMovieId()));
        }
        return list;
    }

    public List<Movie> findUpcomingByReleaseDate(int limit) {
        String sql = """
                SELECT m.MovieID, m.Title, m.OriginalTitle, m.Duration, m.ReleaseDate, m.EndDate,
                       m.[Language], m.Director, m.Actors, m.AgeRating, m.[Description],
                       m.PosterURL AS Poster, m.BannerURL AS Banner, m.TrailerURL AS Trailer,
                       m.[Status], m.CreatedAt, m.UpdatedAt
                FROM dbo.Movie m
                WHERE m.ReleaseDate > CAST(SYSDATETIME() AS DATE)
                  AND UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                ORDER BY m.ReleaseDate ASC, m.Title ASC
                """;
        if (limit > 0) {
            sql = sql.replaceFirst("SELECT ", "SELECT TOP (" + limit + ") ");
        }
        List<Movie> list = jdbcTemplate.query(sql, movieRowMapper);
        for (Movie m : list) {
            m.setGenres(findGenresByMovieId(m.getMovieId()));
        }
        return list;
    }

    public List<Movie> findFeatured(int limit) {
        return findTopGrossing(limit);
    }

    /**
     * Returns currently bookable movies ordered by successfully paid revenue.
     * A payment is collapsed to one amount per booking so gateway callbacks do
     * not accidentally count the same booking more than once.
     */
    public List<Movie> findTopGrossing(int limit) {
        String sql = """
                WITH paid_booking AS (
                    SELECT p.BookingID, MAX(p.Amount) AS PaidAmount
                    FROM dbo.Payment p
                    WHERE UPPER(p.[Status]) IN ('PAID', 'SUCCESS', 'COMPLETED')
                    GROUP BY p.BookingID
                ), movie_revenue AS (
                    SELECT st.MovieID, SUM(pb.PaidAmount) AS Revenue
                    FROM paid_booking pb
                    JOIN dbo.Booking b ON b.BookingID = pb.BookingID
                    JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                    WHERE UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED')
                    GROUP BY st.MovieID
                )
                SELECT m.MovieID, m.Title, m.OriginalTitle, m.Duration, m.ReleaseDate, m.EndDate,
                       m.[Language], m.Director, m.Actors, m.AgeRating, m.[Description],
                       m.PosterURL AS Poster, m.BannerURL AS Banner, m.TrailerURL AS Trailer,
                       m.[Status], m.CreatedAt, m.UpdatedAt
                FROM dbo.Movie m
                LEFT JOIN movie_revenue mr ON mr.MovieID = m.MovieID
                WHERE (UPPER(m.[Status]) IN ('NOW_SHOWING', 'NOW SHOWING', 'ACTIVE')
                       OR EXISTS (
                           SELECT 1 FROM dbo.Showtime st
                           WHERE st.MovieID = m.MovieID
                             AND st.StartTime > SYSDATETIME()
                             AND UPPER(st.[Status]) IN ('SCHEDULED', 'ACTIVE')
                       ))
                  AND UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                ORDER BY COALESCE(mr.Revenue, 0) DESC, m.ReleaseDate DESC, m.MovieID DESC
                """;
        if (limit > 0) {
            sql = sql.replaceFirst("SELECT m.MovieID", "SELECT TOP (" + limit + ") m.MovieID");
        }
        List<Movie> list = jdbcTemplate.query(sql, movieRowMapper);
        for (Movie m : list) {
            m.setGenres(findGenresByMovieId(m.getMovieId()));
        }
        return list;
    }

    public List<Movie> searchMovies(String keyword, Integer genreId, String status, String sortBy) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.MovieID, m.Title, m.OriginalTitle, m.Duration, m.ReleaseDate, m.EndDate,
                       m.[Language], m.Director, m.Actors, m.AgeRating, m.[Description],
                       m.PosterURL AS Poster, m.BannerURL AS Banner, m.TrailerURL AS Trailer,
                       m.[Status], m.CreatedAt, m.UpdatedAt
                FROM dbo.Movie m
                WHERE UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(m.Title) LIKE LOWER(?) OR LOWER(m.Director) LIKE LOWER(?)) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
        }

        if (genreId != null && genreId > 0) {
            sql.append(" AND EXISTS (SELECT 1 FROM dbo.MovieGenre mg WHERE mg.MovieID = m.MovieID AND mg.GenreID = ?) ");
            params.add(genreId);
        }

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            if ("NOW_SHOWING".equalsIgnoreCase(status) || "now-showing".equalsIgnoreCase(status)) {
                sql.append(" AND (UPPER(m.[Status]) IN ('NOW_SHOWING', 'NOW SHOWING', 'ACTIVE') ");
                sql.append("      OR EXISTS (SELECT 1 FROM dbo.Showtime st WHERE st.MovieID = m.MovieID AND st.StartTime > SYSDATETIME() AND UPPER(st.[Status]) IN ('SCHEDULED', 'ACTIVE'))) ");
            } else if ("COMING_SOON".equalsIgnoreCase(status)
                    || "coming-soon".equalsIgnoreCase(status)
                    || "upcoming".equalsIgnoreCase(status)) {
                sql.append(" AND m.ReleaseDate > CAST(SYSDATETIME() AS DATE) ");
            }
        }

        if ("title".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY m.Title ASC ");
        } else if ("duration".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY m.Duration DESC ");
        } else {
            sql.append(" ORDER BY m.ReleaseDate DESC, m.Title ASC ");
        }

        List<Movie> list = jdbcTemplate.query(sql.toString(), movieRowMapper, params.toArray());
        for (Movie m : list) {
            m.setGenres(findGenresByMovieId(m.getMovieId()));
        }
        return list;
    }

    public List<Movie> findRelated(int movieId, int limit) {
        String sql = """
                SELECT m.MovieID, m.Title, m.OriginalTitle, m.Duration, m.ReleaseDate, m.EndDate,
                       m.[Language], m.Director, m.Actors, m.AgeRating, m.[Description],
                       m.PosterURL AS Poster, m.BannerURL AS Banner, m.TrailerURL AS Trailer,
                       m.[Status], m.CreatedAt, m.UpdatedAt
                FROM dbo.Movie m
                WHERE m.MovieID <> ?
                  AND UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                ORDER BY m.ReleaseDate DESC
                """;
        if (limit > 0) {
            sql = sql.replaceFirst("SELECT ", "SELECT TOP (" + limit + ") ");
        }
        List<Movie> list = jdbcTemplate.query(sql, movieRowMapper, movieId);
        for (Movie m : list) {
            m.setGenres(findGenresByMovieId(m.getMovieId()));
        }
        return list;
    }

    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}
