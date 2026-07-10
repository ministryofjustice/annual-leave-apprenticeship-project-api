INSERT INTO users (id, first_name, last_name, email, password, manager_id, annual_entitlement, is_manager) VALUES
  ('00000000-0000-0000-0000-000000000002', 'Bob', 'Smith', 'bob@example.com', 'password', NULL, 30, true);

INSERT INTO users (id, first_name, last_name, email, password, manager_id, annual_entitlement, is_manager) VALUES
  ('00000000-0000-0000-0000-000000000001', 'Alice', 'Johnson', 'alice@example.com', 'password', '00000000-0000-0000-0000-000000000002', 25, false);

INSERT INTO leave_requests (id, created_at, creator_id, approver_id, start_date, end_date, duration, is_first_day_half_day, is_last_day_half_day, status, creator_note) VALUES
  ('00000000-0000-0000-0000-000000000101', '2026-05-20 10:00:00', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', '2026-06-10', '2026-06-14', 4.5, false, true, 'PENDING', 'Family holiday');

INSERT INTO leave_requests (id, created_at, decision_at, creator_id, approver_id, start_date, end_date, duration, is_first_day_half_day, is_last_day_half_day, status, creator_note, approver_note) VALUES
  ('00000000-0000-0000-0000-000000000102', '2026-05-22 14:30:00', '2026-05-23 09:00:00', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', '2026-07-01', '2026-07-03', 3.0, false, false, 'APPROVED', 'Short break', 'Looks good');
