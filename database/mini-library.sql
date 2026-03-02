CREATE DATABASE IF NOT EXISTS mini_library;
USE mini_library;

-- Stores login credentials
CREATE TABLE IF NOT EXISTS users (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- Stores both Regular Books and Textbooks in one table.
-- grade_level is NULL for Regular Books, and filled for Textbooks.
CREATE TABLE IF NOT EXISTS books (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    type        VARCHAR(20)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    pages       INT          NOT NULL,
    grade_level INT          DEFAULT NULL
);

-- Seed two demo users (plain-text for now; see Step 5 for hashing)
INSERT INTO users (username, password) VALUES
    ('admin', 'password'),
    ('user',  '1234');