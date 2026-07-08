CREATE TABLE users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_premium BOOLEAN NOT NULL,
    premium_expires_at TIMESTAMP WITH TIME ZONE,
    coins INTEGER NOT NULL,
    gems INTEGER NOT NULL,
    country VARCHAR(2),
    birth_date DATE,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
)