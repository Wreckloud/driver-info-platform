CREATE TABLE driver_record_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    driver_record_id BIGINT NOT NULL,
    storage_name VARCHAR(100) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    display_order TINYINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_driver_record_photo_record
        FOREIGN KEY (driver_record_id) REFERENCES driver_record (id),
    UNIQUE KEY uk_driver_record_photo_storage_name (storage_name),
    KEY idx_driver_record_photo_record_order (driver_record_id, display_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
