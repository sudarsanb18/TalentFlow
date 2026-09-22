package service;

import model.Recruiter;
import repository.RecruiterRepository;
import util.FileManager;
import util.Validation;

import java.util.ArrayList;

/**
 * RecruiterService handles all business logic operations for Recruiters in TalentFlow.
 * Enforces dynamic unique Recruiter IDs and prevents duplicate email registration.
 */
public class RecruiterService {
    private final RecruiterRepository recruiterRepo;

    public RecruiterService() {
        this.recruiterRepo = new RecruiterRepository();
    }

    /**
     * Registers a new Recruiter with dynamic unique ID assignment and unique email verification.
     * 
     * @param name     Recruiter name
     * @param email    Recruiter email
     * @param phone    Recruiter phone number
     * @param password Recruiter password
     * @param company  Company name
     * @return Created Recruiter object or null if validation fails or email exists
     */
    public Recruiter addRecruiter(String name, String email, String phone, String password, String company) {
        if (!Validation.isValidName(name)) {
            System.out.println("❌ Invalid Recruiter Name! Must contain letters and be at least 2 characters.");
            return null;
        }
        if (!Validation.isValidEmail(email)) {
            System.out.println("❌ Invalid Email format! Email must contain '@' and end with a valid domain such as '.com', '.net', '.org', or '.in'.");
            return null;
        }
        if (recruiterRepo.isEmailExists(email)) {
            System.out.println("❌ Registration Failed! Email address '" + email + "' is already registered by another recruiter.");
            return null;
        }
        if (!Validation.isValidPhone(phone)) {
            System.out.println("❌ Invalid Phone Number! Must be a 10-digit number starting with 6, 7, 8, or 9.");
            return null;
        }
        if (!Validation.isValidPassword(password)) {
            System.out.println("❌ Invalid Password! Must be at least 8 characters long and contain uppercase, lowercase, digit, and special char.");
            return null;
        }
        if (company == null || company.trim().isEmpty()) {
            System.out.println("❌ Company name cannot be empty!");
            return null;
        }

        int id = recruiterRepo.getNextId();
        Recruiter recruiter = new Recruiter(id, name.trim(), email.trim(), phone.trim(), password.trim(), company.trim());
        boolean success = recruiterRepo.addRecruiter(recruiter);

        if (success) {
            System.out.println("✅ Recruiter registered successfully with Unique Recruiter ID: " + recruiter.getId());
            FileManager.writeLog("Added new recruiter: " + recruiter.getName() + " for company " + recruiter.getCompany() + " (ID: " + recruiter.getId() + ")");
            return recruiter;
        } else {
            System.out.println("❌ Failed to add recruiter.");
            return null;
        }
    }

    /**
     * Authenticates a recruiter using Recruiter ID and password.
     * 
     * @param id       Recruiter ID
     * @param password Password string
     * @return Recruiter object if authenticated, null otherwise
     */
    public Recruiter authenticateRecruiter(int id, String password) {
        Recruiter recruiter = recruiterRepo.findById(id);
        if (recruiter != null && recruiter.getPassword().equals(password.trim())) {
            return recruiter;
        }
        return null;
    }

    /**
     * Displays all recruiters in formatted tabular layout using StringBuilder.
     */
    public void viewRecruiters() {
        ArrayList<Recruiter> recruiters = recruiterRepo.getAllRecruiters();
        if (recruiters.isEmpty()) {
            System.out.println("⚠️ No recruiters found in the system.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=========================================================================================================\n");
        sb.append(String.format("| %-6s | %-20s | %-25s | %-12s | %-20s |\n", "ID", "NAME", "EMAIL", "PHONE", "COMPANY"));
        sb.append("=========================================================================================================\n");

        for (Recruiter r : recruiters) {
            sb.append(String.format("| %-6d | %-20s | %-25s | %-12s | %-20s |\n",
                    r.getId(), r.getName(), r.getEmail(), r.getPhone(), r.getCompany()));
        }
        sb.append("=========================================================================================================\n");

        System.out.print(sb.toString());
    }

    /**
     * Gets all recruiters.
     * @return list of recruiters
     */
    public ArrayList<Recruiter> getAllRecruiters() {
        return recruiterRepo.getAllRecruiters();
    }

    /**
     * Finds recruiter by ID.
     * 
     * @param id Recruiter ID
     * @return Recruiter instance or null if not found
     */
    public Recruiter getRecruiterById(int id) {
        return recruiterRepo.findById(id);
    }

    /**
     * Gets total recruiter count.
     * 
     * @return recruiter count
     */
    public int countRecruiters() {
        return recruiterRepo.getCount();
    }
}
