package util;

import java.util.regex.Pattern;

/**
 * Utility class providing static methods for user input validation.
 * Enforces strict password requirements (8+ chars, upper, lower, digit, special char)
 * and phone number prefix rules (10 digits starting with 6, 7, 8, or 9).
 */
public class Validation {

    // Strict email regex: contains @ and ends with valid domain extension
    private static final String STRICT_EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(com|net|org|edu|in|co|io|dev|gov|ac\\.in)$";
    private static final Pattern emailPattern = Pattern.compile(STRICT_EMAIL_REGEX, Pattern.CASE_INSENSITIVE);

    // 10-digit phone number regex starting with 6, 7, 8, or 9
    private static final String PHONE_REGEX = "^[6-9][0-9]{9}$";
    private static final Pattern phonePattern = Pattern.compile(PHONE_REGEX);

    // Strict password regex: min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";
    private static final Pattern passwordPattern = Pattern.compile(PASSWORD_REGEX);

    /**
     * Validates if email string contains '@' and ends with valid common domain (.com, .net, etc.).
     * 
     * @param email Input email string
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return emailPattern.matcher(email.trim()).matches();
    }

    /**
     * Validates phone number: Must be 10 numerical digits starting with 6, 7, 8, or 9.
     * 
     * @param phone Input phone string
     * @return true if valid 10-digit number starting with 6-9, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return phonePattern.matcher(phone.trim()).matches();
    }

    /**
     * Validates password strength:
     * Minimum 8 characters long, containing uppercase, lowercase, number, and special character.
     * 
     * @param password Input password string
     * @return true if password meets security policy, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        return passwordPattern.matcher(password.trim()).matches();
    }

    /**
     * Validates candidate or recruiter name.
     * Name must contain non-space alphabetic characters and be at least 2 characters long.
     * 
     * @param name Input name string
     * @return true if valid, false otherwise
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().length() < 2) {
            return false;
        }
        return name.trim().matches("^[a-zA-Z\\s.]+$");
    }

    /**
     * Validates experience in years.
     * Must be non-negative (>= 0.0) and realistic (<= 60.0).
     * 
     * @param experience Years of experience
     * @return true if valid positive experience, false otherwise
     */
    public static boolean isPositiveExperience(double experience) {
        return experience >= 0.0 && experience <= 60.0;
    }

    /**
     * Validates skill field.
     * Skill string must not be empty or blank.
     * 
     * @param skill Input skill string
     * @return true if non-empty, false otherwise
     */
    /** Allowed candidate profile statuses. */
    public static final String[] CANDIDATE_STATUSES = {"Available", "Not Available", "Hired"};

    /**
     * Returns the properly capitalised status when it is one of CANDIDATE_STATUSES, otherwise null.
     *
     * @param status Input status string
     * @return normalised status or null
     */
    public static String normalizeCandidateStatus(String status) {
        if (status == null) return null;
        for (String allowed : CANDIDATE_STATUSES) {
            if (allowed.equalsIgnoreCase(status.trim())) return allowed;
        }
        return null;
    }

    public static boolean isValidSkill(String skill) {
        return skill != null && !skill.trim().isEmpty();
    }
}
