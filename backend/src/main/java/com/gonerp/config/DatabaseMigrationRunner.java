package com.gonerp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Runs safe schema migrations on startup before Hibernate and scheduled tasks.
 * Each migration checks if the column/table already exists to be idempotent.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            // wt_settings: add break_reminder_minutes
            addColumnIfNotExists(conn, "wt_settings", "break_reminder_minutes",
                    "ALTER TABLE wt_settings ADD COLUMN break_reminder_minutes INT NOT NULL DEFAULT 240");

            // wt_settings: add force_checkout_time
            addColumnIfNotExists(conn, "wt_settings", "force_checkout_time",
                    "ALTER TABLE wt_settings ADD COLUMN force_checkout_time TIME DEFAULT '00:00:00'");

            // wt_user_configs: create table if not exists and populate from org defaults
            if (!tableExists(conn, "wt_user_configs")) {
                log.info("Migration: table wt_user_configs does not exist, Hibernate ddl-auto will create it");
            } else {
                // Populate missing user configs from org settings
                try (Statement stmt = conn.createStatement()) {
                    int rows = stmt.executeUpdate(
                            "INSERT INTO wt_user_configs (user_id, timezone_id, work_start_time, work_end_time, daily_working_hours, weekly_working_hours) " +
                            "SELECT u.id, COALESCE(s.timezone_id, 'Asia/Ho_Chi_Minh'), COALESCE(s.work_start_time, '09:00:00'), " +
                            "COALESCE(s.work_end_time, '18:00:00'), COALESCE(s.daily_working_hours, 8.0), COALESCE(s.weekly_full_time_hours, 40.0) " +
                            "FROM users u LEFT JOIN wt_settings s ON s.organization_id = u.organization_id " +
                            "WHERE u.id NOT IN (SELECT user_id FROM wt_user_configs)");
                    if (rows > 0) {
                        log.info("Migration: created {} user work time configs from org defaults", rows);
                    }
                }
            }

            log.info("Database migration check completed");
        } catch (Exception e) {
            log.warn("Database migration runner failed (non-fatal, Hibernate ddl-auto may handle it): {}", e.getMessage());
        }
    }

    private void addColumnIfNotExists(Connection conn, String table, String column, String alterSql) throws Exception {
        if (!columnExists(conn, table, column)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                log.info("Migration: added column {}.{}", table, column);
            }
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    private boolean tableExists(Connection conn, String table) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
