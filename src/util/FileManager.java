package util;

import model.Application;
import model.Candidate;
import model.Job;
import model.Recruiter;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for permanent file storage, CSV report export, and audit logging.
 * Uses FileWriter, BufferedWriter, FileReader, and BufferedReader to persist all
 * candidates, recruiters, jobs, and applications permanently to disk in data/ directory.
 */
public class FileManager {
    private static final String DATA_DIR = "data";
    private static final String LOG_FILE_PATH = "data/logs.txt";
    private static final String CANDIDATES_FILE = "data/candidates.txt";
    private static final String RECRUITERS_FILE = "data/recruiters.txt";
    private static final String JOBS_FILE = "data/jobs.txt";
    private static final String APPLICATIONS_FILE = "data/applications.txt";
    private static final String CSV_EXPORT_FILE = "data/ats_export_report.csv";

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void ensureDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Appends an audit log message to data/logs.txt file with timestamp.
     * 
     * @param message Action message to log
     */
    public static void writeLog(String message) {
        ensureDataDirectory();
        String timeStampedMessage = "[" + LocalDateTime.now().format(dtf) + "] " + message;

        try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(timeStampedMessage);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to write to log file: " + e.getMessage());
        }
    }

    /**
     * Reads all recorded log entries from data/logs.txt and displays them on console.
     */
    public static void readLog() {
        File file = new File(LOG_FILE_PATH);
        if (!file.exists()) {
            System.out.println("No log history found at " + LOG_FILE_PATH);
            return;
        }

        System.out.println("\n================================================================================");
        System.out.println("                            TALENTFLOW AUDIT LOGS                               ");
        System.out.println("================================================================================");

        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                count++;
            }
            if (count == 0) {
                System.out.println("(Log file is currently empty)");
            }
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to read log file: " + e.getMessage());
        }
        System.out.println("================================================================================\n");
    }

    // --------------------------------------------------------------------------------
    // CANDIDATE PERMANENT STORAGE
    // --------------------------------------------------------------------------------

    public static void saveCandidates(List<Candidate> candidates) {
        ensureDataDirectory();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CANDIDATES_FILE))) {
            for (Candidate c : candidates) {
                // id | name | email | phone | password | skill | experience | status | resumeSummary
                String line = String.format("%d|%s|%s|%s|%s|%s|%.2f|%s|%s",
                        c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getPassword(), c.getSkill(), c.getExperience(), c.getStatus(), c.getResumeSummary());
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save candidates to disk: " + e.getMessage());
        }
    }

    public static List<Candidate> loadCandidates() {
        List<Candidate> list = new ArrayList<>();
        File file = new File(CANDIDATES_FILE);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 8) {
                    int id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    String email = parts[2];
                    String phone = parts[3];
                    String password = parts[4];
                    String skill = parts[5];
                    double exp = Double.parseDouble(parts[6]);
                    String status = parts[7];
                    String summary = (parts.length >= 9) ? parts[8] : "No profile summary provided";

                    list.add(new Candidate(id, name, email, phone, password, skill, exp, status, summary));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load candidates from disk: " + e.getMessage());
        }
        return list;
    }

    // --------------------------------------------------------------------------------
    // RECRUITER PERMANENT STORAGE
    // --------------------------------------------------------------------------------

    public static void saveRecruiters(List<Recruiter> recruiters) {
        ensureDataDirectory();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(RECRUITERS_FILE))) {
            for (Recruiter r : recruiters) {
                // id | name | email | phone | password | company
                String line = String.format("%d|%s|%s|%s|%s|%s",
                        r.getId(), r.getName(), r.getEmail(), r.getPhone(), r.getPassword(), r.getCompany());
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save recruiters to disk: " + e.getMessage());
        }
    }

    public static List<Recruiter> loadRecruiters() {
        List<Recruiter> list = new ArrayList<>();
        File file = new File(RECRUITERS_FILE);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    int id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    String email = parts[2];
                    String phone = parts[3];
                    String password = parts[4];
                    String company = parts[5];

                    list.add(new Recruiter(id, name, email, phone, password, company));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load recruiters from disk: " + e.getMessage());
        }
        return list;
    }

    // --------------------------------------------------------------------------------
    // JOB PERMANENT STORAGE
    // --------------------------------------------------------------------------------

    public static void saveJobs(List<Job> jobs) {
        ensureDataDirectory();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(JOBS_FILE))) {
            for (Job j : jobs) {
                // jobId | title | company | location | salaryRange | requiredSkill
                String line = String.format("%d|%s|%s|%s|%s|%s",
                        j.getJobId(), j.getTitle(), j.getCompany(), j.getLocation(), j.getSalaryRange(), j.getRequiredSkill());
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save jobs to disk: " + e.getMessage());
        }
    }

    public static List<Job> loadJobs() {
        List<Job> list = new ArrayList<>();
        File file = new File(JOBS_FILE);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    int id = Integer.parseInt(parts[0]);
                    String title = parts[1];
                    String company = parts[2];
                    String location = parts[3];
                    String salary = (parts.length >= 6) ? parts[4] : "Not Disclosed";
                    String reqSkill = (parts.length >= 6) ? parts[5] : parts[4];

                    list.add(new Job(id, title, company, location, salary, reqSkill));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load jobs from disk: " + e.getMessage());
        }
        return list;
    }

    // --------------------------------------------------------------------------------
    // APPLICATION PERMANENT STORAGE
    // --------------------------------------------------------------------------------

    public static void saveApplications(List<Application> applications) {
        ensureDataDirectory();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(APPLICATIONS_FILE))) {
            for (Application a : applications) {
                // applicationId | candidateId | jobId | status
                String line = String.format("%d|%d|%d|%s",
                        a.getApplicationId(), a.getCandidateId(), a.getJobId(), a.getStatus());
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save applications to disk: " + e.getMessage());
        }
    }

    public static List<Application> loadApplications() {
        List<Application> list = new ArrayList<>();
        File file = new File(APPLICATIONS_FILE);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    int appId = Integer.parseInt(parts[0]);
                    int candId = Integer.parseInt(parts[1]);
                    int jobId = Integer.parseInt(parts[2]);
                    String status = parts[3];

                    list.add(new Application(appId, candId, jobId, status));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load applications from disk: " + e.getMessage());
        }
        return list;
    }

    // --------------------------------------------------------------------------------
    // HR BUSINESS EXPORT: EXPORT ATS SYSTEM SUMMARY TO CSV FILE
    // --------------------------------------------------------------------------------

    public static void exportATSToCSV(List<Candidate> candidates, List<Recruiter> recruiters, List<Job> jobs, List<Application> applications) {
        ensureDataDirectory();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CSV_EXPORT_FILE))) {
            bw.write("=== TALENTFLOW ATS ENTERPRISE REPORT ===");
            bw.newLine();
            bw.write("Export Timestamp," + LocalDateTime.now().format(dtf));
            bw.newLine();
            bw.newLine();

            // RECRUITERS SECTION
            bw.write("--- RECRUITERS DIRECTORY ---");
            bw.newLine();
            bw.write("Recruiter ID,Name,Email,Phone,Company");
            bw.newLine();
            for (Recruiter r : recruiters) {
                bw.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\"", r.getId(), r.getName(), r.getEmail(), r.getPhone(), r.getCompany()));
                bw.newLine();
            }
            bw.newLine();

            // JOBS SECTION
            bw.write("--- JOB POSTINGS DIRECTORY ---");
            bw.newLine();
            bw.write("Job ID,Title,Company,Location,Salary Range,Required Skill");
            bw.newLine();
            for (Job j : jobs) {
                bw.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"", j.getJobId(), j.getTitle(), j.getCompany(), j.getLocation(), j.getSalaryRange(), j.getRequiredSkill()));
                bw.newLine();
            }
            bw.newLine();

            // CANDIDATES SECTION
            bw.write("--- CANDIDATES DIRECTORY ---");
            bw.newLine();
            bw.write("Candidate ID,Name,Email,Phone,Skill,Experience(Yrs),Status,Bio Summary");
            bw.newLine();
            for (Candidate c : candidates) {
                bw.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%.1f,\"%s\",\"%s\"", c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getSkill(), c.getExperience(), c.getStatus(), c.getResumeSummary()));
                bw.newLine();
            }
            bw.newLine();

            // APPLICATIONS SECTION
            bw.write("--- APPLICATIONS TRACKING DIRECTORY ---");
            bw.newLine();
            bw.write("Application ID,Candidate ID,Job ID,Status");
            bw.newLine();
            for (Application a : applications) {
                bw.write(String.format("%d,%d,%d,\"%s\"", a.getApplicationId(), a.getCandidateId(), a.getJobId(), a.getStatus()));
                bw.newLine();
            }

            System.out.println("✅ HR Report successfully exported to file: " + CSV_EXPORT_FILE);
            writeLog("Exported full ATS CSV report to " + CSV_EXPORT_FILE);
        } catch (IOException e) {
            System.err.println("Failed to export ATS report to CSV: " + e.getMessage());
        }
    }
}
