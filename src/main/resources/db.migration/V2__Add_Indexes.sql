-- Add index on the paid_by column in expenses
CREATE INDEX idx_paid_by ON expenses (paid_by);

-- Add index on user_id in expense_participants
CREATE INDEX idx_participant ON expense_participants (user_id);

-- Add index on email in users table
CREATE INDEX idx_user_email ON users (email);
