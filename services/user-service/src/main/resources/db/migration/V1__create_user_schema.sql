CREATE TABLE IF NOT EXISTS user_profiles (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_id      VARCHAR(36)  UNIQUE NOT NULL,   -- references auth-service user ID
    full_name    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) UNIQUE NOT NULL,
    phone        VARCHAR(20),
    avatar_url   VARCHAR(500),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_addresses (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    label          VARCHAR(50)  NOT NULL DEFAULT 'HOME',   -- HOME, WORK, OTHER
    recipient_name VARCHAR(255) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    street         VARCHAR(500) NOT NULL,
    ward           VARCHAR(100),
    district       VARCHAR(100) NOT NULL,
    province       VARCHAR(100) NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_auth_id  ON user_profiles (auth_id);
CREATE INDEX idx_address_user  ON user_addresses (user_id);
