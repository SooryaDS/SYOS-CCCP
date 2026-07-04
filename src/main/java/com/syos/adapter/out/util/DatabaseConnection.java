package com.syos.adapter.out.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/syos_db2";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private static final HikariDataSource dataSource;

    static {
        try {
            // ✅ Explicitly load MySQL driver to avoid "No suitable driver" errors
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL JDBC Driver loaded.");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USER);
            config.setPassword(DB_PASSWORD);

            // ✅ Optional pool tuning
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(30000);
            config.setLeakDetectionThreshold(20000);

            // ✅ MySQL-specific optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            System.out.println("✅ HikariCP Connection Pool initialized.");

            // Optional: test connection right away
            try (Connection testConn = dataSource.getConnection()) {
                System.out.println("✅ Successfully connected to the database.");
            }

        } catch (Exception e) {
            System.err.println("Failed to initialize HikariCP: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database pool initialization error", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("HikariCP Connection Pool shut down.");
        }
    }
}
