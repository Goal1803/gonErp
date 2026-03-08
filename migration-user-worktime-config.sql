-- Migration: Populate wt_user_configs for all existing users using their org's wt_settings defaults.
-- Safe to run multiple times — skips users who already have a config row.

INSERT INTO wt_user_configs (user_id, timezone_id, work_start_time, work_end_time, daily_working_hours, weekly_working_hours)
SELECT
    u.id,
    COALESCE(s.timezone_id, 'Asia/Ho_Chi_Minh'),
    COALESCE(s.work_start_time, '09:00:00'),
    COALESCE(s.work_end_time, '18:00:00'),
    COALESCE(s.daily_working_hours, 8.0),
    COALESCE(s.weekly_full_time_hours, 40.0)
FROM users u
LEFT JOIN wt_settings s ON s.organization_id = u.organization_id
WHERE u.id NOT IN (SELECT user_id FROM wt_user_configs);
