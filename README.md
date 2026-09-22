# TalentFlow - Recruitment & Applicant Tracking System (ATS)

**TalentFlow** is a enterprise-structured Recruitment and Applicant Tracking System (ATS) developed using Core Java 21. It provides an end-to-end console solution for recruiters to post jobs, manage candidate profiles, process job applications, and transition candidates through interview and selection workflows.

---

## 📌 Project Overview
- **Project Title:** TalentFlow ATS
- **Language:** Core Java (JDK 21)
- **Architecture:** Layered Architecture (Model - Repository - Service - Utility - UI Menu)
- **Persistence:** MySQL database accessed through JDBC, with file-based Audit Logging (`data/logs.txt`) and CSV export.
- **Dependencies:** Core Java 21 + MySQL Connector/J. No external application framework is used.

---

## 🎯 Objectives
1. Provide recruiters with a command-line tool to manage hiring pipelines efficiently.
2. Demonstrate mastery over core Object-Oriented Programming (OOP) concepts for College Review II and Viva presentation.
3. Ensure defensive program execution where system errors (`InputMismatchException`, `NullPointerException`) are handled cleanly without crashing.

---

## ✨ Features
- **Candidate Management:** Add, view tabular candidates list, search by ID, search by Name (overloaded), update details, and delete candidate records.
- **Recruiter Management:** Register recruiters with company mapping and view recruiters list.
- **Job Management:** Post jobs with skill requirements, search by Job ID or Job Title (overloaded), update job postings, and delete job openings.
- **Applicant Tracking Workflow:** Apply candidates to active jobs, view full application records with candidate and job names, and transition statuses (`Applied` ➡️ `Interview` ➡️ `Selected` / `Rejected`).
- **Audit Logging:** Automatically log system operations to `data/logs.txt` using `BufferedWriter` and inspect logs on program exit via `BufferedReader`.
- **System Metrics:** Real-time totals for candidate count, recruiter count, and job count.

---

## 📁 Folder Structure

```
TalentFlow/
├── src/
│   ├── model/
│   │   ├── User.java             (Abstract base class)
│   │   ├── Candidate.java        (Subclass of User)
│   │   ├── Recruiter.java        (Subclass of User)
│   │   ├── Job.java              (Job opening model)
│   │   └── Application.java      (Application workflow model)
│   ├── repository/
│   │   ├── CandidateRepository.java   (JDBC + MySQL CRUD)
│   │   ├── RecruiterRepository.java   (JDBC + MySQL CRUD)
│   │   ├── JobRepository.java         (JDBC + MySQL CRUD)
│   │   └── ApplicationRepository.java (JDBC + MySQL CRUD)
│   ├── service/
│   │   ├── CandidateService.java      (Candidate business logic)
│   │   ├── RecruiterService.java      (Recruiter business logic)
│   │   ├── JobService.java            (Job posting business logic)
│   │   └── ApplicationService.java    (Application workflow business logic)
│   ├── util/
│   │   ├── Validation.java            (Email, Name, Skill, Exp regex validations)
│   │   └── FileManager.java           (File I/O logs reader & writer)
│   └── menu/
│       ├── Menu.java                  (Interactive CLI UI menu & exception handling)
│       └── Main.java                  (Application entry point)
├── data/
│   └── logs.txt                   (Audit trail log file)
├── sql/
│   └── schema.sql                 (MySQL database schema DDL script)
└── README.md                      (Project documentation)
```

---

## 🛠️ OOP Concepts Implemented

| OOP Concept | Implementation Details |
| :--- | :--- |
| **Abstraction** | `User.java` defined as `public abstract class` with `public abstract void displayDetails()`. |
| **Inheritance** | `Candidate` and `Recruiter` classes extend `User`. |
| **Encapsulation** | All fields are strictly `private` and accessible via public Getters and Setters. |
| **Polymorphism (Overriding)** | Subclasses override `displayDetails()` and `toString()`. |
| **Polymorphism (Overloading)** | `CandidateService` and `JobService` implement `searchCandidate(int)` / `searchCandidate(String)` and `searchJob(int)` / `searchJob(String)`. |
| **Static Members** | Static utility/configuration members are retained; persistent entity state is stored in MySQL. |
| **StringBuilder** | `StringBuilder` utilized in service layers for tabular presentation. |
| **Exception Handling** | Guarded inputs with `InputMismatchException`, `IllegalArgumentException`, and `NullPointerException` recovery in `Menu.java`. |

---

## 🚀 How to Compile and Run

### 1. Configure MySQL
Run `sql/schema.sql` in MySQL Workbench or the MySQL client. Then open `src/util/DBConnection.java` and set your MySQL username/password.

### 2. Add MySQL Connector/J
Place the MySQL Connector/J JAR in `lib/` with the filename `mysql-connector-j-26.7.0.jar`, or update `.classpath` to match the JAR you downloaded.

### 3. Open Terminal in Project Root
Navigate to the root directory `TalentFlow/`:
```bash
cd TalentFlow
```

### 4. Compile All Source Files
Compile all Java packages into a `bin/` directory:
```bash
javac -d bin src/model/*.java src/repository/*.java src/util/*.java src/service/*.java src/menu/*.java
```

### 5. Run the Application
Execute the compiled `Main` entry point:
```bash
java -cp bin menu.Main
```

---

## 🔮 Future Enhancements
1. **JDBC / MySQL Integration:** Connect Repositories to relational tables using `sql/schema.sql`.
2. **REST API / Web Frontend:** Wrap services with Spring Boot or Javalin REST controllers and connect a Vue/React dashboard.
3. **Resume File Upload:** Support candidate PDF resume parsing and document storage.
