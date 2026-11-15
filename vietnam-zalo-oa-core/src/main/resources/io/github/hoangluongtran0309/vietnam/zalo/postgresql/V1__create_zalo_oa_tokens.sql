CREATE TABLE IF NOT EXISTS zalo_oa_tokens (
    oa_id VARCHAR(128) PRIMARY KEY,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    access_token_expires_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
