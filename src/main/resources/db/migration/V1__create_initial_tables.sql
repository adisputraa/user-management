CREATE TABLE users (
    id          UUID         NOT NULL,
    username    VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    keycloak_id VARCHAR(255) NOT NULL,
    role        VARCHAR(32)  NOT NULL CHECK (role IN ('ADMIN', 'CUSTOMER', 'MERCHANT')),
    status      VARCHAR(32)  NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_keycloak_id UNIQUE (keycloak_id)
);

-- One-to-one with users: the profile shares the user's primary key.
CREATE TABLE user_profiles (
    id           UUID         NOT NULL,
    first_name   VARCHAR(255) NOT NULL,
    last_name    VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32),
    address      VARCHAR(255),
    CONSTRAINT pk_user_profiles PRIMARY KEY (id),
    CONSTRAINT fk_user_profiles_users FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_users_created_at ON users (created_at);
