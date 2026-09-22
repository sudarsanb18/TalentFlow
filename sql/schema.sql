-- =========================================================
-- TalentFlow Recruitment and Applicant Tracking System
-- MySQL database schema for JDBC integration
-- =========================================================

CREATE DATABASE IF NOT EXISTS talentflow_db;
USE talentflow_db;

CREATE TABLE IF NOT EXISTS recruiter (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    company VARCHAR(150) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS candidate (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    skill VARCHAR(100) NOT NULL,
    experience DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    status VARCHAR(50) DEFAULT 'Available',
    resume_summary VARCHAR(1000) DEFAULT 'No profile summary provided',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job (
    job_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    company VARCHAR(150) NOT NULL,
    location VARCHAR(100) NOT NULL,
    salary_range VARCHAR(100) DEFAULT 'Not Disclosed',
    required_skill VARCHAR(100) NOT NULL,
    recruiter_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recruiter_id) REFERENCES recruiter(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS application (
    application_id INT AUTO_INCREMENT PRIMARY KEY,
    candidate_id INT NOT NULL,
    job_id INT NOT NULL,
    status ENUM('Applied', 'Interview', 'Selected', 'Rejected', 'Withdrawn') DEFAULT 'Applied',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (candidate_id) REFERENCES candidate(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES job(job_id) ON DELETE CASCADE,
    CONSTRAINT unique_candidate_job UNIQUE (candidate_id, job_id)
);

CREATE INDEX idx_candidate_skill ON candidate(skill);
CREATE INDEX idx_job_skill ON job(required_skill);
CREATE INDEX idx_application_status ON application(status);

-- Safe upgrades for databases created by an earlier TalentFlow version.

ALTER TABLE application MODIFY COLUMN status ENUM('Applied', 'Interview', 'Selected', 'Rejected', 'Withdrawn') DEFAULT 'Applied';
