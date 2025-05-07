-- V1__create_initial_tables.sql
CREATE TABLE users (
                       user_id BINARY(16) NOT NULL,
                       login VARCHAR(50) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       first_name VARCHAR(100) NOT NULL,
                       birth_date DATE NOT NULL,
                       PRIMARY KEY (user_id),
                       UNIQUE (login)
);

CREATE TABLE user_emails (
                             id BIGINT AUTO_INCREMENT,
                             email VARCHAR(255) NOT NULL,
                             user_id BINARY(16) NOT NULL,
                             PRIMARY KEY (id),
                             UNIQUE (email),
                             FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE user_phone_numbers (
                                    id BIGINT AUTO_INCREMENT,
                                    phone_number VARCHAR(20) NOT NULL,
                                    user_id BINARY(16) NOT NULL,
                                    PRIMARY KEY (id),
                                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);