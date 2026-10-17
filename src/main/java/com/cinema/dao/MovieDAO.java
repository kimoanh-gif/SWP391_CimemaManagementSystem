package com.cinema.dao;

import com.cinema.model.Movie;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MovieDAO {

    private final JdbcTemplate jdbcTemplate;

    public MovieDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Movie> movieRowMapper = (rs, rowNum) -> mapMovie(rs);

    private Movie mapMovie(ResultSet rs) throws SQLException {
        Movie movie = new Movie();
        movie.setMovieId(rs.getInt("MovieID"));
        movie.setTitle(rs.getString("Title"));
        movie.setDuration(rs.getInt("Duration"));
        Date relDate = rs.getDate("ReleaseDate");
        movie.setReleaseDate(relDate != null ? relDate.toLocalDate() : null);
        movie.setLanguage(rs.getString("Language"));
        movie.setDirector(rs.getString("Director"));
        movie.setAgeRating(rs.getString("AgeRating"));
        movie.setDescription(rs.getString("Description"));
        movie.setStatus(rs.getString("Status"));
        movie.setPosterUrl(rs.getString("PosterURL"));
        movie.setTrailerUrl(rs.getString("TrailerURL"));
        movie.setGenres(rs.getString("Genres"));

        // Rating & reviews if present in query projection
        try {
            double avg = rs.getDouble("AvgRating");
            if (!rs.wasNull()) movie.setAverageRating(avg);
            int count = rs.getInt("ReviewCount");
            if (!rs.wasNull()) movie.setReviewCount(count);
        } catch (SQLException ignored) {
            // Columns not part of this projection
        }

        try {
            int showtimes = rs.getInt("ActiveShowtimes");
            if (!rs.wasNull()) movie.setActiveShowtimesCount(showtimes);
        } catch (SQLException ignored) {
        }

        return movie;
    }

    private static final String BASE_SELECT = """
            SELECT m.MovieID, m.Title, m.Duration, m.ReleaseDate, m.[Language],
                   m.Director, m.AgeRating, m.[Description], m.[Status], m.PosterURL, m.TrailerURL,
                   (SELECT STRING_AGG(g.[Name], ', ')
                    FROM dbo.MovieGenre mg
                    JOIN dbo.Genre g ON g.GenreID = mg.GenreID
                    WHERE mg.MovieID = m.MovieID) AS Genres,
                   COALESCE((SELECT AVG(CAST(r.Rating AS FLOAT))
                             FROM dbo.MovieReview r
                             WHERE r.MovieID = m.MovieID AND UPPER(r.[Status]) = 'ACTIVE'), 5.0) AS AvgRating,
                   COALESCE((SELECT COUNT(*)
                             FROM dbo.MovieReview r
                             WHERE r.MovieID = m.MovieID AND UPPER(r.[Status]) = 'ACTIVE'), 0) AS ReviewCount,
                   (SELECT COUNT(*)
                    FROM dbo.Showtime st
                    WHERE st.MovieID = m.MovieID
                      AND st.StartTime > SYSDATETIME()
                      AND UPPER(st.[Status]) IN ('SCHEDULED', 'ACTIVE')) AS ActiveShowtimes
            FROM dbo.Movie m
            """;

    public List<Movie> findAllActive() {
        String sql = BASE_SELECT + """
                WHERE UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                ORDER BY m.ReleaseDate DESC, m.Title ASC
                """;
        return jdbcTemplate.query(sql, movieRowMapper);
    }

    public List<Movie> findNowShowing(int limit) {
        String sql = BASE_SELECT + """
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
            sql = "SELECT TOP (" + limit + ") sub.* FROM (" + sql + ") sub";
        }
        return jdbcTemplate.query(sql, movieRowMapper);
    }

    public List<Movie> findComingSoon(int limit) {
        String sql = BASE_SELECT + """
                WHERE UPPER(m.[Status]) IN ('COMING_SOON', 'COMING SOON', 'UPCOMING')
                  AND UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                ORDER BY m.ReleaseDate ASC
                """;
        if (limit > 0) {
            sql = "SELECT TOP (" + limit + ") sub.* FROM (" + sql + ") sub";
        }
        return jdbcTemplate.query(sql, movieRowMapper);
    }

    public List<Movie> findFeatured(int limit) {
        String sql = BASE_SELECT + """
                WHERE UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                ORDER BY m.ReleaseDate DESC
                """;
        if (limit > 0) {
            sql = "SELECT TOP (" + limit + ") sub.* FROM (" + sql + ") sub";
        }
        return jdbcTemplate.query(sql, movieRowMapper);
    }

    public Optional<Movie> findById(int movieId) {
        String sql = BASE_SELECT + " WHERE m.MovieID = ?";
        try {
            Movie movie = jdbcTemplate.queryForObject(sql, movieRowMapper, movieId);
            return Optional.ofNullable(movie);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Movie> searchMovies(String keyword, Integer genreId, String status, String sortBy) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        sql.append(" WHERE UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED') ");

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
            } else if ("COMING_SOON".equalsIgnoreCase(status) || "coming-soon".equalsIgnoreCase(status)) {
                sql.append(" AND UPPER(m.[Status]) IN ('COMING_SOON', 'COMING SOON', 'UPCOMING') ");
            }
        }

        if ("title".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY m.Title ASC ");
        } else if ("rating".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY AvgRating DESC, m.ReleaseDate DESC ");
        } else if ("duration".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY m.Duration DESC ");
        } else {
            // Default newest release date
            sql.append(" ORDER BY m.ReleaseDate DESC, m.Title ASC ");
        }

        return jdbcTemplate.query(sql.toString(), movieRowMapper, params.toArray());
    }

    public List<Movie> findRelated(int movieId, int limit) {
        String sql = BASE_SELECT + """
                WHERE m.MovieID <> ?
                  AND UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')
                ORDER BY m.ReleaseDate DESC
                """;
        if (limit > 0) {
            sql = "SELECT TOP (" + limit + ") sub.* FROM (" + sql + ") sub";
        }
        return jdbcTemplate.query(sql, movieRowMapper, movieId);
    }
}
