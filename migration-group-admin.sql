-- Migration: Add group_role to user_user_groups
-- Safe to run on both fresh and existing databases

-- Add group_role column if it doesn't exist
ALTER TABLE user_user_groups ADD COLUMN IF NOT EXISTS group_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';

-- Migrate data from old group_admin column if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_user_groups' AND column_name = 'group_admin') THEN
        UPDATE user_user_groups SET group_role = 'ADMIN' WHERE group_admin = TRUE;
        ALTER TABLE user_user_groups DROP COLUMN group_admin;
    END IF;
END $$;
