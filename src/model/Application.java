package model;

/**
 * Application class representing a job application filed by a candidate for a specific job in TalentFlow.
 * Demonstrates Encapsulation.
 */
public class Application {
    private int applicationId;
    private int candidateId;
    private int jobId;
    private String status;

    /**
     * Default Constructor
     */
    public Application() {
        this.applicationId = 0;
        this.candidateId = 0;
        this.jobId = 0;
        this.status = "Applied";
    }

    /**
     * Parameterized Constructor
     * 
     * @param applicationId Unique Application ID
     * @param candidateId   Candidate ID applying
     * @param jobId         Job ID applied for
     * @param status        Application status (Applied, Interview, Selected, Rejected)
     */
    public Application(int applicationId, int candidateId, int jobId, String status) {
        this.applicationId = applicationId;
        this.candidateId = candidateId;
        this.jobId = jobId;
        this.status = status;
    }

    /**
     * Gets Application ID.
     * @return applicationId
     */
    public int getApplicationId() {
        return applicationId;
    }

    /**
     * Sets Application ID.
     * @param applicationId unique application identifier
     */
    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Gets Candidate ID associated with application.
     * @return candidateId
     */
    public int getCandidateId() {
        return candidateId;
    }

    /**
     * Sets Candidate ID.
     * @param candidateId candidate identifier
     */
    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    /**
     * Gets Job ID associated with application.
     * @return jobId
     */
    public int getJobId() {
        return jobId;
    }

    /**
     * Sets Job ID.
     * @param jobId job identifier
     */
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    /**
     * Gets Application Status.
     * @return status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets Application Status.
     * @param status application status string
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Displays formatted details of the application.
     */
    public void displayApplication() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("APPLICATION | App ID: %-5d | Candidate ID: %-5d | Job ID: %-5d | Status: %-15s\n",
                applicationId, candidateId, jobId, status);
        System.out.println("--------------------------------------------------------------------------------");
    }

    /**
     * Overridden toString method for string representation of Application.
     * @return formatted application string
     */
    @Override
    public String toString() {
        return "Application [ApplicationId=" + applicationId + ", CandidateId=" + candidateId +
               ", JobId=" + jobId + ", Status=" + status + "]";
    }
}
