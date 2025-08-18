-- Create return_tracking table for tracking return orders
CREATE TABLE IF NOT EXISTS return_tracking (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    sku_id VARCHAR(255),
    quantity INTEGER,
    return_amount DECIMAL(10,2),
    order_status VARCHAR(100),
    order_date DATE,
    return_status VARCHAR(50) DEFAULT 'PENDING_RECEIPT',
    received_date TIMESTAMP,
    received_by VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_return_tracking_order_id ON return_tracking(order_id);
CREATE INDEX IF NOT EXISTS idx_return_tracking_sku_id ON return_tracking(sku_id);
CREATE INDEX IF NOT EXISTS idx_return_tracking_order_date ON return_tracking(order_date);
CREATE INDEX IF NOT EXISTS idx_return_tracking_return_status ON return_tracking(return_status);
CREATE INDEX IF NOT EXISTS idx_return_tracking_order_status ON return_tracking(order_status);

-- Add comments to table and columns
COMMENT ON TABLE return_tracking IS 'Tracks return orders and their receipt status';
COMMENT ON COLUMN return_tracking.order_id IS 'Unique order identifier';
COMMENT ON COLUMN return_tracking.sku_id IS 'Stock keeping unit identifier';
COMMENT ON COLUMN return_tracking.quantity IS 'Quantity of items returned';
COMMENT ON COLUMN return_tracking.return_amount IS 'Amount refunded/returned';
COMMENT ON COLUMN return_tracking.order_status IS 'Original order status (RETURN, RTO, etc.)';
COMMENT ON COLUMN return_tracking.order_date IS 'Date when the order was placed';
COMMENT ON COLUMN return_tracking.return_status IS 'Current return tracking status (PENDING_RECEIPT, RECEIVED, NOT_RECEIVED)';
COMMENT ON COLUMN return_tracking.received_date IS 'Date when the return was physically received';
COMMENT ON COLUMN return_tracking.received_by IS 'Name/ID of person who received the return';
COMMENT ON COLUMN return_tracking.notes IS 'Additional notes about the return';
COMMENT ON COLUMN return_tracking.created_at IS 'Timestamp when tracking record was created';
COMMENT ON COLUMN return_tracking.updated_at IS 'Timestamp when tracking record was last updated';
