package com.cinema.dao;

import com.cinema.model.Genre;
import com.cinema.model.Movie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only movie queries for the public (Guest / Customer) screens:
 * Home Page, Movie List and Movie Details.
 *
 * Business rules (SRS UC01 / UC02):
 * - BR-ML-01 / BR-MI-01: only movies in a public status are returned
 *   ("Now Showing" and "Upcoming"); "Ended" / "Archived" movies are never shown.
 * - BR-ML-02 / BR-ML-04: Now Showing and Coming Soon are separated strictly by status,
 *   so one category is never filled with movies from another.
 * - Average rating and review count are calculated from ACTIVE reviews.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Repository
public class PublicMovieDAO {

    /** Status values that count as "Now Showing". */
    static final String NOW_SHOWING_CONDITION =
            "UPPER(m.[Status]) IN ('NOW SHOWING', 'NOW_SHOWING')";

    /** Status values that count as "Coming Soon". */
    static final String COMING_SOON_CONDITION =
            "UPPER(m.[Status]) IN ('UPCOMING', 'COMING SOON', 'COMING_SOON')";

    /** A movie is public when it is Now Showing or Coming Soon. */
    static final String PUBLIC_CONDITION =
            "(" + NOW_SHOWING_CONDITION + " OR " + COMING_SOON_CONDITION + ")";

    /*
     * m.* is used on purpose: some local databases still have the older Movie table
     * (without OriginalTitle / EndDate / Actors ...). Optional columns are read safely
     * in the row mapper, so the public pages work with both the old and the new schema.
     */
    private static final String BASE_SELECT = """
            SELECT m.*,
                   COALESCE(rv.AvgRating, 0) AS AvgRating,
                   COALESCE(rv.ReviewCount, 0) AS ReviewCount
            FROM dbo.Movie m
            LEFT JOIN (
                SELECT r.MovieID,
                       AVG(CAST(r.Rating AS FLOAT)) AS AvgRating,
                       COUNT(*) AS ReviewCount
                FROM dbo.MovieReview r
                WHERE UPPER(r.[Status]) = 'ACTIVE'
                GROUP BY r.MovieID
            ) rv ON rv.MovieID = m.MovieID
            """;

    private final JdbcTemplate jdbcTemplate;

    public PublicMovieDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Movie> movieRowMapper = (rs, rowNum) -> {
        Set<String> columns = columnNames(rs);
        Movie movie = new Movie();
        movie.setMovieId(rs.getInt("MovieID"));
        movie.setTitle(rs.getString("Title"));
        movie.setDuration(rs.getInt("Duration"));
        movie.setAgeRating(rs.getString("AgeRating"));
        movie.setStatus(rs.getString("Status"));

        movie.setOriginalTitle(optionalString(rs, columns, "OriginalTitle"));
        movie.setLanguage(optionalString(rs, columns, "Language"));
        movie.setDirector(optionalString(rs, columns, "Director"));
        movie.setActors(optionalString(rs, columns, "Actors"));
        movie.setDescription(optionalString(rs, columns, "Description"));
        movie.setPoster(optionalString(rs, columns, "PosterURL"));
        movie.setTrailer(optionalString(rs, columns, "TrailerURL"));

        if (columns.contains("RELEASEDATE")) {
            Date releaseDate = rs.getDate("ReleaseDate");
            movie.setReleaseDate(releaseDate != null ? releaseDate.toLocalDate() : null);
        }
        if (columns.contains("ENDDATE")) {
            Date endDate = rs.getDate("EndDate");
            movie.setEndDate(endDate != null ? endDate.toLocalDate() : null);
        }
        if (columns.contains("CREATEDAT")) {
            Timestamp createdAt = rs.getTimestamp("CreatedAt");
            movie.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        }
        if (columns.contains("UPDATEDAT")) {
            Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
            movie.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        }

        double avg = rs.getDouble("AvgRating");
        movie.setAverageRating(Math.round(avg * 10.0) / 10.0);
        movie.setReviewCount(rs.getInt("ReviewCount"));
        return movie;
    };

    private static Set<String> columnNames(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Set<String> names = new HashSet<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            names.add(meta.getColumnLabel(i).toUpperCase());
        }
        return names;
    }

    private static String optionalString(ResultSet rs, Set<String> columns, String column) throws SQLException {
        return columns.contains(column.toUpperCase()) ? rs.getString(column) : null;
    }

    /** Now Showing movies, newest release first. */
    public List<Movie> findNowShowing(int limit) {
        String sql = top(limit) + " WHERE " + NOW_SHOWING_CONDITION
                + " ORDER BY m.ReleaseDate DESC, m.Title ASC";
        return withGenres(jdbcTemplate.query(sql, movieRowMapper));
    }

    /** Coming Soon movies, nearest release first. */
    public List<Movie> findComingSoon(int limit) {
        String sql = top(limit) + " WHERE " + COMING_SOON_CONDITION
                + " ORDER BY CASE WHEN m.ReleaseDate IS NULL THEN 1 ELSE 0 END, m.ReleaseDate ASC, m.Title ASC";
        return withGenres(jdbcTemplate.query(sql, movieRowMapper));
    }

    /**
     * Featured movies for the hero slider: Now Showing movies ordered by audience rating,
     * then by newest release.
     */
    public List<Movie> findFeatured(int limit) {
        String sql = top(limit) + " WHERE " + NOW_SHOWING_CONDITION
                + " ORDER BY COALESCE(rv.AvgRating, 0) DESC, COALESCE(rv.ReviewCount, 0) DESC, m.ReleaseDate DESC";
        return withGenres(jdbcTemplate.query(sql, movieRowMapper));
    }

    /**
     * Search public movies.
     *
     * @param keyword   matches title, original title or director (optional)
     * @param genreId   genre filter (optional)
     * @param ageRating age rating filter, e.g. "T13" (optional)
     * @param type      "now-showing", "coming-soon" or anything else for all public movies
     * @param sortBy    "newest", "oldest", "rating", "popular", "title", "duration"
     */
    public List<Movie> search(String keyword, Integer genreId, String ageRating, String type, String sortBy) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();

        if ("now-showing".equalsIgnoreCase(type)) {
            sql.append(" WHERE ").append(NOW_SHOWING_CONDITION);
        } else if ("coming-soon".equalsIgnoreCase(type)) {
            sql.append(" WHERE ").append(COMING_SOON_CONDITION);
        } else {
            sql.append(" WHERE ").append(PUBLIC_CONDITION);
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (m.Title LIKE ? OR m.Director LIKE ?) ");
            String kw = "%" + escapeLike(keyword.trim()) + "%";
            params.add(kw);
            params.add(kw);
        }

        if (genreId != null && genreId > 0) {
            sql.append(" AND EXISTS (SELECT 1 FROM dbo.MovieGenre mg WHERE mg.MovieID = m.MovieID AND mg.GenreID = ?) ");
            params.add(genreId);
        }

        if (ageRating != null && !ageRating.isBlank()) {
            sql.append(" AND UPPER(m.AgeRating) = ? ");
            params.add(ageRating.trim().toUpperCase());
        }

        sql.append(orderBy(sortBy));
        return withGenres(jdbcTemplate.query(sql.toString(), movieRowMapper, params.toArray()));
    }

    /** Finds a movie only if it is public (BR-MI-01). */
    public Optional<Movie> findPublicById(int movieId) {
        String sql = BASE_SELECT + " WHERE m.MovieID = ? AND " + PUBLIC_CONDITION;
        List<Movie> result = jdbcTemplate.query(sql, movieRowMapper, movieId);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(withGenres(result).get(0));
    }

    /**
     * Related movies: public movies sharing at least one genre with the given movie,
     * then other Now Showing movies to fill the remaining slots.
     */
    public List<Movie> findRelated(int movieId, int limit) {
        String sql = top(limit) + " WHERE m.MovieID <> ? AND " + PUBLIC_CONDITION + """
                 ORDER BY CASE WHEN EXISTS (
                              SELECT 1 FROM dbo.MovieGenre a
                              JOIN dbo.MovieGenre b ON b.GenreID = a.GenreID
                              WHERE a.MovieID = m.MovieID AND b.MovieID = ?) THEN 0 ELSE 1 END,
                          CASE WHEN UPPER(m.[Status]) IN ('NOW SHOWING', 'NOW_SHOWING') THEN 0 ELSE 1 END,
                          m.ReleaseDate DESC
                """;
        return withGenres(jdbcTemplate.query(sql, movieRowMapper, movieId, movieId));
    }

    // ------------------------------------------------------------------ helpers

    private String top(int limit) {
        if (limit > 0) {
            return BASE_SELECT.replaceFirst("SELECT ", "SELECT TOP (" + limit + ") ");
        }
        return BASE_SELECT;
    }

    private String orderBy(String sortBy) {
        if (sortBy == null) {
            sortBy = "newest";
        }
        return switch (sortBy.toLowerCase()) {
            case "oldest" -> " ORDER BY CASE WHEN m.ReleaseDate IS NULL THEN 1 ELSE 0 END, m.ReleaseDate ASC, m.Title ASC ";
            case "rating" -> " ORDER BY COALESCE(rv.AvgRating, 0) DESC, COALESCE(rv.ReviewCount, 0) DESC, m.Title ASC ";
            case "popular" -> " ORDER BY COALESCE(rv.ReviewCount, 0) DESC, COALESCE(rv.AvgRating, 0) DESC, m.Title ASC ";
            case "title" -> " ORDER BY m.Title ASC ";
            case "duration" -> " ORDER BY m.Duration DESC, m.Title ASC ";
            default -> " ORDER BY m.ReleaseDate DESC, m.Title ASC ";
        };
    }

    private static String escapeLike(String value) {
        return value.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    /** Loads genres for all movies with one query instead of one query per movie. */
    private List<Movie> withGenres(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) {
            return movies == null ? Collections.emptyList() : movies;
        }
        List<Integer> ids = movies.stream().map(Movie::getMovieId).collect(Collectors.toList());
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = """
                SELECT mg.MovieID, g.GenreID, g.[Name], g.[Description], g.[Status]
                FROM dbo.MovieGenre mg
                JOIN dbo.Genre g ON g.GenreID = mg.GenreID
                WHERE mg.MovieID IN (%s)
                ORDER BY g.[Name]
                """.formatted(placeholders);

        Map<Integer, List<Genre>> genresByMovie = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            Genre genre = new Genre();
            genre.setGenreId(rs.getInt("GenreID"));
            genre.setName(rs.getString("Name"));
            genre.setDescription(rs.getString("Description"));
            genre.setStatus(rs.getString("Status"));
            genresByMovie.computeIfAbsent(rs.getInt("MovieID"), k -> new ArrayList<>()).add(genre);
        }, ids.toArray());

        for (Movie movie : movies) {
            movie.setGenres(genresByMovie.getOrDefault(movie.getMovieId(), new ArrayList<>()));
        }
        return movies;
    }
}
