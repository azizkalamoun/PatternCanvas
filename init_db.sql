CREATE DATABASE IF NOT EXISTS dessin;
USE dessin;

-- Table for original save/load (single drawing)
CREATE TABLE IF NOT EXISTS shapes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    x1 DOUBLE NOT NULL,
    y1 DOUBLE NOT NULL,
    x2 DOUBLE NOT NULL,
    y2 DOUBLE NOT NULL,
    color VARCHAR(50) DEFAULT 'RGB(0,0,0)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for DatabaseLogger
CREATE TABLE IF NOT EXISTS log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for named drawings (multiple drawings)
CREATE TABLE IF NOT EXISTS drawings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for shapes within named drawings
CREATE TABLE IF NOT EXISTS drawing_shapes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    drawing_id INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    x1 DOUBLE NOT NULL,
    y1 DOUBLE NOT NULL,
    x2 DOUBLE NOT NULL,
    y2 DOUBLE NOT NULL,
    color VARCHAR(50) DEFAULT 'RGB(0,0,0)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (drawing_id) REFERENCES drawings(id) ON DELETE CASCADE
);
