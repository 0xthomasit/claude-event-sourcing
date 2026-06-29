-- Seed admin user for development
-- Password: Admin@123456 (BCrypt hash)
INSERT INTO users (id, email, password_hash, full_name, enabled)
VALUES (
    'aaaaaaaa-0000-0000-0000-000000000001',
    'admin@example.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/HS.iK8i',
    'System Admin',
    TRUE
) ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role)
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'ADMIN')
ON CONFLICT DO NOTHING;