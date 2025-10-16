-- MySQL schema for worktime project

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS process_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    content VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS worker (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    workshop VARCHAR(255),
    team VARCHAR(255),
    entry_date DATE,
    leave_date DATE
);

CREATE TABLE IF NOT EXISTS uploaded_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255),
    data LONGBLOB,
    upload_time DATETIME
);

CREATE TABLE IF NOT EXISTS work_time (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_date DATE,
    end_date DATE
);

CREATE TABLE IF NOT EXISTS work_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process VARCHAR(255),
    hours DOUBLE,
    work_time_id BIGINT,
    CONSTRAINT fk_work_step_time FOREIGN KEY (work_time_id)
        REFERENCES work_time(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS work_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_number VARCHAR(255),
    product_name VARCHAR(255),
    drawing_number VARCHAR(255),
    product_code VARCHAR(255),
    part_name VARCHAR(255),
    plan_qty INT,
    process_name VARCHAR(255),
    process_code VARCHAR(255),
    barcode VARCHAR(255),
    barcode_image LONGBLOB,
    batch_number VARCHAR(255),
    hours DOUBLE,
    worker_codes VARCHAR(255),
    worker_names VARCHAR(255),
    worker_qtys VARCHAR(255),
    qualified_qty INT,
    hour_subtotal DOUBLE,
    filled BOOLEAN,
    start_time DATETIME,
    end_time DATETIME,
    inspector VARCHAR(255),
    remark1 VARCHAR(255),
    remark2 VARCHAR(255),
    file_id BIGINT,
    supplemental BOOLEAN,
    CONSTRAINT fk_work_record_file FOREIGN KEY (file_id)
        REFERENCES uploaded_file(id)
);
