CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users
(
    id         UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    username   VARCHAR(64)                            NOT NULL,
    password   VARCHAR(64)                            NOT NULL,
    role       VARCHAR(64)                            NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

