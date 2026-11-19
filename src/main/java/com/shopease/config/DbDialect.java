package com.shopease.config;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Tells the few places that need database-specific SQL whether they talk to MySQL or PostgreSQL.
 * Detected lazily from the JDBC metadata, so the application still deploys while the database is down.
 */
@Component
public class DbDialect {

    private final DataSource dataSource;
    private volatile Boolean postgres;

    public DbDialect(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isPostgres() {
        Boolean p = postgres;
        if (p == null) {
            try (Connection c = dataSource.getConnection()) {
                p = c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgres");
                postgres = p;
            } catch (SQLException e) {
                throw new CannotGetJdbcConnectionException("Cannot detect the database type", e);
            }
        }
        return p;
    }
}
