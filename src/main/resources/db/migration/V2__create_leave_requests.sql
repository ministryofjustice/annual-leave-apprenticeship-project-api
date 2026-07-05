CREATE TABLE leave_requests (
  id UUID PRIMARY KEY,
  created_at TIMESTAMP NOT NULL,
  decision_at TIMESTAMP,
  creator_id UUID NOT NULL,
  approver_id UUID,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  duration DOUBLE PRECISION NOT NULL,
  is_first_day_half_day BOOLEAN NOT NULL,
  is_last_day_half_day BOOLEAN NOT NULL,
  status VARCHAR(20) NOT NULL,
  creator_note TEXT,
  approver_note TEXT,
  decision_seen_at TIMESTAMP,
  CONSTRAINT fk_leave_requests_creator FOREIGN KEY (creator_id) REFERENCES users(id),
  CONSTRAINT fk_leave_requests_approver FOREIGN KEY (approver_id) REFERENCES users(id)
);
