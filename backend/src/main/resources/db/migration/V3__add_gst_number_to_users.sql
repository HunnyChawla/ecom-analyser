-- Migration: Add GST number column to users table
-- Version: V3
-- Description: Adds mandatory GST number field for user registration

-- Add GST number column
ALTER TABLE users ADD COLUMN gst_number VARCHAR(15) NOT NULL;

-- Add unique constraint on GST number
ALTER TABLE users ADD CONSTRAINT uk_users_gst_number UNIQUE (gst_number);

-- Add index for better performance on GST number lookups
CREATE INDEX idx_users_gst_number ON users(gst_number);

-- Add comment for documentation
COMMENT ON COLUMN users.gst_number IS 'GST number - mandatory 15 character unique identifier';
