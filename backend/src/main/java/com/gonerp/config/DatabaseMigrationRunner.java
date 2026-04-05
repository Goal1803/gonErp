package com.gonerp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Runs safe schema migrations on startup BEFORE Hibernate validates the schema.
 * The EntityManagerFactoryDependsOnPostProcessor ensures JPA waits for the
 * "databaseMigration" bean to be fully initialized before creating EntityManagerFactory.
 */
@Slf4j
@Configuration
public class DatabaseMigrationRunner {

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor emfDependsOnMigration() {
        return new EntityManagerFactoryDependsOnPostProcessor("databaseMigration");
    }

    @Bean(name = "databaseMigration")
    public Object runMigrations(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {

            // ── wt_settings migrations ──

            addColumnIfNotExists(conn, "wt_settings", "timezone_id",
                    "ALTER TABLE wt_settings ADD COLUMN timezone_id VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh'");

            addColumnIfNotExists(conn, "wt_settings", "break_reminder_minutes",
                    "ALTER TABLE wt_settings ADD COLUMN break_reminder_minutes INT NOT NULL DEFAULT 240");

            addColumnIfNotExists(conn, "wt_settings", "force_checkout_time",
                    "ALTER TABLE wt_settings ADD COLUMN force_checkout_time TIME DEFAULT '00:00:00'");

            // ── wt_time_entries: convert timestamps to TIMESTAMP WITH TIME ZONE ──

            convertColumnToTimestampTZ(conn, "wt_time_entries", "check_in_time");
            convertColumnToTimestampTZ(conn, "wt_time_entries", "check_out_time");

            // ── wt_breaks: convert timestamps to TIMESTAMP WITH TIME ZONE ──

            convertColumnToTimestampTZ(conn, "wt_breaks", "start_time");
            convertColumnToTimestampTZ(conn, "wt_breaks", "end_time");

            // ── user_user_groups: add group_role, migrate from group_admin ──

            if (tableExists(conn, "user_user_groups")) {
                addColumnIfNotExists(conn, "user_user_groups", "group_role",
                        "ALTER TABLE user_user_groups ADD COLUMN group_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER'");

                if (columnExists(conn, "user_user_groups", "group_admin")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("UPDATE user_user_groups SET group_role = 'ADMIN' WHERE group_admin = TRUE");
                        stmt.execute("ALTER TABLE user_user_groups DROP COLUMN group_admin");
                        log.info("Migration: migrated group_admin to group_role and dropped group_admin column");
                    }
                }
            }

            // ── wt_user_configs: populate from org defaults for existing users ──

            if (!tableExists(conn, "wt_user_configs")) {
                log.info("Migration: table wt_user_configs does not exist, Hibernate ddl-auto will create it");
            } else {
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

            // ── fin_transactions: add inter_account flag ──

            addColumnIfNotExists(conn, "fin_transactions", "inter_account",
                    "ALTER TABLE fin_transactions ADD COLUMN inter_account BOOLEAN NOT NULL DEFAULT FALSE");

            // ── tm_cards: card archive support ──

            addColumnIfNotExists(conn, "tm_cards", "archived",
                    "ALTER TABLE tm_cards ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE");

            addColumnIfNotExists(conn, "tm_cards", "archived_at",
                    "ALTER TABLE tm_cards ADD COLUMN archived_at TIMESTAMP");

            // ── tm_boards: auto-archive settings ──

            addColumnIfNotExists(conn, "tm_boards", "auto_archive_days",
                    "ALTER TABLE tm_boards ADD COLUMN auto_archive_days INTEGER");

            addColumnIfNotExists(conn, "tm_boards", "archive_column_ids",
                    "ALTER TABLE tm_boards ADD COLUMN archive_column_ids TEXT");

            log.info("Database migration check completed");
        } catch (Exception e) {
            log.warn("Database migration runner failed (non-fatal, Hibernate ddl-auto may handle it): {}", e.getMessage());
        }
        return new Object(); // marker bean
    }

    private void addColumnIfNotExists(Connection conn, String table, String column, String alterSql) throws Exception {
        if (!columnExists(conn, table, column)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                log.info("Migration: added column {}.{}", table, column);
            }
        }
    }

    private void convertColumnToTimestampTZ(Connection conn, String table, String column) throws Exception {
        if (!tableExists(conn, table)) return;
        if (!columnExists(conn, table, column)) return;

        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            if (rs.next()) {
                String typeName = rs.getString("TYPE_NAME").toLowerCase();
                if (typeName.contains("timestamptz") || typeName.contains("timestamp with time zone")) {
                    return;
                }
            }
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ALTER COLUMN " + column +
                    " TYPE TIMESTAMP WITH TIME ZONE USING " + column + " AT TIME ZONE 'Asia/Ho_Chi_Minh'");
            log.info("Migration: converted {}.{} to TIMESTAMP WITH TIME ZONE", table, column);
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
