CREATE TABLE users (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    email VARCHAR(255) NOT NULL,
    keycloak_id VARCHAR(255),
    role VARCHAR(255) CHECK (role IN ('ADMIN','CUSTOMER','MITRA')),
    status VARCHAR(255) CHECK (status IN ('ACTIVE','INACTIVE')),
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    username VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE user_profiles (
    id UUID NOT NULL,
    address VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone_number VARCHAR(255),
    PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS users
    ADD CONSTRAINT UK_email UNIQUE (email);

ALTER TABLE IF EXISTS users
    ADD CONSTRAINT UK_keycloak_id UNIQUE (keycloak_id);

ALTER TABLE IF EXISTS users
    ADD CONSTRAINT UK_username UNIQUE (username);

ALTER TABLE IF EXISTS user_profiles
    ADD CONSTRAINT FK_user_profiles_users
    FOREIGN KEY (id)
    REFERENCES users;