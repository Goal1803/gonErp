-- Runs BEFORE Hibernate ddl-auto on every startup.
-- All statements must be idempotent.

-- Card archive support
ALTER TABLE tm_cards ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tm_cards ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;

-- Board auto-archive settings
ALTER TABLE tm_boards ADD COLUMN IF NOT EXISTS auto_archive_days INTEGER;
ALTER TABLE tm_boards ADD COLUMN IF NOT EXISTS archive_column_ids TEXT;

-- Fix board_type check constraint to include POD_ORDER
ALTER TABLE tm_boards DROP CONSTRAINT IF EXISTS tm_boards_board_type_check;
ALTER TABLE tm_boards ADD CONSTRAINT tm_boards_board_type_check CHECK (board_type IN ('GENERAL', 'POD_DESIGN', 'POD_ORDER'));

-- Backfill POD_ORDER default columns: Cancelled, Refunded, Replaced.
-- Each INSERT is idempotent (NOT EXISTS guard) and re-reads MAX(position)
-- at run time so ordering stays correct across runs.
INSERT INTO tm_columns (board_id, title, position, created_at, last_updated_at)
SELECT b.id, 'Cancelled',
       COALESCE((SELECT MAX(position) FROM tm_columns WHERE board_id = b.id), -1) + 1,
       now(), now()
FROM tm_boards b
WHERE b.board_type = 'POD_ORDER'
  AND NOT EXISTS (
    SELECT 1 FROM tm_columns c WHERE c.board_id = b.id AND c.title = 'Cancelled'
  );

INSERT INTO tm_columns (board_id, title, position, created_at, last_updated_at)
SELECT b.id, 'Refunded',
       COALESCE((SELECT MAX(position) FROM tm_columns WHERE board_id = b.id), -1) + 1,
       now(), now()
FROM tm_boards b
WHERE b.board_type = 'POD_ORDER'
  AND NOT EXISTS (
    SELECT 1 FROM tm_columns c WHERE c.board_id = b.id AND c.title = 'Refunded'
  );

INSERT INTO tm_columns (board_id, title, position, created_at, last_updated_at)
SELECT b.id, 'Replaced',
       COALESCE((SELECT MAX(position) FROM tm_columns WHERE board_id = b.id), -1) + 1,
       now(), now()
FROM tm_boards b
WHERE b.board_type = 'POD_ORDER'
  AND NOT EXISTS (
    SELECT 1 FROM tm_columns c WHERE c.board_id = b.id AND c.title = 'Replaced'
  );

-- Image-similarity search: 64-bit perceptual hash for each mockup.
ALTER TABLE tm_design_mockups ADD COLUMN IF NOT EXISTS image_hash BIGINT;
CREATE INDEX IF NOT EXISTS idx_tm_design_mockups_image_hash ON tm_design_mockups (image_hash);

-- Supplier transaction: buyer-facing order id (e.g. Merchize "External number"),
-- used by the match step to pair txns directly with EcomOrder.platformOrderId.
ALTER TABLE ecom_supplier_transactions ADD COLUMN IF NOT EXISTS external_number VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_ecom_supplier_txn_external_number
    ON ecom_supplier_transactions (supplier_id, external_number);

-- Board load performance: the board view reads cards by column ordered by
-- position and filters out archived ones; child collections are counted by
-- card_id. Postgres does not auto-index foreign keys, so add them explicitly.
CREATE INDEX IF NOT EXISTS idx_tm_cards_column_position ON tm_cards (column_id, position);
CREATE INDEX IF NOT EXISTS idx_tm_cards_archived ON tm_cards (archived);
CREATE INDEX IF NOT EXISTS idx_tm_card_comments_card ON tm_card_comments (card_id);
CREATE INDEX IF NOT EXISTS idx_tm_card_attachments_card ON tm_card_attachments (card_id);
CREATE INDEX IF NOT EXISTS idx_tm_card_members_card ON tm_card_members (card_id);
CREATE INDEX IF NOT EXISTS idx_tm_card_label_map_card ON tm_card_label_map (card_id);
CREATE INDEX IF NOT EXISTS idx_tm_card_type_map_card ON tm_card_type_map (card_id);
