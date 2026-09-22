package model;

/**
 * Candidate class representing a job applicant in the TalentFlow ATS.
 * Includes resume summary bio. Demonstrates Inheritance, Encapsulation, and Polymorphism.
 */
public class Candidate extends User {
    private String skill;
    private double experience;
    private String status;
    private String resumeSummary;

    /**
     * Default Constructor
     */
    public Candidate() {
        super();
        this.skill = "";
        this.experience = 0.0;
        this.status = "Available";
        this.resumeSummary = "No profile summary provided";
    }

    /**
     * Parameterized Constructor
     * 
     * @param id            Unique candidate ID
     * @param name          Candidate full name
     * @param email         Candidate email address
     * @param phone         Candidate phone number
     * @param password      Candidate password
     * @param skill         Primary technical skill
     * @param experience    Years of professional experience
     * @param status        Candidate availability or status
     * @param resumeSummary Professional resume summary bio
     */
    public Candidate(int id, String name, String email, String phone, String password, String skill, double experience, String status, String resumeSummary) {
        super(id, name, email, phone, password);
        this.skill = skill;
        this.experience = experience;
        this.status = status;
        this.resumeSummary = (resumeSummary == null || resumeSummary.trim().isEmpty()) ? "No profile summary provided" : resumeSummary;
    }

    public Candidate(int id, String name, String email, String phone, String password, String skill, double experience, String status) {
        this(id, name, email, phone, password, skill, experience, status, "No profile summary provided");
    }

    /**
     * Gets primary skill.
     * @return skill
     */
    public String getSkill() {
        return skill;
    }

    /**
     * Sets primary skill.
     * @param skill candidate skill
     */
    public void setSkill(String skill) {
        this.skill = skill;
    }

    /**
     * Gets total years of experience.
     * @return experience in years
     */
    public double getExperience() {
        return experience;
    }

    /**
     * Sets total years of experience.
     * @param experience experience in years
     */
    public void setExperience(double experience) {
        this.experience = experience;
    }

    /**
     * Gets candidate status.
     * @return candidate status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets candidate status.
     * @param status candidate status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets Resume Summary bio.
     * @return resumeSummary
     */
    public String getResumeSummary() {
        return resumeSummary;
    }

    /**
     * Sets Resume Summary bio.
     * @param resumeSummary summary bio
     */
    public void setResumeSummary(String resumeSummary) {
        this.resumeSummary = resumeSummary;
    }

    /**
     * Implementation of abstract displayDetails method from User (Polymorphism).
     */
    @Override
    public void displayDetails() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("CANDIDATE DETAILS | ID: %-5d | Name: %-20s | Email: %-25s\n", getId(), getName(), getEmail());
        System.out.printf("                  | Phone: %-15s | Skill: %-15s | Exp: %-4.1f yrs | Status: %-10s\n", getPhone(), skill, experience, status);
        System.out.printf("                  | Bio Summary: %-55s\n", resumeSummary);
        System.out.println("--------------------------------------------------------------------------------");
    }

    /**
     * Overridden toString method for string representation of Candidate.
     * @return formatted candidate string
     */
    @Override
    public String toString() {
        return "Candidate [ID=" + getId() + ", Name=" + getName() + ", Email=" + getEmail() +
               ", Phone=" + getPhone() + ", Skill=" + skill + ", Experience=" + experience + " yrs, Status=" + status + ", Summary=" + resumeSummary + "]";
    }
}
