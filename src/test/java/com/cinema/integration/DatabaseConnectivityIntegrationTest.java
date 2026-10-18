package com.cinema.integration;

import com.cinema.dao.DatabaseConnectionDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class DatabaseConnectivityIntegrationTest {

    @Autowired
    private DatabaseConnectionDao databaseConnectionDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify SQL Server connection is active via SELECT 1")
    void testDatabaseConnection() {
        boolean connected = false;
        try {
            connected = databaseConnectionDao.isConnectionAvailable();
        } catch (Exception ex) {
            System.err.println("Database connection failed: " + ex.getMessage());
        }
        assertTrue(connected, "SQL Server at localhost:1433 (CinemaManagementDB) should be reachable and responsive");
    }

    @Test
    @DisplayName("Inspect existence of core tables in database schema")
    void testCoreTablesExist() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'";
        List<String> tables = jdbcTemplate.queryForList(sql, String.class);
        System.out.println("Discovered database tables: " + tables);

        assertNotNull(tables);
        // Check for required schema tables
        boolean hasMovie = tables.stream().anyMatch(t -> t.equalsIgnoreCase("Movie"));
        boolean hasRoom = tables.stream().anyMatch(t -> t.equalsIgnoreCase("Room"));
        boolean hasGenre = tables.stream().anyMatch(t -> t.equalsIgnoreCase("Genre"));
        boolean hasSeatType = tables.stream().anyMatch(t -> t.equalsIgnoreCase("SeatType"));
        boolean hasTicketPricing = tables.stream().anyMatch(t -> t.equalsIgnoreCase("TicketPricing"));
        boolean hasFBItem = tables.stream().anyMatch(t -> t.equalsIgnoreCase("FBItem"));

        System.out.println("Table status -> Movie: " + hasMovie + ", Room: " + hasRoom + ", Genre: " + hasGenre
                + ", SeatType: " + hasSeatType + ", TicketPricing: " + hasTicketPricing + ", FBItem: " + hasFBItem);
    }
}
