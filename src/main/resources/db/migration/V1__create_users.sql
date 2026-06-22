CREATE TABLE users (
  id UUID PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  manager_id UUID,
  annual_entitlement INT NOT NULL,
  is_manager BOOLEAN NOT NULL DEFAULT false,
  CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES users(id)
);
