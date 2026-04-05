-- Runs BEFORE Hibernate ddl-auto on every startup.
-- All statements must be idempotent (IF NOT EXISTS).

-- Card archive support
ALTER TABLE tm_cards ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tm_cards ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;

-- Board auto-archive settings
ALTER TABLE tm_boards ADD COLUMN IF NOT EXISTS auto_archive_days INTEGER;
ALTER TABLE tm_boards ADD COLUMN IF NOT EXISTS archive_column_ids TEXT;
