package menu;

import model.Candidate;
import model.Job;
import model.Recruiter;
import service.ApplicationService;
import service.CandidateService;
import service.JobService;
import service.RecruiterService;
import util.FileManager;
import util.Validation;

import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Menu class driving the 4-Portal Role-Based Console Architecture for TalentFlow ATS:
 * 1. Admin Portal (Password: admin_1234)
 * 2. Recruiter Portal (Password: Sarnine@123)
 * 3. Candidate Portal
 * 4. Apply / Register as New Candidate
 * 5. Exit
 */
public class Menu {
    private final CandidateService candidateService;
    private final RecruiterService recruiterService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final Scanner scanner;

    private static final String ADMIN_PASSWORD = "admin_1234";

    public Menu() {
        this.candidateService = new CandidateService();
        this.recruiterService = new RecruiterService();
        this.jobService = new JobService();
        this.applicationService = new ApplicationService();
        this.scanner = new Scanner(System.in);
        
        // Seed initial demo data only if database files on disk are completely empty
        seedInitialDataIfEmpty();
    }

    /**
     * Seeds initial demo data ONLY if data storage files on disk are empty.
     */
    private void seedInitialDataIfEmpty() {
        if (recruiterService.countRecruiters() == 0 && jobService.countJobs() == 0 && candidateService.countCandidates() == 0) {
            System.out.println("ℹ️ First time system startup detected. Seeding initial baseline data...");
            // Recruiters (ID 1, 2)
            recruiterService.addRecruiter("Sarah Connor", "sarah.c@techcorp.com", "9876543210", "Sarnine@123", "TechCorp Systems");
            recruiterService.addRecruiter("David Miller", "david.m@innovate.io", "9876543211", "Sarnine@123", "Innovate Solutions");

            // Jobs (ID 101, 102, 103)
            jobService.addJob("Java Backend Engineer", "TechCorp Systems", "Bangalore / Remote", "12 - 18 LPA", "Java");
            jobService.addJob("Full Stack Developer", "Innovate Solutions", "Hyderabad", "15 - 22 LPA", "React & Java");
            jobService.addJob("Data Engineer", "TechCorp Systems", "Pune", "10 - 14 LPA", "Python");

            // Candidates (ID 501, 502, 503)
            candidateService.addCandidate("Alice Smith", "alice.smith@gmail.com", "9123456780", "Pass@1234", "Java", 3.5, "Experienced Java Developer specializing in Microservices");
            candidateService.addCandidate("Bob Johnson", "bob.j@yahoo.com", "9123456781", "Pass@1234", "React & Java", 5.0, "Senior Full Stack Specialist proficient in React and Java");
            candidateService.addCandidate("Charlie Brown", "charlie.b@outlook.com", "9123456782", "Pass@1234", "Python", 2.0, "Data Engineer skilled in SQL and Python pipelines");

            // Applications (App ID 1001, 1002)
            applicationService.applyJob(501, 101); // Alice -> Java Backend
            applicationService.applyJob(502, 102); // Bob -> Full Stack

            FileManager.writeLog("Initial demonstration data seeded successfully. Recruiter password set to 'Sarnine@123'.");
        } else {
            System.out.println("✅ Permanent data storage loaded: " + candidateService.countCandidates() + " candidates, " +
                    recruiterService.countRecruiters() + " recruiters, " + jobService.countJobs() + " jobs.");
        }
    }

    /**
     * Entry method to launch ATS menu loop.
     */
    public void start() {
        displayMainMenu();
    }

    public void displayMenu() {
        displayMainMenu();
    }

    /**
     * Top-Level Role-Based Main Menu.
     */
    public void displayMainMenu() {
        boolean running = true;
        System.out.println("\n********************************************************************************");
        System.out.println("                 WELCOME TO TALENTFLOW ATS - MAIN PORTAL                        ");
        System.out.println("********************************************************************************");

        while (running) {
            printMainMenuHeader();
            int choice = readIntInput("Enter your choice : ");

            try {
                switch (choice) {
                    case 1 -> handleAdminPortal();
                    case 2 -> handleRecruiterPortal();
                    case 3 -> handleCandidatePortal();
                    case 4 -> handleNewCandidateRegistrationAndApplication();
                    case 5 -> {
                        System.out.println("\nAudit Log View requested before exit:");
                        FileManager.readLog();
                        System.out.println("\nThank you for using TalentFlow ATS! All data saved permanently.");
                        running = false;
                    }
                    default -> System.out.println("❌ Invalid Choice! Please select an option between 1 and 5.");
                }
            } catch (Exception e) {
                System.err.println("⚠️ System error handled cleanly: " + e.getMessage());
                FileManager.writeLog("Handled exception in main menu: " + e.toString());
            }
        }
    }

    private void printMainMenuHeader() {
        System.out.println("\n====================================");
        System.out.println("    TALENTFLOW ATS - MAIN PORTAL    ");
        System.out.println("====================================");
        System.out.println("1  Admin");
        System.out.println("2  Recruiter");
        System.out.println("3  Candidate");
        System.out.println("4  Apply / Register as New Candidate");
        System.out.println("5  Exit");
        System.out.println("====================================");
    }

    // ================================================================================
    // 1. ADMIN PORTAL (Password: admin_1234)
    // ================================================================================

    private void handleAdminPortal() {
        System.out.println("\n🔒 ADMIN SECURITY AUTHENTICATION");
        String password = readStringInput("Enter Admin Password: ");

        if (!ADMIN_PASSWORD.equals(password.trim())) {
            System.out.println("❌ Access Denied! Invalid Admin Password.");
            return;
        }

        System.out.println("✅ ADMIN AUTHENTICATION SUCCESSFUL! Welcome System Administrator.");
        boolean adminLoop = true;

        while (adminLoop) {
            System.out.println("\n====================================");
            System.out.println("          ADMIN PORTAL              ");
            System.out.println("====================================");
            System.out.println("1  Add Recruiter");
            System.out.println("2  View Recruiter Directory");
            System.out.println("3  Master Admin Dashboard & CSV Export");
            System.out.println("4  Back to Main Menu");
            System.out.println("====================================");

            int choice = readIntInput("Enter Choice : ");
            switch (choice) {
                case 1 -> handleAddRecruiter();
                case 2 -> recruiterService.viewRecruiters();
                case 3 -> handleMasterAdminDashboard();
                case 4 -> adminLoop = false;
                default -> System.out.println("❌ Invalid option choice.");
            }
        }
    }

    // ================================================================================
    // 2. RECRUITER PORTAL (Password: Sarnine@123)
    // ================================================================================

    private void handleRecruiterPortal() {
        System.out.println("\n🔒 RECRUITER AUTHENTICATION REQUIRED");
        int recruiterId = readIntInput("Enter Recruiter ID (e.g. 1, 2): ");
        String password = readStringInput("Enter Recruiter Password: ");

        Recruiter recruiter = recruiterService.authenticateRecruiter(recruiterId, password);
        if (recruiter == null) {
            System.out.println("❌ Authentication Failed! Invalid Recruiter ID or Password.");
            return;
        }

        System.out.println("✅ Welcome Recruiter " + recruiter.getName() + " (" + recruiter.getCompany() + ")");
        boolean recruiterLoop = true;

        while (recruiterLoop) {
            System.out.println("\n====================================");
            System.out.println("        RECRUITER PORTAL            ");
            System.out.println("====================================");
            System.out.println("1  Add Job");
            System.out.println("2  View Jobs");
            System.out.println("3  Search Job (by ID or Title)");
            System.out.println("4  Update Job");
            System.out.println("5  Delete Job");
            System.out.println("------------------------------------");
            System.out.println("6  View Candidates");
            System.out.println("7  Search Candidate by ID");
            System.out.println("8  Search Candidate by Name");
            System.out.println("9  Update Candidate Profile");
            System.out.println("10 Delete Candidate Profile");
            System.out.println("------------------------------------");
            System.out.println("11 View Applications");
            System.out.println("12 Update Application Status");
            System.out.println("13 Run Skill Matching Engine");
            System.out.println("14 Back to Main Menu");
            System.out.println("====================================");

            int choice = readIntInput("Enter Choice : ");
            switch (choice) {
                case 1 -> handleAddJobForRecruiter(recruiter);
                case 2 -> jobService.viewJobs();
                case 3 -> handleSearchJob();
                case 4 -> handleUpdateJobForRecruiter(recruiter);
                case 5 -> handleDeleteJobForRecruiter(recruiter);
                case 6 -> candidateService.viewCandidates();
                case 7 -> handleSearchCandidateById();
                case 8 -> handleSearchCandidateByName();
                case 9 -> handleUpdateCandidateDirect();
                case 10 -> handleDeleteCandidateDirect();
                case 11 -> applicationService.viewApplicationsForRecruiter();
                case 12 -> handleUpdateApplicationStatusDirect();
                case 13 -> applicationService.runAutomatedSkillMatching();
                case 14 -> recruiterLoop = false;
                default -> System.out.println("❌ Invalid choice.");
            }
        }
    }

    // ================================================================================
    // 3. CANDIDATE PORTAL
    // ================================================================================

    private void handleCandidatePortal() {
        System.out.println("\n🔒 CANDIDATE LOGIN REQUIRED");
        int candidateId = readIntInput("Enter Candidate ID (e.g. 501, 502): ");
        Candidate candidate = candidateService.getCandidateById(candidateId);

        if (candidate == null) {
            System.out.println("❌ Candidate ID " + candidateId + " not found.");
            return;
        }

        if (!candidate.getPassword().isEmpty()) {
            String password = readStringInput("Enter Candidate Password: ");
            if (!candidate.getPassword().equals(password.trim())) {
                System.out.println("❌ Authentication Failed! Invalid Password.");
                return;
            }
        }

        System.out.println("✅ Welcome Candidate " + candidate.getName() + " (Skill: " + candidate.getSkill() + ")");
        boolean candidateLoop = true;

        while (candidateLoop) {
            System.out.println("\n====================================");
            System.out.println("        CANDIDATE PORTAL            ");
            System.out.println("====================================");
            System.out.println("1  View My Applications & Statuses");
            System.out.println("2  View Available Job Openings");
            System.out.println("3  Update My Profile");
            System.out.println("4  Withdraw My Application");
            System.out.println("5  Delete My Account");
            System.out.println("6  Back to Main Menu");
            System.out.println("====================================");

            int choice = readIntInput("Enter Choice : ");
            switch (choice) {
                case 1 -> applicationService.viewApplicationsForCandidate(candidateId);
                case 2 -> jobService.viewJobs();
                case 3 -> {
                    String skill = readStringInput("Enter New Skill (" + candidate.getSkill() + "): ");
                    double exp = readDoubleInput("Enter New Experience (" + candidate.getExperience() + " yrs): ");
                    String status = readStringInput("Enter New Status (" + candidate.getStatus() + "): ");
                    candidateService.updateCandidate(candidateId, skill, exp, status);
                }
                case 4 -> {
                    applicationService.viewApplicationsForCandidate(candidateId);
                    int appId = readIntInput("Enter Application ID to withdraw: ");
                    applicationService.withdrawApplication(appId, candidateId);
                }
                case 5 -> {
                    System.out.print("⚠️ Are you sure you want to delete your candidate profile? (yes/no): ");
                    String confirm = scanner.nextLine().trim();
                    if (confirm.equalsIgnoreCase("yes")) {
                        candidateService.deleteCandidate(candidateId);
                        candidateLoop = false;
                    }
                }
                case 6 -> candidateLoop = false;
                default -> System.out.println("❌ Invalid choice.");
            }
        }
    }

    // ================================================================================
    // 4. APPLY / REGISTER AS NEW CANDIDATE
    // ================================================================================

    private void handleNewCandidateRegistrationAndApplication() {
        System.out.println("\n--- CANDIDATE JOB APPLICATION & REGISTRATION ---");
        jobService.viewJobs();
        int jobId = readIntInput("Enter Job ID you wish to apply for (e.g. 101, 102): ");

        Job job = jobService.searchJob(jobId);
        if (job == null) {
            System.out.println("❌ Job ID " + jobId + " does not exist.");
            return;
        }

        System.out.println("\n--- Please enter your Candidate Profile details ---");
        String name = readStringInput("Enter Your Full Name: ");
        String email = readValidEmailInput("Enter Your Email Address: ");
        String phone = readValidPhoneInput("Enter Your 10-digit Phone Number (starting with 6,7,8,9): ");
        String password = readValidPasswordInput("Create Your Password (min 8 chars, upper, lower, digit, special char): ");
        String skill = readStringInput("Enter Your Primary Skill (e.g. Java, Python, React): ");
        double experience = readDoubleInput("Enter Your Experience (in years): ");
        String resumeSummary = readStringInput("Enter Short Bio / Resume Summary: ");

        Candidate candidate = candidateService.addCandidate(name, email, phone, password, skill, experience, resumeSummary);
        if (candidate != null) {
            applicationService.applyJob(candidate.getId(), jobId);
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("🎉 SUCCESS! YOUR JOB APPLICATION IS COMPLETE & SAVED PERMANENTLY.");
            System.out.println("🔑 YOUR AUTO-GENERATED UNIQUE CANDIDATE ID IS : " + candidate.getId());
            System.out.println("🔒 YOUR SECURE PASSWORD HAS BEEN SET.");
            System.out.println("--------------------------------------------------------------------------------");
        }
    }

    // ================================================================================
    // HELPER METHODS
    // ================================================================================

    private void handleAddRecruiter() {
        System.out.println("\n--- Add New Recruiter ---");
        String name = readStringInput("Enter Recruiter Name: ");
        String email = readValidEmailInput("Enter Recruiter Email: ");
        String phone = readValidPhoneInput("Enter Recruiter 10-digit Phone Number (starting with 6,7,8,9): ");
        String password = readValidPasswordInput("Set Recruiter Password (min 8 chars, upper, lower, digit, special char): ");
        String company = readStringInput("Enter Company Name: ");

        recruiterService.addRecruiter(name, email, phone, password, company);
    }

    private void handleAddJobForRecruiter(Recruiter recruiter) {
        System.out.println("\n--- Post New Job Opening ---");
        String title = readStringInput("Enter Job Title: ");
        String location = readStringInput("Enter Job Location: ");
        String salaryRange = readStringInput("Enter Salary Range / Budget (e.g. 10 - 15 LPA): ");
        String requiredSkill = readStringInput("Enter Required Skill for Candidates: ");

        jobService.addJob(title, recruiter.getCompany(), location, salaryRange, requiredSkill);
    }

    private void handleUpdateJobForRecruiter(Recruiter recruiter) {
        int jobId = readIntInput("Enter Job ID to update: ");
        Job existing = jobService.searchJob(jobId);
        if (existing != null) {
            String title = readStringInput("Enter New Job Title (" + existing.getTitle() + "): ");
            String location = readStringInput("Enter New Location (" + existing.getLocation() + "): ");
            String requiredSkill = readStringInput("Enter New Required Skill (" + existing.getRequiredSkill() + "): ");

            jobService.updateJob(jobId, title, location, requiredSkill);
        }
    }

    private void handleDeleteJobForRecruiter(Recruiter recruiter) {
        int jobId = readIntInput("Enter Job ID to delete: ");
        jobService.deleteJob(jobId);
    }

    private void handleSearchJob() {
        System.out.println("Search Job by: 1) Job ID  2) Job Title");
        int subChoice = readIntInput("Select sub-option (1 or 2): ");
        if (subChoice == 1) {
            int jobId = readIntInput("Enter Job ID to search (e.g. 101): ");
            jobService.searchJob(jobId);
        } else if (subChoice == 2) {
            String title = readStringInput("Enter Job Title keyword to search: ");
            jobService.searchJob(title);
        } else {
            System.out.println("❌ Invalid sub-option choice.");
        }
    }

    private void handleSearchCandidateById() {
        int id = readIntInput("Enter Candidate ID to search: ");
        candidateService.searchCandidate(id);
    }

    private void handleSearchCandidateByName() {
        String name = readStringInput("Enter Candidate Name or keyword to search: ");
        candidateService.searchCandidate(name);
    }

    private void handleUpdateCandidateDirect() {
        int id = readIntInput("Enter Candidate ID to update: ");
        Candidate existing = candidateService.getCandidateById(id);
        if (existing != null) {
            String skill = readStringInput("Enter New Skill (" + existing.getSkill() + "): ");
            double exp = readDoubleInput("Enter New Experience (" + existing.getExperience() + " yrs): ");
            String status = readStringInput("Enter New Status (" + existing.getStatus() + "): ");
            candidateService.updateCandidate(id, skill, exp, status);
        }
    }

    private void handleDeleteCandidateDirect() {
        int id = readIntInput("Enter Candidate ID to delete: ");
        candidateService.deleteCandidate(id);
    }

    private void handleUpdateApplicationStatusDirect() {
        System.out.println("--- Update Application Status ---");
        int appId = readIntInput("Enter Application ID (e.g. 1001): ");
        System.out.println("Choose new status option:");
        System.out.println("1) Applied  2) Interview  3) Selected  4) Rejected  5) Withdrawn");
        int stChoice = readIntInput("Select status option (1-5): ");
        String newStatus = switch (stChoice) {
            case 1 -> "Applied";
            case 2 -> "Interview";
            case 3 -> "Selected";
            case 4 -> "Rejected";
            case 5 -> "Withdrawn";
            default -> "";
        };

        if (!newStatus.isEmpty()) {
            applicationService.updateApplicationStatus(appId, newStatus);
        } else {
            System.out.println("❌ Invalid status option selected.");
        }
    }

    /**
     * MASTER ADMIN DASHBOARD (Tabular organized presentation)
     */
    private void handleMasterAdminDashboard() {
        System.out.println("\n*********************************************************************************************************");
        System.out.println("                                TALENTFLOW ATS - MASTER ADMIN DASHBOARD                                  ");
        System.out.println("*********************************************************************************************************");

        System.out.println("\n📊 1. SYSTEM METRICS OVERVIEW");
        System.out.println("---------------------------------------------------------------------------------------------------------");
        System.out.println(" • Total Recruiters Registered : " + recruiterService.countRecruiters());
        System.out.println(" • Total Jobs Posted           : " + jobService.countJobs());
        System.out.println(" • Total Candidates Registered : " + candidateService.countCandidates());
        System.out.println(" • Total Applications Filed    : " + applicationService.getAllApplications().size());

        System.out.println("\n🏢 2. COMPLETE RECRUITER DIRECTORY");
        recruiterService.viewRecruiters();

        System.out.println("\n💼 3. COMPLETE JOB POSTINGS DIRECTORY");
        jobService.viewJobs();

        System.out.println("\n👨‍🎓 4. COMPLETE CANDIDATE DIRECTORY");
        candidateService.viewCandidates();

        System.out.println("\n📄 5. COMPLETE APPLICATION TRACKING RECORDS");
        applicationService.viewApplicationsForRecruiter();

        System.out.println("\n📜 6. SYSTEM AUDIT LOG HISTORY");
        FileManager.readLog();

        System.out.println("---------------------------------------------------------------------------------------------------------");
        System.out.println("Do you want to export this complete ATS Report to a CSV file (data/ats_export_report.csv)? (1: Yes / 2: No)");
        int expChoice = readIntInput("Choice: ");
        if (expChoice == 1) {
            FileManager.exportATSToCSV(candidateService.getAllCandidates(), recruiterService.getAllRecruiters(), jobService.getAllJobs(), applicationService.getAllApplications());
        }

        System.out.println("*********************************************************************************************************");
        System.out.println("                             END OF MASTER ADMIN DASHBOARD REPORT                                        ");
        System.out.println("*********************************************************************************************************\n");
    }

    private String readValidPasswordInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String password = scanner.nextLine().trim();
            if (Validation.isValidPassword(password)) {
                return password;
            }
            System.out.println("❌ Invalid Password! Password must be at least 8 characters long and contain uppercase (A-Z), lowercase (a-z), numbers (0-9), and special characters (!@#$%^&*). Please try again.");
        }
    }

    private String readValidEmailInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String email = scanner.nextLine().trim();
            if (Validation.isValidEmail(email)) {
                return email;
            }
            System.out.println("❌ Invalid Email format! Email must contain '@' and end with a valid domain such as '.com', '.net', '.org', or '.in'. Please enter again.");
        }
    }

    private String readValidPhoneInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String phone = scanner.nextLine().trim();
            if (Validation.isValidPhone(phone)) {
                return phone;
            }
            System.out.println("❌ Invalid Phone Number! Must be a 10-digit numerical phone number starting with 6, 7, 8, or 9 (e.g. 9876543210). Please enter again.");
        }
    }

    private int readIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                int input = scanner.nextInt();
                scanner.nextLine();
                return input;
            } catch (InputMismatchException ime) {
                System.out.println("❌ Invalid input! Please enter a valid numerical integer.");
                scanner.nextLine();
            }
        }
    }

    private double readDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                double input = scanner.nextDouble();
                scanner.nextLine();
                return input;
            } catch (InputMismatchException ime) {
                System.out.println("❌ Invalid input! Please enter a valid decimal/number.");
                scanner.nextLine();
            }
        }
    }

    private String readStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}