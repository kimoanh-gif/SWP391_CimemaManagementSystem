package com.cinema.dao;

import com.cinema.model.Genre;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GenreDAO {

    private final JdbcTemplate jdbcTemplate;

    public GenreDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Genre> findAll() {
        String sql = "SELECT GenreID, [Name] FROM dbo.Genre ORDER BY [Name]";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new Genre(rs.getInt("GenreID"), rs.getString("Name")));
    }

    public List<Genre> findByMovieId(int movieId) {
        String sql = """
                SELECT g.GenreID, g.[Name]
                FROM dbo.Genre g
                JOIN dbo.MovieGenre mg ON mg.GenreID = g.GenreID
                WHERE mg.MovieID = ?
                ORDER BY g.[Name]
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new Genre(rs.getInt("GenreID"), rs.getString("Name")), movieId);
    }
}
