-- optional explicit schema; JPA is set to update
-- provided for reference/testing
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    sku VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    selling_price DECIMAL(10,2) NOT NULL,
    order_date_time TIMESTAMP NOT NULL,
    product_name TEXT,
    customer_state VARCHAR(100),
    size VARCHAR(50),
    supplier_listed_price DECIMAL(10,2),
    supplier_discounted_price DECIMAL(10,2),
    packet_id VARCHAR(255),
    reason_for_credit_entry VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(255) NOT NULL,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    payment_date_time TIMESTAMP NOT NULL,
    order_status VARCHAR(100) NOT NULL,
    transaction_id VARCHAR(255),
    final_settlement_amount DECIMAL(10,2),
    price_type VARCHAR(100),
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
    claims DECIMAL(10,2),
    recovery DECIMAL(10,2),
    compensation_reason TEXT,
    claims_reason TEXT,
    recovery_reason TEXT,
    dispatch_date DATE,
    product_gst_percentage DECIMAL(5,2),
    listing_price_incl_taxes DECIMAL(10,2)
);

CREATE TABLE IF NOT EXISTS sku_prices (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    purchase_price DECIMAL(10,2) NOT NULL,
    updated_at TIMESTAMP NOT NULL
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

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_sku_group_mappings_sku ON sku_group_mappings(sku);
CREATE INDEX IF NOT EXISTS idx_sku_group_mappings_group_id ON sku_group_mappings(group_id);
CREATE INDEX IF NOT EXISTS idx_sku_groups_group_name ON sku_groups(group_name);


