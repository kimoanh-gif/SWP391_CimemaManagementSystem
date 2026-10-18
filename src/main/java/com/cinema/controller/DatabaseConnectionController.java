
package com.cinema.controller;

import com.cinema.service.DatabaseConnectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
public class DatabaseConnectionController {

    private final DatabaseConnectionService databaseConnectionService;

    public DatabaseConnectionController(
            DatabaseConnectionService databaseConnectionService) {
        this.databaseConnectionService = databaseConnectionService;
    }

    @GetMapping("/api/db-check")
    public ResponseEntity<Map<String, Object>> checkDatabase() {
        try {
            boolean connected =
                    databaseConnectionService.isDatabaseAvailable();

            return ResponseEntity.ok(Map.of(
                    "databaseConnected", connected,
                    "message", "Database connection successful"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "databaseConnected", false,
                    "message", "Database connection failed"
            ));
        }
    }
}
