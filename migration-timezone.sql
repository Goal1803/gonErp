-- Migration: Convert existing naive timestamps to TIMESTAMP WITH TIME ZONE
-- Run this on production BEFORE deploying the new backend code.
-- Existing timestamps are assumed to be in Asia/Ho_Chi_Minh timezone.

ALTER TABLE wt_settings ADD COLUMN IF NOT EXISTS timezone_id VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh';

ALTER TABLE wt_time_entries
  ALTER COLUMN check_in_time TYPE TIMESTAMP WITH TIME ZONE USING check_in_time AT TIME ZONE 'Asia/Ho_Chi_Minh',
  ALTER COLUMN check_out_time TYPE TIMESTAMP WITH TIME ZONE USING check_out_time AT TIME ZONE 'Asia/Ho_Chi_Minh';

ALTER TABLE wt_breaks
  ALTER COLUMN start_time TYPE TIMESTAMP WITH TIME ZONE USING start_time AT TIME ZONE 'Asia/Ho_Chi_Minh',
  ALTER COLUMN end_time TYPE TIMESTAMP WITH TIME ZONE USING end_time AT TIME ZONE 'Asia/Ho_Chi_Minh';
