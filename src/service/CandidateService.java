package service;

import model.Candidate;
import repository.ApplicationRepository;
import repository.CandidateRepository;
import util.FileManager;
import util.Validation;

import java.util.ArrayList;
import java.util.List;

/**
 * CandidateService handles all business logic operations for candidates in TalentFlow.
 * Enforces dynamic unique Candidate IDs and prevents duplicate email registration.
 */
public class CandidateService {
    private final CandidateRepository candidateRepo;
    private final ApplicationRepository applicationRepo;

    public CandidateService() {
        this.candidateRepo = new CandidateRepository();
        this.applicationRepo = new ApplicationRepository();
    }

    /**
     * Registers a new candidate with dynamic unique ID assignment and unique email verification.
     * 
     * @param name          Candidate name
     * @param email         Candidate email
     * @param phone         Candidate 10-digit phone number (starting with 6-9)
     * @param password      Candidate password (min 8 chars, upper, lower, digit, special)
     * @param skill         Candidate primary skill
     * @param experience    Experience in years
     * @param resumeSummary Professional resume bio summary
     * @return Created Candidate object, or null if validation fails or email exists
     */
    public Candidate addCandidate(String name, String email, String phone, String password, String skill, double experience, String resumeSummary) {
        if (!Validation.isValidName(name)) {
            System.out.println("❌ Invalid Name! Must contain alphabets and be at least 2 characters long.");
            return null;
        }
        if (!Validation.isValidEmail(email)) {
            System.out.println("❌ Invalid Email format! Email must contain '@' and end with a valid domain such as '.com', '.net', '.org', or '.in'.");
            return null;
        }
        if (candidateRepo.isEmailExists(email)) {
            System.out.println("❌ Registration Failed! Email address '" + email + "' is already registered in TalentFlow.");
            return null;
        }
        if (!Validation.isValidPhone(phone)) {
            System.out.println("❌ Invalid Phone Number! Must be a 10-digit number starting with 6, 7, 8, or 9.");
            return null;
        }
        if (!Validation.isValidSkill(skill)) {
            System.out.println("❌ Invalid Skill! Skill field cannot be empty.");
            return null;
        }
        if (!Validation.isPositiveExperience(experience)) {
            System.out.println("❌ Invalid Experience! Experience must be between 0.0 and 60.0 years.");
            return null;
        }

        int id = candidateRepo.getNextId();
        String pwd = (password != null) ? password.trim() : "";
        Candidate candidate = new Candidate(id, name.trim(), email.trim(), phone.trim(), pwd, skill.trim(), experience, "Available", resumeSummary);
        boolean success = candidateRepo.addCandidate(candidate);

        if (success) {
            System.out.println("✅ Candidate registered successfully with Unique Candidate ID: " + candidate.getId());
            FileManager.writeLog("Added new candidate: " + candidate.getName() + " (ID: " + candidate.getId() + ")");
            return candidate;
        } else {
            System.out.println("❌ Failed to add candidate.");
            return null;
        }
    }

    public Candidate addCandidate(String name, String email, String phone, String password, String skill, double experience) {
        return addCandidate(name, email, phone, password, skill, experience, "No profile summary provided");
    }

    /**
     * Authenticates candidate or verifies password.
     * 
     * @param id       Candidate ID
     * @param password Candidate Password
     * @return Candidate object if authenticated or password empty/unset, null otherwise
     */
    public Candidate authenticateCandidate(int id, String password) {
        Candidate candidate = candidateRepo.findById(id);
        if (candidate != null) {
            if (candidate.getPassword().isEmpty() || candidate.getPassword().equals(password.trim())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Sets or updates a candidate's password on-demand.
     * 
     * @param id          Candidate ID
     * @param newPassword New password string
     * @return true if updated, false otherwise
     */
    public boolean setCandidatePassword(int id, String newPassword) {
        Candidate candidate = candidateRepo.findById(id);
        if (candidate != null && Validation.isValidPassword(newPassword)) {
            candidate.setPassword(newPassword.trim());
            FileManager.saveCandidates(candidateRepo.getAllCandidates());
            FileManager.writeLog("Candidate ID " + id + " set secure password.");
            return true;
        }
        return false;
    }

    /**
     * Gets candidate by ID.
     * @param id candidate ID
     * @return candidate object or null
     */
    public Candidate getCandidateById(int id) {
        return candidateRepo.findById(id);
    }

    /**
     * Gets all candidates list.
     * @return ArrayList of candidates
     */
    public ArrayList<Candidate> getAllCandidates() {
        return candidateRepo.getAllCandidates();
    }

    /**
     * Displays all candidates using StringBuilder formatting.
     */
    public void viewCandidates() {
        ArrayList<Candidate> candidates = candidateRepo.getAllCandidates();
        if (candidates.isEmpty()) {
            System.out.println("⚠️ No candidates found in the system.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=======================================================================================================================\n");
        sb.append(String.format("| %-6s | %-20s | %-25s | %-12s | %-16s | %-8s | %-10s |\n", "ID", "NAME", "EMAIL", "PHONE", "SKILL", "EXP(YRS)", "STATUS"));
        sb.append("=======================================================================================================================\n");

        for (Candidate c : candidates) {
            sb.append(String.format("| %-6d | %-20s | %-25s | %-12s | %-16s | %-8.1f | %-10s |\n",
                    c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getSkill(), c.getExperience(), c.getStatus()));
        }
        sb.append("=======================================================================================================================\n");

        System.out.print(sb.toString());
    }

    /**
     * Method Overloading: Search candidate by ID.
     * 
     * @param id Candidate ID
     * @return Candidate if found, null otherwise
     */
    public Candidate searchCandidate(int id) {
        Candidate candidate = candidateRepo.findById(id);
        if (candidate != null) {
            System.out.println("\n✅ Candidate Found:");
            candidate.displayDetails();
        } else {
            System.out.println("⚠️ Candidate with ID " + id + " not found.");
        }
        return candidate;
    }

    /**
     * Method Overloading: Search candidates by Name substring.
     * 
     * @param name Candidate Name
     * @return List of matching Candidate objects
     */
    public List<Candidate> searchCandidate(String name) {
        List<Candidate> results = candidateRepo.findByName(name);
        if (results.isEmpty()) {
            System.out.println("⚠️ No candidates matching name '" + name + "' were found.");
        } else {
            System.out.println("\n✅ Found " + results.size() + " matching candidate(s):");
            for (Candidate c : results) {
                c.displayDetails();
            }
        }
        return results;
    }

    /**
     * Updates an existing candidate's information.
     * 
     * @param id            Candidate ID to update
     * @param newSkill      Updated skill
     * @param newExperience Updated experience
     * @param newStatus     Updated status
     * @return true if updated successfully, false otherwise
     */
    public boolean updateCandidate(int id, String newSkill, double newExperience, String newStatus) {
        Candidate candidate = candidateRepo.findById(id);
        if (candidate == null) {
            System.out.println("❌ Candidate with ID " + id + " does not exist.");
            return false;
        }

        if (!Validation.isValidSkill(newSkill)) {
            System.out.println("❌ Invalid Skill value.");
            return false;
        }
        if (!Validation.isPositiveExperience(newExperience)) {
            System.out.println("❌ Invalid Experience value.");
            return false;
        }

        candidate.setSkill(newSkill.trim());
        candidate.setExperience(newExperience);
        if (newStatus != null && !newStatus.trim().isEmpty()) {
            candidate.setStatus(newStatus.trim());
        }

        boolean updated = candidateRepo.updateCandidate(candidate);
        if (updated) {
            System.out.println("✅ Candidate ID " + id + " updated successfully.");
            FileManager.writeLog("Updated candidate details for Candidate ID: " + id);
        } else {
            System.out.println("❌ Failed to update candidate.");
        }
        return updated;
    }

    /**
     * Deletes a candidate by ID and cascade deletes associated applications.
     * 
     * @param id Candidate ID to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteCandidate(int id) {
        boolean deleted = candidateRepo.deleteCandidate(id);
        if (deleted) {
            applicationRepo.deleteApplicationsByCandidateId(id);
            System.out.println("✅ Candidate ID " + id + " and associated applications deleted successfully.");
            FileManager.writeLog("Deleted candidate ID: " + id + " and associated applications.");
        } else {
            System.out.println("❌ Candidate with ID " + id + " not found or could not be deleted.");
        }
        return deleted;
    }

    /**
     * Gets total candidate count.
     * 
     * @return total candidate count
     */
    public int countCandidates() {
        return candidateRepo.getCount();
    }
}
