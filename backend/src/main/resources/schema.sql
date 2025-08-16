-- optional explicit schema; JPA is set to update
-- provided for reference/testing
-- Core tables
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    sku VARCHAR(255),
    quantity INTEGER,
    selling_price DECIMAL(10,2),
    order_date_time TIMESTAMP,
    product_name TEXT,
    customer_state VARCHAR(255),
    size VARCHAR(255),
    supplier_listed_price DECIMAL(10,2),
    supplier_discounted_price DECIMAL(10,2),
    packet_id VARCHAR(255),
    reason_for_credit_entry TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(255) NOT NULL,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    payment_date_time TIMESTAMP NOT NULL,
    order_status VARCHAR(255),
    transaction_id VARCHAR(255),
    final_settlement_amount DECIMAL(10,2),
    price_type VARCHAR(255),
    total_sale_amount DECIMAL(10,2),
    total_sale_return_amount DECIMAL(10,2),
    fixed_fee DECIMAL(10,2),
    warehousing_fee DECIMAL(10,2),
    return_premium DECIMAL(10,2),
    meesho_commission_percentage DECIMAL(5,2),
    meesho_commission DECIMAL(10,2),
    meesho_gold_platform_fee DECIMAL(10,2),
    meesho_mall_platform_fee DECIMAL(10,2),
    return_shipping_charge DECIMAL(10,2),
    gst_compensation DECIMAL(10,2),
    shipping_charge DECIMAL(10,2),
    other_support_service_charges DECIMAL(10,2),
    waivers DECIMAL(10,2),
    net_other_support_service_charges DECIMAL(10,2),
    gst_on_net_other_support_service_charges DECIMAL(10,2),
    tcs DECIMAL(10,2),
    tds_rate_percentage DECIMAL(5,2),
    tds DECIMAL(10,2),
    compensation DECIMAL(10,2),
    compensation_reason TEXT,
    claims DECIMAL(10,2),
    claims_reason TEXT,
    recovery DECIMAL(10,2),
    recovery_reason TEXT,
    dispatch_date DATE,
    product_gst_percentage DECIMAL(5,2),
    listing_price_incl_taxes DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sku_prices (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    purchase_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SKU Groups table for grouping SKUs and applying group-level pricing
CREATE TABLE IF NOT EXISTS sku_groups (
    id BIGSERIAL PRIMARY KEY,
    group_name VARCHAR(255) NOT NULL UNIQUE,
    purchase_price DECIMAL(10,2) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SKU Group Mappings table to link SKUs to groups
CREATE TABLE IF NOT EXISTS sku_group_mappings (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    group_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES sku_groups(id) ON DELETE CASCADE
);

-- Staging tables for file ingestion
CREATE TABLE IF NOT EXISTS orders_raw (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    row_number INTEGER NOT NULL,
    raw_data TEXT,
    validation_status VARCHAR(50) DEFAULT 'PENDING',
    validation_errors TEXT,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payments_raw (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    row_number INTEGER NOT NULL,
    raw_data TEXT,
    validation_status VARCHAR(50) DEFAULT 'PENDING',
    validation_errors TEXT,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_orders_order_id ON orders(order_id);
CREATE INDEX IF NOT EXISTS idx_orders_sku ON orders(sku);
CREATE INDEX IF NOT EXISTS idx_orders_order_date_time ON orders(order_date_time);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_payment_date_time ON payments(payment_date_time);
CREATE INDEX IF NOT EXISTS idx_sku_prices_sku ON sku_prices(sku);
CREATE INDEX IF NOT EXISTS idx_sku_group_mappings_sku ON sku_group_mappings(sku);
CREATE INDEX IF NOT EXISTS idx_sku_group_mappings_group_id ON sku_group_mappings(group_id);
CREATE INDEX IF NOT EXISTS idx_sku_groups_group_name ON sku_groups(group_name);

-- Staging table indexes
CREATE INDEX IF NOT EXISTS idx_orders_raw_batch_id ON orders_raw(batch_id);
CREATE INDEX IF NOT EXISTS idx_orders_raw_validation_status ON orders_raw(validation_status);
CREATE INDEX IF NOT EXISTS idx_payments_raw_batch_id ON payments_raw(batch_id);
CREATE INDEX IF NOT EXISTS idx_payments_raw_validation_status ON payments_raw(validation_status);


