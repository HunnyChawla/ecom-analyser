-- Add quantity column to payments table
ALTER TABLE payments ADD COLUMN quantity INTEGER;

-- Update existing payments to have quantity = 1 as default (since we can't determine the actual quantity)
UPDATE payments SET quantity = 1 WHERE quantity IS NULL;

-- Make the column NOT NULL after setting default values
ALTER TABLE payments ALTER COLUMN quantity SET NOT NULL;
