-- Sprint 10 - COD Payment
-- Add payment fields to orders table

ALTER TABLE orders 
ADD COLUMN payment_method VARCHAR(20) DEFAULT 'COD' COMMENT 'Payment method: COD, BANK_TRANSFER, VNPAY, MOMO, ZALOPAY',
ADD COLUMN payment_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'Payment status: PENDING, PAID, FAILED, REFUNDED, CANCELLED',
ADD COLUMN paid_at TIMESTAMP NULL COMMENT 'Timestamp when payment was completed';

-- Add indexes for payment queries
CREATE INDEX idx_orders_payment_status ON orders(payment_status);
CREATE INDEX idx_orders_payment_method ON orders(payment_method);
CREATE INDEX idx_orders_payment_method_status ON orders(payment_method, payment_status);

-- Update existing orders to have COD payment method
UPDATE orders 
SET payment_method = 'COD', 
    payment_status = 'PENDING'
WHERE payment_method IS NULL;
