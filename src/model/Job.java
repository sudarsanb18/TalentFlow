package model;

/**
 * Job class representing a job opening posted by a recruiter in TalentFlow.
 * Includes salary range / compensation budget. Demonstrates Encapsulation.
 */
public class Job {
    private int jobId;
    private String title;
    private String company;
    private String location;
    private String salaryRange;
    private String requiredSkill;

    /**
     * Default Constructor
     */
    public Job() {
        this.jobId = 0;
        this.title = "";
        this.company = "";
        this.location = "";
        this.salaryRange = "Not Disclosed";
        this.requiredSkill = "";
    }

    /**
     * Parameterized Constructor
     * 
     * @param jobId         Unique Job Identifier
     * @param title         Job Title
     * @param company       Company Name
     * @param location      Job Location / Remote status
     * @param salaryRange   Compensation budget (e.g. "10 - 15 LPA")
     * @param requiredSkill Key required skill for position
     */
    public Job(int jobId, String title, String company, String location, String salaryRange, String requiredSkill) {
        this.jobId = jobId;
        this.title = title;
        this.company = company;
        this.location = location;
        this.salaryRange = (salaryRange == null || salaryRange.trim().isEmpty()) ? "Not Disclosed" : salaryRange;
        this.requiredSkill = requiredSkill;
    }

    public Job(int jobId, String title, String company, String location, String requiredSkill) {
        this(jobId, title, company, location, "Not Disclosed", requiredSkill);
    }

    /**
     * Gets Job ID.
     * @return jobId
     */
    public int getJobId() {
        return jobId;
    }

    /**
     * Sets Job ID.
     * @param jobId unique job identifier
     */
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    /**
     * Gets Job Title.
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets Job Title.
     * @param title job title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets Company Name.
     * @return company
     */
    public String getCompany() {
        return company;
    }

    /**
     * Sets Company Name.
     * @param company company name
     */
    public void setCompany(String company) {
        this.company = company;
    }

    /**
     * Gets Location.
     * @return location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets Location.
     * @param location job location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets Salary Range.
     * @return salaryRange
     */
    public String getSalaryRange() {
        return salaryRange;
    }

    /**
     * Sets Salary Range.
     * @param salaryRange compensation budget
     */
    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    /**
     * Gets Required Skill.
     * @return requiredSkill
     */
    public String getRequiredSkill() {
        return requiredSkill;
    }

    /**
     * Sets Required Skill.
     * @param requiredSkill required skill
     */
    public void setRequiredSkill(String requiredSkill) {
        this.requiredSkill = requiredSkill;
    }

    /**
     * Displays detailed job description on console.
     */
    public void displayJob() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("JOB POSTING | ID: %-5d | Title: %-25s | Company: %-20s\n", jobId, title, company);
        System.out.printf("            | Location: %-18s | Salary: %-18s | Skill: %-15s\n", location, salaryRange, requiredSkill);
        System.out.println("--------------------------------------------------------------------------------");
    }

    /**
     * Overridden toString method for string representation of Job.
     * @return formatted job string
     */
    @Override
    public String toString() {
        return "Job [JobId=" + jobId + ", Title=" + title + ", Company=" + company +
               ", Location=" + location + ", Salary=" + salaryRange + ", RequiredSkill=" + requiredSkill + "]";
    }
}
