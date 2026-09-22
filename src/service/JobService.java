package service;

import model.Job;
import repository.ApplicationRepository;
import repository.JobRepository;
import util.FileManager;
import util.Validation;

import java.util.ArrayList;
import java.util.List;

/**
 * JobService handles all business logic related to Job postings in TalentFlow.
 * Enforces dynamic unique Job IDs and cascade deletion integrity.
 */
public class JobService {
    private final JobRepository jobRepo;
    private final ApplicationRepository applicationRepo;

    public JobService() {
        this.jobRepo = new JobRepository();
        this.applicationRepo = new ApplicationRepository();
    }

    /**
     * Posts a new job opening with dynamic unique Job ID assignment.
     * 
     * @param title         Job Title
     * @param company       Company Name
     * @param location      Location / Remote
     * @param salaryRange   Compensation budget range
     * @param requiredSkill Required Skill
     * @return Created Job object or null if validation fails
     */
    public Job addJob(String title, String company, String location, String salaryRange, String requiredSkill) {
        if (title == null || title.trim().isEmpty()) {
            System.out.println("❌ Job Title cannot be empty!");
            return null;
        }
        if (company == null || company.trim().isEmpty()) {
            System.out.println("❌ Company name cannot be empty!");
            return null;
        }
        if (location == null || location.trim().isEmpty()) {
            System.out.println("❌ Location cannot be empty!");
            return null;
        }
        if (!Validation.isValidSkill(requiredSkill)) {
            System.out.println("❌ Required skill cannot be empty!");
            return null;
        }

        int id = jobRepo.getNextId();
        Job job = new Job(id, title.trim(), company.trim(), location.trim(), salaryRange, requiredSkill.trim());
        boolean success = jobRepo.addJob(job);

        if (success) {
            System.out.println("✅ Job posted successfully with Unique Job ID: " + job.getJobId());
            FileManager.writeLog("Posted new job: " + job.getTitle() + " (Salary: " + job.getSalaryRange() + ") at " + job.getCompany() + " (Job ID: " + job.getJobId() + ")");
            return job;
        } else {
            System.out.println("❌ Failed to post job.");
            return null;
        }
    }

    public Job addJob(String title, String company, String location, String requiredSkill) {
        return addJob(title, company, location, "Not Disclosed", requiredSkill);
    }

    /**
     * Displays all job postings using StringBuilder formatting.
     * PUBLIC VIEW: Hides the Required Skill column to protect matching integrity.
     */
    public void viewJobs() {
        ArrayList<Job> jobs = jobRepo.getAllJobs();
        if (jobs.isEmpty()) {
            System.out.println("⚠️ No job postings currently available.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=======================================================================================================================\n");
        sb.append(String.format("| %-6s | %-28s | %-24s | %-20s | %-18s |\n", "JOB ID", "TITLE", "COMPANY", "LOCATION", "SALARY RANGE"));
        sb.append("=======================================================================================================================\n");

        for (Job j : jobs) {
            sb.append(String.format("| %-6d | %-28s | %-24s | %-20s | %-18s |\n",
                    j.getJobId(), j.getTitle(), j.getCompany(), j.getLocation(), j.getSalaryRange()));
        }
        sb.append("=======================================================================================================================\n");

        System.out.print(sb.toString());
    }

    /**
     * Method Overloading: Search job by Job ID.
     * 
     * @param id Job ID
     * @return Job object if found, null otherwise
     */
    public Job searchJob(int id) {
        Job job = jobRepo.findById(id);
        if (job != null) {
            System.out.println("\n✅ Job Found:");
            job.displayJob();
        } else {
            System.out.println("⚠️ Job with ID " + id + " not found.");
        }
        return job;
    }

    /**
     * Method Overloading: Search jobs by Title substring.
     * 
     * @param title Title substring
     * @return List of matching Job objects
     */
    public List<Job> searchJob(String title) {
        List<Job> results = jobRepo.findByTitle(title);
        if (results.isEmpty()) {
            System.out.println("⚠️ No jobs matching title '" + title + "' were found.");
        } else {
            System.out.println("\n✅ Found " + results.size() + " matching job(s):");
            for (Job j : results) {
                j.displayJob();
            }
        }
        return results;
    }

    /**
     * Gets all posted jobs in system.
     * @return list of jobs
     */
    public ArrayList<Job> getAllJobs() {
        return jobRepo.getAllJobs();
    }

    /**
     * Updates details of an existing job posting.
     * 
     * @param jobId            Job ID
     * @param newTitle         Updated Title
     * @param newLocation      Updated Location
     * @param newRequiredSkill Updated Required Skill
     * @return true if updated, false otherwise
     */
    public boolean updateJob(int jobId, String newTitle, String newLocation, String newRequiredSkill) {
        Job job = jobRepo.findById(jobId);
        if (job == null) {
            System.out.println("❌ Job with ID " + jobId + " does not exist.");
            return false;
        }

        if (newTitle != null && !newTitle.trim().isEmpty()) {
            job.setTitle(newTitle.trim());
        }
        if (newLocation != null && !newLocation.trim().isEmpty()) {
            job.setLocation(newLocation.trim());
        }
        if (newRequiredSkill != null && !newRequiredSkill.trim().isEmpty()) {
            job.setRequiredSkill(newRequiredSkill.trim());
        }

        boolean updated = jobRepo.updateJob(job);
        if (updated) {
            System.out.println("✅ Job ID " + jobId + " updated successfully.");
            FileManager.writeLog("Updated job posting details for Job ID: " + jobId);
        } else {
            System.out.println("❌ Failed to update job.");
        }
        return updated;
    }

    /**
     * Deletes a job posting by ID and cascade deletes associated applications.
     * 
     * @param id Job ID to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteJob(int id) {
        boolean deleted = jobRepo.deleteJob(id);
        if (deleted) {
            applicationRepo.deleteApplicationsByJobId(id);
            System.out.println("✅ Job ID " + id + " and associated applications deleted successfully.");
            FileManager.writeLog("Deleted Job ID: " + id + " and associated applications.");
        } else {
            System.out.println("❌ Job with ID " + id + " not found or could not be deleted.");
        }
        return deleted;
    }

    /**
     * Gets total number of job postings.
     * 
     * @return total jobs count
     */
    public int countJobs() {
        return jobRepo.getCount();
    }
}
