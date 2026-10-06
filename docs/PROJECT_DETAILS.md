# TalentFlow – Complete Project Details

**TalentFlow: A Recruitment and Applicant Tracking System using Core Java and JDBC**

| | |
|---|---|
| Team | Bharathvaj S (2104251040148), Sudarsan B (2104251040196) – B.E. CSE, Section N |
| Institution | Chennai Institute of Technology (Autonomous), affiliated to Anna University, Chennai |
| Course | Java Programming – Project-Based Learning (PBL), 2026–2027 |
| PBL supervisor / Java staff | Ms. L. Swathi, Assistant Professor |
| Class advisors | Mr. R. Raja and Ms. L. Swathi, Assistant Professors |
| Project co-ordinator | Dr. R. Kavitha, M.E., Ph.D., Associate Professor |
| Head of Department | Dr. S. Pavithra |
| Repository | https://github.com/sudarsanb18/TalentFlow |

---

## 1. What the project does

Small recruitment teams usually track jobs, candidates and application statuses in spreadsheets and email. That leads to duplicate records, lost status updates and no single view of who is at which stage.

TalentFlow is a **console-based Applicant Tracking System (ATS)**. It has three role-based portals – **Admin, Recruiter and Candidate** – plus a public "register and apply" flow. Data is stored in **MySQL** through **JDBC**.

### What makes TalentFlow different

Another batch built a similar project with the same admin/recruiter/candidate workflow and a single skill-match percentage. TalentFlow goes further:

1. **Explainable weighted match %** – required skills (70%) and preferred skills (30%), scaled by minimum experience, and it shows *which* skills are missing.
2. **Per-job match rules** – each job can have its own preferred skills and minimum experience.
3. **Recruiter-only match preview** – the recruiter sees every candidate ranked by match % for a job *before* anyone is applied. Candidates never see scores.
4. **Weighted matching engine with a cut-off** – auto-applies only candidates at or above a chosen percentage.
5. **Ranked shortlist** – applicants ordered by match %, then experience, then who applied first; top N moved to Interview in one step; exported to CSV.
6. **Audit trail** – every match, auto-application, shortlist and export is written to `data/logs.txt` with a timestamp.

---

## 2. Technology

| Item | Used |
|---|---|
| Language | Core Java (JDK 17 or 21; uses records and switch expressions) |
| Database | MySQL 8 through JDBC (`java.sql`: `DriverManager`, `PreparedStatement`, `ResultSet`) |
| Driver | MySQL Connector/J (`lib/mysql-connector-j-26.7.0.jar`) – the only external library |
| Interface | Console (Scanner input, formatted text tables) |
| IDE | Eclipse (`.classpath` included) / VS Code |
| Version control | Git + GitHub |

No Spring, Hibernate, GUI or other framework is used.

---

## 3. Architecture

Layered (N-tier) design. Each layer only talks to the one below it; all SQL lives in the repository layer.

```
User (console)
   │
   ▼
menu/        Presentation  – Main, Menu: portals, input reading, re-prompting on bad input
   │
   ▼
service/     Business rules – validation, ownership checks, workflow, matching, ranking, logging
   │
   ▼
repository/  Data access   – JDBC with PreparedStatement + try-with-resources
   │
   ▼
MySQL (talentflow_db)

model/  plain data classes used by every layer
util/   DBConnection, Validation, FileManager (audit log + CSV)
```

### Source files

| File | Lines | Purpose |
|---|---:|---|
| `src/menu/Main.java` | 41 | Entry point; switches the console to UTF-8 so ✅ ❌ ⚠️ print correctly |
| `src/menu/Menu.java` | 625 | All console menus and input helpers |
| `src/model/User.java` | 128 | Abstract base class (id, name, email, phone, password) |
| `src/model/Candidate.java` | 134 | Candidate extends User (skill, experience, status, resume summary) |
| `src/model/Recruiter.java` | 69 | Recruiter extends User (company) |
| `src/model/Job.java` | 165 | Job posting |
| `src/model/Application.java` | 121 | Candidate–job application with status |
| `src/repository/CandidateRepository.java` | 168 | Candidate CRUD (JDBC) |
| `src/repository/RecruiterRepository.java` | 105 | Recruiter CRUD (JDBC) |
| `src/repository/JobRepository.java` | 137 | Job CRUD (JDBC) |
| `src/repository/ApplicationRepository.java` | 145 | Application CRUD (JDBC) |
| `src/repository/JobMatchRepository.java` | 91 | Job match rules table; creates it automatically |
| `src/service/CandidateService.java` | 270 | Candidate rules, search (overloaded), update, delete |
| `src/service/RecruiterService.java` | 136 | Recruiter registration and login |
| `src/service/JobService.java` | 227 | Job posting, search (overloaded), company-safe update/delete |
| `src/service/ApplicationService.java` | 316 | Apply, withdraw, status workflow, views, classic matching engine |
| `src/service/MatchScorer.java` | 85 | Weighted match % (pure functions, no database) |
| `src/service/RankedApplicant.java` | 69 | Ranking order, cut-off, top-N, CSV writer |
| `src/service/SmartMatchService.java` | 293 | Match rules, weighted engine, preview, ranked shortlist, export |
| `src/util/DBConnection.java` | 22 | JDBC connection (URL, user, password) |
| `src/util/Validation.java` | 116 | Regex validation (email, phone, password, name), experience, candidate status |
| `src/util/FileManager.java` | 333 | Audit log (BufferedWriter/Reader) and CSV export |
| `test/TalentFlowTests.java` | 333 | Custom test runner, 78 tests |
| `sql/schema.sql` | 61 | MySQL schema |

### OOP concepts used

| Concept | Where |
|---|---|
| Abstraction | `User` is abstract with abstract `displayDetails()` |
| Inheritance | `Candidate` and `Recruiter` extend `User` |
| Encapsulation | All model fields private with getters/setters |
| Overriding | `displayDetails()` and `toString()` in `Candidate`, `Recruiter` |
| Overloading | `searchJob(int)` / `searchJob(String)`, `searchCandidate(int)` / `searchCandidate(String)`, two `addJob` methods, `updateJob` / `deleteJob` with and without company, `runAutomatedSkillMatching()` / `(String company)` |
| Records | `MatchScorer.Result`, `RankedApplicant`, `JobMatchRepository.JobRule` |
| Exception handling | `InputMismatchException` re-prompt loops, `SQLException` handling in every repository, `IOException` in CSV export |
| Collections | `ArrayList`, `List`, `Map`, `Set`, `Comparator` |
| File I/O | `BufferedWriter` / `BufferedReader` audit log, CSV export |

---

## 4. Database (`talentflow_db`)

| Table | Columns | Rules |
|---|---|---|
| `recruiter` | id, name, email, phone, password, company, created_at | PK id; email UNIQUE |
| `candidate` | id, name, email, phone, password, skill, experience, status, resume_summary, created_at | PK id; email UNIQUE; index on skill |
| `job` | job_id, title, company, location, salary_range, required_skill, recruiter_id, created_at | PK job_id; FK recruiter_id → recruiter (ON DELETE SET NULL); index on required_skill |
| `application` | application_id, candidate_id, job_id, status, applied_at | PK; FKs → candidate, job (ON DELETE CASCADE); UNIQUE(candidate_id, job_id); status ENUM('Applied','Interview','Selected','Rejected','Withdrawn') |
| `job_match_rule` | job_id, preferred_skills, min_experience | PK job_id; FK → job (ON DELETE CASCADE). **Created automatically by the app** on first run – no SQL needs to be run by hand |

ID ranges: recruiters from 1, jobs from 101, candidates from 501, applications from 1001.

---

## 5. Menus and features

### Main menu
```
1  Admin
2  Recruiter
3  Candidate
4  Apply / Register as New Candidate
5  Exit   (shows the audit log before closing)
```

### Admin portal (password protected)
| Option | Feature |
|---|---|
| 1 | Add Recruiter – validated name, email, phone, strong password, company; duplicate email refused |
| 2 | View Recruiter Directory |
| 3 | Master Admin Dashboard – totals, all recruiters, jobs, candidates, active applications, audit log; optional CSV export to `data/ats_export_report.csv` |
| 4 | Back |

### Recruiter portal (login with recruiter ID + password)
| Option | Feature |
|---|---|
| 1 | Add Job (title, location, salary range, required skills – comma separated) |
| 2 | View Jobs |
| 3 | Search Job by ID or by title |
| 4 | Update Job – **own company's jobs only**; blank input keeps the old value |
| 5 | Delete Job – **own company's jobs only**; its applications are removed too |
| 6 | View Candidates |
| 7 | Search Candidate by ID |
| 8 | Search Candidate by name |
| – | (Option 9 removed: recruiters cannot edit candidate details; only the candidate can, from the Candidate portal) |
| 10 | Delete Candidate Profile |
| 11 | View Applications – active applications only (withdrawn ones are hidden) |
| 12 | Update Application Status – **only Interview, Selected, Rejected**; withdrawn applications cannot be changed |
| 13 | Run Skill Matching Engine (classic) – own company's jobs, whole-skill match, auto-applies matches |
| 15 | **Set Job Match Rules** – preferred skills and minimum experience for one of your jobs |
| 16 | **Run Weighted Matching Engine** – asks for a cut-off (default 60%), shows each candidate's match % and missing skills, auto-applies those at or above the cut-off |
| 17 | **Ranked Shortlist for a Job** – applicants ranked by match %; shortlist top N to Interview and/or export to `data/shortlist_job_<id>.csv` |
| 18 | **Preview Candidate Matches for a Job** – every candidate ranked by match % with missing skills and application status; creates nothing |
| 14 | Back to Main Menu |

### Candidate portal (login with candidate ID + password)
| Option | Feature |
|---|---|
| 1 | View My Applications & Statuses (withdrawn ones hidden) |
| 2 | View Available Job Openings (no match % – scores are recruiter-only) |
| 3 | Update My Profile – skill, experience, status |
| 4 | Withdraw My Application – own applications only; not twice; not after Selected/Rejected |
| 5 | Delete My Account (asks yes/no; removes the candidate and their applications) |
| 6 | Back |

### Apply / Register as New Candidate
Shows the jobs, asks for a job ID, then name, email, phone, password, skills, experience and a short summary. Every field is re-asked until it is valid. A candidate ID is generated and the application is created in one step.

---

## 6. Smart Match – how the percentage works

Implemented in `MatchScorer` (pure Java, no database, fully unit-tested).

```
requiredScore  = matched required skills / total required skills
preferredScore = matched preferred skills / total preferred skills
base           = 0.70 × requiredScore + 0.30 × preferredScore   (job has preferred skills)
               = requiredScore                                    (job has no preferred skills)
expFactor      = min(1, candidate experience / minimum experience)  (1 when no minimum)
match %        = round(base × expFactor × 100)                     always 0–100
```

Skill rules:
- Skills are separated by comma, `&` or `/` – `React & Java` is two skills.
- Comparison is case-insensitive and by **whole skill**: `Java` never matches `JavaScript`.
- Extra spaces, empty items and duplicates are ignored.

Worked examples (each one is an automated test):

| Candidate skills | Exp | Job required | Job preferred | Min exp | Match |
|---|---|---|---|---|---:|
| Java, SQL | 3 | Java, SQL | – | 0 | **100%** |
| Java | 3 | Java, SQL | – | 0 | **50%** |
| Python | 3 | Java, SQL | – | 0 | **0%** |
| JavaScript | 3 | Java | – | 0 | **0%** |
| Java | 3 | Java | Docker, AWS | 0 | **70%** |
| Java, Docker | 3 | Java | Docker, AWS | 0 | **85%** |
| Java | 2 | Java | – | 4 | **50%** |
| React & Java | 5 | Java | – | 0 | **100%** |

Ranking order (shortlist and preview): **match % high → low**, then **more experience first**, then **earlier application first**. Withdrawn applicants are left out of the shortlist.

---

## 7. Business rules and validation

| Rule | Where enforced |
|---|---|
| Email must contain `@` and end with a known domain (.com, .net, .org, .edu, .in, .co, .io, .dev, .gov, .ac.in) | Validation |
| Phone: 10 digits starting with 6, 7, 8 or 9 | Validation |
| Password: 8+ characters with upper, lower, digit and special character | Validation |
| Name: letters (and spaces/dots), at least 2 characters | Validation |
| Experience: 0 to 60 years | Validation |
| Candidate status: Available, Not Available or Hired | Validation / CandidateService |
| Email unique per recruiter and per candidate | Service + UNIQUE constraint |
| One application per candidate per job | Service check + UNIQUE(candidate_id, job_id) |
| Recruiter can only update/delete/rule/shortlist/preview their own company's jobs | JobService / SmartMatchService |
| Only the candidate can update their own profile (recruiters have no edit option) | Menu |
| Recruiter status choices: Interview, Selected, Rejected | Menu |
| Withdrawn application cannot be changed by a recruiter | ApplicationService |
| Candidate withdraws only their own application, once, and not after Selected/Rejected | ApplicationService |
| Deleting a job or candidate removes its applications | Service + ON DELETE CASCADE |
| Non-numeric menu input never crashes the program (re-prompts) | Menu |
| Database errors are caught and reported; the program keeps running | Every repository |

---

## 8. Demo data

When the database is empty, the app seeds demo data on first start:

| Type | Data |
|---|---|
| Recruiters | 1 – Sarah Connor (TechCorp Systems), 2 – David Miller (Innovate Solutions); password `Sarnine@123` |
| Jobs | 101 Java Backend Engineer (TechCorp, Java), 102 Full Stack Developer (Innovate, React & Java), 103 Data Engineer (TechCorp, Python) |
| Candidates | 501 Alice Smith (Java, 3.5 yrs), 502 Bob Johnson (React & Java, 5 yrs), 503 Charlie Brown (Python, 2 yrs); password `Pass@1234` |
| Applications | 1001 Alice → 101, 1002 Bob → 102 |
| Admin | password is the `ADMIN_PASSWORD` constant in `Menu.java` |

---

## 9. Setup, run and test

1. Start MySQL 8 and create the schema:
   ```
   mysql -u root -p < sql/schema.sql
   ```
2. Set your MySQL user and password in `src/util/DBConnection.java`. **Do not push your real password to GitHub.**
3. Put `mysql-connector-j-26.7.0.jar` in `lib/` (see `lib/README.txt`).
4. Compile (Windows – use `:` instead of `;` on Linux/macOS):
   ```
   javac -encoding UTF-8 -cp lib/mysql-connector-j-26.7.0.jar -d bin src/model/*.java src/repository/*.java src/service/*.java src/util/*.java src/menu/*.java test/TalentFlowTests.java
   ```
5. Run the application:
   ```
   java -cp "bin;lib/mysql-connector-j-26.7.0.jar" menu.Main
   ```
6. Run the tests:
   ```
   java -cp "bin;lib/mysql-connector-j-26.7.0.jar" TalentFlowTests          (logic tests, no database)
   java -cp "bin;lib/mysql-connector-j-26.7.0.jar" TalentFlowTests --db     (also database tests)
   ```
   The `--db` tests create rows under the throwaway company `ZZ_TEST_CO` and delete them afterwards. They also write to `data/logs.txt`. Back up data you care about first.

---

## 10. Testing and results

See [TEST_RESULTS.md](TEST_RESULTS.md) for the full list.

**78 of 78 tests passed** on MySQL 8.0.45 (run on 2026-10-06 against a throwaway test database), both on an empty database and on a database with the demo data already in it.

| Category | Tests | Needs database | Passed |
|---|---:|:---:|---:|
| Validation | 18 | No | 18 |
| MatchScorer | 19 | No | 19 |
| Ranking | 7 | No | 7 |
| Shortlist + CSV | 6 | No | 6 |
| Database (smart match, workflow) | 18 | Yes | 18 |
| Fixes (bugs found in end-to-end testing) | 10 | Yes | 10 |
| **Total** | **78** | | **78** |

In addition, every feature was tested end to end through the real console menus with scripted input (admin, all recruiter options, all candidate options, registration, wrong passwords, unknown IDs, invalid input). The run logs contained no exceptions or SQL errors.

### Bugs found and fixed during end-to-end testing
| Bug | Fix |
|---|---|
| A recruiter could edit another company's job | Refused; checked before any input is asked |
| A recruiter could delete another company's job | Refused |
| Candidate status accepted any text (e.g. "vetti") | Only Available / Not Available / Hired |
| Classic matching engine worked across companies and matched "Java" to "JavaScript" | Own company only, whole-skill matching |
| An application could be withdrawn twice or after Selected/Rejected | Refused with a clear message |
| A recruiter could move a withdrawn application back to Interview | Refused |
| Withdrawn applications still showed in candidate and recruiter views | Hidden |
| ✅ ❌ symbols printed as `?` on Windows | Console switched to UTF-8 at start-up |
| Invalid name/experience in registration discarded the whole form | Field is re-asked immediately |
| A recruiter could edit a candidate's profile (option 9) | Option removed; only the candidate can update their profile |
| Two database tests assumed an empty database and failed when real candidates existed | Tests now check only their own test candidates; pass on empty and populated databases |

---

## 11. Change history (GitHub `main`)

| Commit | Change |
|---|---|
| f7f8a2f | Initial TalentFlow JDBC implementation |
| d205d14 | Weighted match percentage and ranked shortlist (additive) |
| 91c7be5 | Match % shown to recruiters only; candidate match preview for recruiters |
| 0aa2fbd | `&` and `/` treated as skill separators |
| b195264 | Console symbols, recruiter status options, withdrawn applications locked |
| 3b485da | Withdrawn applications hidden from views |
| 0c19a71 | Fixes from the full end-to-end test of every feature |
| 4c53898 | Project documentation and test results |
| (latest) | Recruiters can no longer edit candidate profiles; tests independent of existing data |

---

## 12. Known limitations

- Passwords are stored and compared as plain text; the admin password is a constant in the code.
- Database URL, user and password are hard-coded in `DBConnection.java`.
- IDs are generated as `MAX(id) + 1`, which is fine for one user but not for many at the same time.
- A candidate who withdraws cannot apply to the same job again (one application per candidate per job).
- Any logged-in recruiter can delete any candidate profile (option 10).
- Tested with small demo datasets only; no load or multi-user testing.
- Console only; no GUI or web interface.

## 13. Future scope

- Hash passwords (e.g. BCrypt) and move configuration to an external properties file.
- Web interface or REST API on top of the existing service layer.
- Interview scheduling and email notifications on status changes.
- Resume upload with automatic skill extraction.
- JUnit tests with code-coverage reporting.
