-- V2__add_role_tables.sql
CREATE TABLE roles (
                       role_id BINARY(16) NOT NULL,
                       name VARCHAR(50) NOT NULL,
                       description VARCHAR(255) NOT NULL,
                       PRIMARY KEY (role_id),
                       UNIQUE (name)
);

CREATE TABLE role_permissions (
                                  role_id BINARY(16) NOT NULL,
                                  permission VARCHAR(100) NOT NULL,
                                  PRIMARY KEY (role_id, permission),
                                  FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
                            user_id BINARY(16) NOT NULL,
                            role_id BINARY(16) NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);