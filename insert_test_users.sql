-- Insert test users for demo
-- These users are for testing the 3-role refactor

INSERT INTO users (username, password_hash, role, status, created_at) VALUES
('admin', 'admin123', 'ADMIN', 'ACTIVE', NOW()),
('doctor', 'doc123', 'DOCTOR', 'ACTIVE', NOW()),
('receptionist', 'rec123', 'RECEPTIONIST', 'ACTIVE', NOW()),
('patient', 'pat123', 'PATIENT', 'ACTIVE', NOW())
ON CONFLICT (username) DO NOTHING;

-- Insert a sample doctor profile
INSERT INTO doctors (user_id, full_name, specialty, phone, created_at) 
SELECT id, 'BS Nguyễn Thị Hoa', 'Khám tổng quát', '0912345678', NOW()
FROM users WHERE username = 'doctor'
ON CONFLICT (user_id) DO NOTHING;

-- Verify insertion
SELECT 'Test data inserted successfully' as status;
SELECT COUNT(*) as user_count FROM users;
