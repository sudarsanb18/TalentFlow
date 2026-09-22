package service;

import model.Application;
import model.Candidate;
import model.Job;
import repository.ApplicationRepository;
import repository.CandidateRepository;
import repository.JobRepository;
import util.FileManager;

import java.util.ArrayList;

/**
 * ApplicationService manages candidate applications for job postings.
 * Supports Candidate View vs Recruiter View, Status Transitions, Application Withdrawal, and Automated Skill Matching Engine.
 */
public class ApplicationService {
    private final ApplicationRepository applicationRepo;
    private final CandidateRepository candidateRepo;
    private final JobRepository jobRepo;

    public ApplicationService() {
        this.applicationRepo = new ApplicationRepository();
        this.candidateRepo = new CandidateRepository();
        this.jobRepo = new JobRepository();
    }

    /**
     * Creates a new application linking candidateId to jobId.
     * 
     * @param candidateId ID of the candidate applying
     * @param jobId       ID of the job being applied for
     * @return Application instance if successful, null otherwise
     */
    public Application applyJob(int candidateId, int jobId) {
        Candidate candidate = candidateRepo.findById(candidateId);
        if (candidate == null) {
            System.out.println("❌ Application failed: Candidate with ID " + candidateId + " does not exist.");
            return null;
        }

        Job job = jobRepo.findById(jobId);
        if (job == null) {
            System.out.println("❌ Application failed: Job with ID " + jobId + " does not exist.");
            return null;
        }

        // Check for duplicate active application
        for (Application app : applicationRepo.getAllApplications()) {
            if (app.getCandidateId() == candidateId && app.getJobId() == jobId) {
                System.out.println("⚠️ Candidate ID " + candidateId + " has already applied for Job ID " + jobId + "!");
                return null;
            }
        }

        int appId = applicationRepo.getNextId();
        Application application = new Application(appId, candidateId, jobId, "Applied");
        boolean success = applicationRepo.addApplication(application);

        if (success) {
            System.out.println("✅ Application submitted successfully! Application ID: " + appId);
            FileManager.writeLog("Candidate ID " + candidateId + " applied for Job ID " + jobId + " (App ID: " + appId + ")");
            return application;
        } else {
            System.out.println("❌ Failed to create application.");
            return null;
        }
    }

    /**
     * Candidate Application Withdrawal Feature
     * Allows candidate to withdraw their active job application cleanly.
     * 
     * @param applicationId Application ID to withdraw
     * @param candidateId   Candidate ID owning the application
     * @return true if withdrawn successfully, false otherwise
     */
    public boolean withdrawApplication(int applicationId, int candidateId) {
        Application app = applicationRepo.findById(applicationId);
        if (app == null) {
            System.out.println("❌ Application ID " + applicationId + " not found.");
            return false;
        }
        if (app.getCandidateId() != candidateId) {
            System.out.println("❌ Access Denied! Application ID " + applicationId + " does not belong to Candidate ID " + candidateId + ".");
            return false;
        }

        boolean updated = applicationRepo.updateStatus(applicationId, "Withdrawn");
        if (updated) {
            System.out.println("✅ Application ID " + applicationId + " has been successfully Withdrawn.");
            FileManager.writeLog("Candidate ID " + candidateId + " withdrew Application ID " + applicationId);
        } else {
            System.out.println("❌ Failed to withdraw application.");
        }
        return updated;
    }

    /**
     * Gets all applications.
     * @return list of applications
     */
    public ArrayList<Application> getAllApplications() {
        return applicationRepo.getAllApplications();
    }

    /**
     * DUAL INTERFACE - Candidate View: Displays applications submitted by a specific candidate.
     * 
     * @param candidateId Candidate ID
     */
    public void viewApplicationsForCandidate(int candidateId) {
        Candidate candidate = candidateRepo.findById(candidateId);
        if (candidate == null) {
            System.out.println("❌ Candidate record not found.");
            return;
        }

        ArrayList<Application> allApps = applicationRepo.getAllApplications();
        ArrayList<Application> candApps = new ArrayList<>();
        for (Application app : allApps) {
            if (app.getCandidateId() == candidateId) {
                candApps.add(app);
            }
        }

        if (candApps.isEmpty()) {
            System.out.println("⚠️ You currently have 0 active job applications.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=========================================================================================================\n");
        sb.append(String.format("                     CANDIDATE APPLICATIONS PORTAL | CANDIDATE: %-25s\n", candidate.getName()));
        sb.append("=========================================================================================================\n");
        sb.append(String.format("| %-8s | %-8s | %-24s | %-20s | %-12s |\n", "APP ID", "JOB ID", "JOB TITLE", "COMPANY", "STATUS"));
        sb.append("=========================================================================================================\n");

        for (Application app : candApps) {
            Job job = jobRepo.findById(app.getJobId());
            String jobTitle = (job != null) ? job.getTitle() : "Unknown";
            String company = (job != null) ? job.getCompany() : "Unknown";

            sb.append(String.format("| %-8d | %-8d | %-24s | %-20s | %-12s |\n",
                    app.getApplicationId(), app.getJobId(), jobTitle, company, app.getStatus()));
        }
        sb.append("=========================================================================================================\n");

        System.out.print(sb.toString());
    }

    /**
     * DUAL INTERFACE - Recruiter View: Displays all job applications across all posted jobs.
     */
    public void viewApplicationsForRecruiter() {
        ArrayList<Application> apps = applicationRepo.getAllApplications();
        if (apps.isEmpty()) {
            System.out.println("⚠️ No job applications found in the system.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=========================================================================================================\n");
        sb.append("                               RECRUITER APPLICANT TRACKING DASHBOARD                    \n");
        sb.append("=========================================================================================================\n");
        sb.append(String.format("| %-8s | %-8s | %-20s | %-8s | %-22s | %-12s |\n",
                "APP ID", "CAND ID", "CANDIDATE NAME", "JOB ID", "JOB TITLE", "STATUS"));
        sb.append("=========================================================================================================\n");

        for (Application app : apps) {
            Candidate candidate = candidateRepo.findById(app.getCandidateId());
            Job job = jobRepo.findById(app.getJobId());

            String candName = (candidate != null) ? candidate.getName() : "Unknown";
            String jobTitle = (job != null) ? job.getTitle() : "Unknown";

            sb.append(String.format("| %-8d | %-8d | %-20s | %-8d | %-22s | %-12s |\n",
                    app.getApplicationId(), app.getCandidateId(), candName, app.getJobId(), jobTitle, app.getStatus()));
        }
        sb.append("=========================================================================================================\n");

        System.out.print(sb.toString());
    }

    /**
     * AUTOMATED SKILL MATCHING ENGINE
     * Automatically compares candidate skills against job required skills across recruiters,
     * matching candidates with eligible job openings and creating applications.
     */
    public void runAutomatedSkillMatching() {
        ArrayList<Candidate> candidates = candidateRepo.getAllCandidates();
        ArrayList<Job> jobs = jobRepo.getAllJobs();

        if (candidates.isEmpty() || jobs.isEmpty()) {
            System.out.println("⚠️ Skill Matching Engine requires registered Candidates and Job openings.");
            return;
        }

        System.out.println("\n⚡ RUNNING AUTOMATED SKILL MATCHING & CANDIDATE ALLOCATION ENGINE...");
        int matchesFound = 0;
        int newAppsCreated = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("\n=========================================================================================================\n");
        sb.append("                                AUTOMATED SKILL MATCHING RESULTS                                         \n");
        sb.append("=========================================================================================================\n");
        sb.append(String.format("| %-18s | %-16s | %-22s | %-16s | %-12s |\n",
                "CANDIDATE NAME", "CANDIDATE SKILL", "JOB TITLE", "JOB REQ SKILL", "MATCH ACTION"));
        sb.append("=========================================================================================================\n");

        for (Candidate c : candidates) {
            for (Job j : jobs) {
                // Perform case-insensitive skill matching
                boolean isMatch = c.getSkill().toLowerCase().contains(j.getRequiredSkill().toLowerCase()) ||
                                  j.getRequiredSkill().toLowerCase().contains(c.getSkill().toLowerCase());

                if (isMatch) {
                    matchesFound++;
                    boolean exists = false;
                    for (Application app : applicationRepo.getAllApplications()) {
                        if (app.getCandidateId() == c.getId() && app.getJobId() == j.getJobId()) {
                            exists = true;
                            break;
                        }
                    }

                    String action;
                    if (!exists) {
                        int appId = applicationRepo.getNextId();
                        applicationRepo.addApplication(new Application(appId, c.getId(), j.getJobId(), "Applied"));
                        newAppsCreated++;
                        action = "✅ AUTO-ASSIGNED";
                        FileManager.writeLog("Automated Engine matched Candidate " + c.getName() + " to Job " + j.getTitle() + " (App ID: " + appId + ")");
                    } else {
                        action = "ℹ️ ALREADY APPLIED";
                    }

                    sb.append(String.format("| %-18s | %-16s | %-22s | %-16s | %-12s |\n",
                            c.getName(), c.getSkill(), j.getTitle(), j.getRequiredSkill(), action));
                }
            }
        }
        sb.append("=========================================================================================================\n");
        System.out.print(sb.toString());
        System.out.println("📊 Skill Engine Summary: " + matchesFound + " skill matches evaluated. " + newAppsCreated + " new candidate job assignments created.\n");
    }

    /**
     * Updates an application status following ATS workflow: Applied -> Interview -> Selected / Rejected.
     * 
     * @param applicationId Application ID
     * @param newStatus     New status string ("Interview", "Selected", "Rejected", "Withdrawn")
     * @return true if status transition succeeded, false otherwise
     */
    public boolean updateApplicationStatus(int applicationId, String newStatus) {
        Application app = applicationRepo.findById(applicationId);
        if (app == null) {
            System.out.println("❌ Application with ID " + applicationId + " does not exist.");
            return false;
        }

        String formattedStatus = newStatus.trim();
        if (!formattedStatus.equalsIgnoreCase("Applied") &&
            !formattedStatus.equalsIgnoreCase("Interview") &&
            !formattedStatus.equalsIgnoreCase("Selected") &&
            !formattedStatus.equalsIgnoreCase("Rejected") &&
            !formattedStatus.equalsIgnoreCase("Withdrawn")) {
            System.out.println("❌ Invalid Status choice! Allowed values: Applied, Interview, Selected, Rejected, Withdrawn");
            return false;
        }

        boolean updated = applicationRepo.updateStatus(applicationId, formattedStatus);
        if (updated) {
            System.out.println("✅ Application ID " + applicationId + " status updated to '" + formattedStatus + "'.");
            FileManager.writeLog("Application ID " + applicationId + " status updated from '" + app.getStatus() + "' to '" + formattedStatus + "'");
        } else {
            System.out.println("❌ Failed to update application status.");
        }
        return updated;
    }
}
