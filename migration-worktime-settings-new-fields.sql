-- Migration: Add break_reminder_minutes and force_checkout_time to wt_settings
-- Safe to run multiple times (IF NOT EXISTS / idempotent defaults).

ALTER TABLE wt_settings ADD COLUMN IF NOT EXISTS break_reminder_minutes INT NOT NULL DEFAULT 240;
ALTER TABLE wt_settings ADD COLUMN IF NOT EXISTS force_checkout_time TIME DEFAULT '00:00:00';
