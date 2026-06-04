-- Runs BEFORE Hibernate ddl-auto on every startup.
-- All statements must be idempotent.

-- Card archive support
ALTER TABLE tm_cards ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tm_cards ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;

-- POD_DESIGN card classification: designer- vs seller-generated.
-- Existing POD_DESIGN cards predate the field, so backfill them to DESIGNER.
ALTER TABLE tm_cards ADD COLUMN IF NOT EXISTS gen_type VARCHAR(20);
UPDATE tm_cards c
SET gen_type = 'DESIGNER'
FROM tm_columns col
JOIN tm_boards b ON b.id = col.board_id
WHERE c.column_id = col.id
  AND b.board_type = 'POD_DESIGN'
  AND c.gen_type IS NULL;

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

-- POD_DESIGN: insert "Seller Gen- Done" right after "Done" and
-- "Seller Gen- Listed" right after "Listed" on existing boards.
-- These behave like Done/Listed in dashboards/reports (seller-generated variant).
-- Each pair is a guarded shift-then-insert; the NOT EXISTS guard on the new
-- title makes both the position shift and the insert idempotent (skipped once
-- the column exists, e.g. on boards created after this code shipped).
-- Run the "Done" pair before the "Listed" pair so positions stay consistent.

-- 1) "Seller Gen- Done" after "Done": open the slot, then insert.
UPDATE tm_columns c
SET position = c.position + 1, last_updated_at = now()
FROM tm_boards b, tm_columns d
WHERE c.board_id = b.id
  AND b.board_type = 'POD_DESIGN'
  AND d.board_id = b.id AND d.title = 'Done'
  AND c.position > d.position
  AND NOT EXISTS (
    SELECT 1 FROM tm_columns x WHERE x.board_id = b.id AND x.title = 'Seller Gen- Done'
  );

INSERT INTO tm_columns (board_id, title, position, created_at, last_updated_at)
SELECT b.id, 'Seller Gen- Done', d.position + 1, now(), now()
FROM tm_boards b
JOIN tm_columns d ON d.board_id = b.id AND d.title = 'Done'
WHERE b.board_type = 'POD_DESIGN'
  AND NOT EXISTS (
    SELECT 1 FROM tm_columns x WHERE x.board_id = b.id AND x.title = 'Seller Gen- Done'
  );

-- 2) "Seller Gen- Listed" after "Listed": open the slot, then insert.
UPDATE tm_columns c
SET position = c.position + 1, last_updated_at = now()
FROM tm_boards b, tm_columns l
WHERE c.board_id = b.id
  AND b.board_type = 'POD_DESIGN'
  AND l.board_id = b.id AND l.title = 'Listed'
  AND c.position > l.position
  AND NOT EXISTS (
    SELECT 1 FROM tm_columns x WHERE x.board_id = b.id AND x.title = 'Seller Gen- Listed'
  );

INSERT INTO tm_columns (board_id, title, position, created_at, last_updated_at)
SELECT b.id, 'Seller Gen- Listed', l.position + 1, now(), now()
FROM tm_boards b
JOIN tm_columns l ON l.board_id = b.id AND l.title = 'Listed'
WHERE b.board_type = 'POD_DESIGN'
  AND NOT EXISTS (
    SELECT 1 FROM tm_columns x WHERE x.board_id = b.id AND x.title = 'Seller Gen- Listed'
  );
