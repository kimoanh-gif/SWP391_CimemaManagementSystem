package com.cinema.dao;

import com.cinema.model.Genre;
import com.cinema.model.GenreForm;
import com.cinema.model.MovieSummary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Genre and MovieGenre operations.
 * Implements direct SQL Server access via Spring JdbcTemplate.
 */
@Repository
public class GenreDao {

    private final JdbcTemplate jdbcTemplate;

    public GenreDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Genre> genreRowMapper = (rs, rowNum) -> {
        Genre genre = new Genre();
        genre.setGenreId(rs.getInt("GenreID"));
        genre.setName(rs.getString("Name"));
        genre.setDescription(rs.getString("Description"));
        genre.setStatus(rs.getString("Status"));

        Timestamp createdAtTs = rs.getTimestamp("CreatedAt");
        if (createdAtTs != null) {
            genre.setCreatedAt(createdAtTs.toLocalDateTime());
        }

        Timestamp updatedAtTs = rs.getTimestamp("UpdatedAt");
        if (updatedAtTs != null) {
            genre.setUpdatedAt(updatedAtTs.toLocalDateTime());
        }

        try {
            genre.setMovieCount(rs.getInt("MovieCount"));
        } catch (Exception ignored) {
            // MovieCount column not present in some queries
        }

        return genre;
    };

    /**
     * Retrieves all genres with associated movie count, supporting search keyword and status filtering.
     */
    public List<Genre> findAll(String keyword, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    g.GenreID,
                    g.[Name],
                    g.[Description],
                    g.[Status],
                    g.CreatedAt,
                    g.UpdatedAt,
                    COUNT(mg.MovieID) AS MovieCount
                FROM Genre g
                LEFT JOIN MovieGenre mg ON mg.GenreID = g.GenreID
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(g.[Name]) LIKE LOWER(?) OR LOWER(g.[Description]) LIKE LOWER(?))");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append(" AND UPPER(g.[Status]) = UPPER(?)");
            params.add(status.trim());
        }

        sql.append("""
                GROUP BY g.GenreID, g.[Name], g.[Description], g.[Status], g.CreatedAt, g.UpdatedAt
                ORDER BY g.[Name] ASC
                """);

        try {
            return jdbcTemplate.query(sql.toString(), genreRowMapper, params.toArray());
        } catch (DataAccessException ex) {
            // Fallback in case MovieGenre does not exist yet
            return findAllFallback(keyword, status);
        }
    }

    private List<Genre> findAllFallback(String keyword, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    g.GenreID,
                    g.[Name],
                    g.[Description],
                    g.[Status],
                    g.CreatedAt,
                    g.UpdatedAt,
                    0 AS MovieCount
                FROM Genre g
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(g.[Name]) LIKE LOWER(?) OR LOWER(g.[Description]) LIKE LOWER(?))");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append(" AND UPPER(g.[Status]) = UPPER(?)");
            params.add(status.trim());
        }
        sql.append(" ORDER BY g.[Name] ASC");

        return jdbcTemplate.query(sql.toString(), genreRowMapper, params.toArray());
    }

    /**
     * Finds a genre by its ID.
     */
    public Optional<Genre> findById(int genreId) {
        String sql = """
                SELECT
                    g.GenreID,
                    g.[Name],
                    g.[Description],
                    g.[Status],
                    g.CreatedAt,
                    g.UpdatedAt,
                    (SELECT COUNT(1) FROM MovieGenre mg WHERE mg.GenreID = g.GenreID) AS MovieCount
                FROM Genre g
                WHERE g.GenreID = ?
                """;

        try {
            List<Genre> results = jdbcTemplate.query(sql, genreRowMapper, genreId);
            if (results.isEmpty()) {
                return Optional.empty();
            }
            Genre genre = results.get(0);
            genre.setAssociatedMovieTitles(findAssociatedMovieTitles(genreId));
            return Optional.of(genre);
        } catch (DataAccessException ex) {
            // Fallback if MovieGenre doesn't exist
            String simpleSql = """
                    SELECT GenreID, [Name], [Description], [Status], CreatedAt, UpdatedAt, 0 AS MovieCount
                    FROM Genre
                    WHERE GenreID = ?
                    """;
            return jdbcTemplate.query(simpleSql, genreRowMapper, genreId).stream().findFirst();
        }
    }

    /**
     * Finds a genre by its exact Name (case-insensitive) to enforce BR-Identity uniqueness.
     */
    public Optional<Genre> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = """
                SELECT GenreID, [Name], [Description], [Status], CreatedAt, UpdatedAt, 0 AS MovieCount
                FROM Genre
                WHERE LOWER(LTRIM(RTRIM([Name]))) = LOWER(LTRIM(RTRIM(?)))
                """;

        return jdbcTemplate.query(sql, genreRowMapper, name.trim()).stream().findFirst();
    }

    /**
     * Checks if any other genre already uses the specified name.
     */
    public boolean existsByNameIgnoreCase(String name, Integer excludeId) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String sql = """
                SELECT COUNT(1)
                FROM Genre
                WHERE LOWER(LTRIM(RTRIM([Name]))) = LOWER(LTRIM(RTRIM(?)))
                  AND (? IS NULL OR GenreID <> ?)
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name.trim(), excludeId, excludeId);
        return count != null && count > 0;
    }

    /**
     * Inserts a new genre and returns the generated GenreID.
     */
    public int insert(GenreForm form) {
        String sql = """
                INSERT INTO Genre ([Name], [Description], [Status], CreatedAt)
                VALUES (?, ?, ?, SYSUTCDATETIME())
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, form.getName().trim());
            ps.setString(2, emptyToNull(form.getDescription()));
            ps.setString(3, form.getStatus() != null ? form.getStatus().trim() : "ACTIVE");
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key != null ? key.intValue() : 0;
    }

    /**
     * Updates an existing genre's name, description, and status.
     */
    public int update(int id, GenreForm form) {
        String sql = """
                UPDATE Genre
                SET [Name] = ?,
                    [Description] = ?,
                    [Status] = ?,
                    UpdatedAt = SYSUTCDATETIME()
                WHERE GenreID = ?
                """;

        return jdbcTemplate.update(
                sql,
                form.getName().trim(),
                emptyToNull(form.getDescription()),
                form.getStatus() != null ? form.getStatus().trim() : "ACTIVE",
                id
        );
    }

    /**
     * Updates only the Status field ('ACTIVE' or 'INACTIVE').
     */
    public int updateStatus(int id, String status) {
        String sql = """
                UPDATE Genre
                SET [Status] = ?,
                    UpdatedAt = SYSUTCDATETIME()
                WHERE GenreID = ?
                """;

        return jdbcTemplate.update(sql, status.trim(), id);
    }

    /**
     * Deletes a genre by ID (only when permitted and no movie associations exist).
     */
    public int delete(int id) {
        String sql = "DELETE FROM Genre WHERE GenreID = ?";
        return jdbcTemplate.update(sql, id);
    }

    /**
     * Counts how many movies are associated with this genre.
     */
    public int countAssociatedMovies(int genreId) {
        String sql = "SELECT COUNT(1) FROM MovieGenre WHERE GenreID = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, genreId);
            return count != null ? count : 0;
        } catch (DataAccessException ex) {
            return 0;
        }
    }

    /**
     * Counts how many active movies (Upcoming or Now Showing) are associated with this genre.
     */
    public int countActiveAssociatedMovies(int genreId) {
        String sql = """
                SELECT COUNT(1)
                FROM MovieGenre mg
                JOIN Movie m ON m.MovieID = mg.MovieID
                WHERE mg.GenreID = ?
                  AND LOWER(m.[Status]) IN ('upcoming', 'now showing')
                """;
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, genreId);
            return count != null ? count : 0;
        } catch (DataAccessException ex) {
            // Fallback: If Movie table doesn't have status or doesn't exist, check MovieGenre
            return countAssociatedMovies(genreId);
        }
    }

    /**
     * Retrieves titles of movies associated with this genre.
     */
    public List<String> findAssociatedMovieTitles(int genreId) {
        String sql = """
                SELECT m.Title
                FROM MovieGenre mg
                JOIN Movie m ON m.MovieID = mg.MovieID
                WHERE mg.GenreID = ?
                ORDER BY m.Title ASC
                """;
        try {
            return jdbcTemplate.queryForList(sql, String.class, genreId);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves summaries of movies associated with this genre.
     */
    public List<MovieSummary> findAssociatedMovies(int genreId) {
        String sql = """
                SELECT m.MovieID, m.Title, m.[Status]
                FROM MovieGenre mg
                JOIN Movie m ON m.MovieID = mg.MovieID
                WHERE mg.GenreID = ?
                ORDER BY m.Title ASC
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new MovieSummary(
                    rs.getInt("MovieID"),
                    rs.getString("Title"),
                    rs.getString("Status")
            ), genreId);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}
