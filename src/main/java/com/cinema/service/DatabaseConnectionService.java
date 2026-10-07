package com.cinema.service;

import com.cinema.dao.DatabaseConnectionDao;
import org.springframework.stereotype.Service;

/**
 * Provides an optional database connectivity check for diagnostics.
 */
@Service
public class DatabaseConnectionService {

    private final DatabaseConnectionDao databaseConnectionDao;

    public DatabaseConnectionService(DatabaseConnectionDao databaseConnectionDao) {
        this.databaseConnectionDao = databaseConnectionDao;
    }

    public boolean isDatabaseAvailable() {
        return databaseConnectionDao.isConnectionAvailable();
    }
}
