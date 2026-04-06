-- Runs BEFORE Hibernate ddl-auto on every startup.
-- All statements must be idempotent.

-- Card archive support
ALTER TABLE tm_cards ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tm_cards ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;

-- Board auto-archive settings
ALTER TABLE tm_boards ADD COLUMN IF NOT EXISTS auto_archive_days INTEGER;
ALTER TABLE tm_boards ADD COLUMN IF NOT EXISTS archive_column_ids TEXT;

-- Fix board_type check constraint to include POD_ORDER
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.check_constraints WHERE constraint_name = 'tm_boards_board_type_check') THEN
        ALTER TABLE tm_boards DROP CONSTRAINT tm_boards_board_type_check;
    END IF;
    ALTER TABLE tm_boards ADD CONSTRAINT tm_boards_board_type_check CHECK (board_type IN ('GENERAL', 'POD_DESIGN', 'POD_ORDER'));
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;
