package com.cinema.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Infrastructure-only DAO used to check connectivity to the existing database.
 * It does not access any application tables.
 */
@Component
public class DatabaseConnectionDao {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isConnectionAvailable() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return Integer.valueOf(1).equals(result);
    }
}
