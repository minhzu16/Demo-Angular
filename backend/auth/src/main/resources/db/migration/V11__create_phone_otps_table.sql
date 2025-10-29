-- Sprint 10 - Phone OTP Verification
-- Create phone_otps table

CREATE TABLE phone_otps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(15) NOT NULL COMMENT 'Phone number',
    otp_code VARCHAR(6) NOT NULL COMMENT 'OTP code (6 digits)',
    purpose VARCHAR(20) NOT NULL COMMENT 'REGISTER, LOGIN, VERIFY, RESET',
    expires_at TIMESTAMP NOT NULL COMMENT 'Expiration time',
    verified BOOLEAN DEFAULT FALSE COMMENT 'Whether OTP has been verified',
    attempts INT DEFAULT 0 COMMENT 'Number of verification attempts',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    
    INDEX idx_phone_purpose (phone, purpose),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add phone verification fields to users table
ALTER TABLE users 
ADD COLUMN phone VARCHAR(15) NULL COMMENT 'Phone number',
ADD COLUMN phone_verified BOOLEAN DEFAULT FALSE COMMENT 'Phone verification status',
ADD COLUMN phone_verified_at TIMESTAMP NULL COMMENT 'Phone verification timestamp';

-- Add index for phone lookup
CREATE INDEX idx_users_phone ON users(phone);
