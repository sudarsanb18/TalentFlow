package service;

import model.Application;
import model.Candidate;
import model.Job;
import repository.ApplicationRepository;
import repository.CandidateRepository;
import repository.JobMatchRepository;
import repository.JobMatchRepository.JobRule;
import repository.JobRepository;
import util.FileManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SmartMatchService adds the weighted match percentage and the ranked shortlist on top of the existing
 * services. It only reads and extends data; the original services and tables are not modified.
 */
public class SmartMatchService {
    public static final int DEFAULT_CUTOFF = 60;

    private final JobRepository jobRepo = new JobRepository();
    private final CandidateRepository candidateRepo = new CandidateRepository();
    private final ApplicationRepository applicationRepo = new ApplicationRepository();
    private final JobMatchRepository ruleRepo = new JobMatchRepository();

    // ---------------------------------------------------------------- job rules

    /** Sets the preferred skills and minimum experience of a job owned by the given company. */
    public boolean setJobRule(int jobId, String company, String preferredSkills, double minExperience) {
        Job job = jobRepo.findById(jobId);
        if (job == null) {
            System.out.println("Job ID " + jobId + " does not exist.");
            return false;
        }
        if (!ownsJob(job, company)) {
            System.out.println("Access denied: Job ID " + jobId + " belongs to another company.");
            return false;
        }
        if (minExperience < 0) {
            System.out.println("Minimum experience cannot be negative.");
            return false;
        }
        String cleaned = String.join(", ", MatchScorer.parseSkills(preferredSkills));
        boolean saved = ruleRepo.saveRule(new JobRule(jobId, cleaned, minExperience));
        if (saved) {
            System.out.println("Match rules saved for Job ID " + jobId + ": preferred = ["
                    + (cleaned.isEmpty() ? "none" : cleaned) + "], minimum experience = " + minExperience + " yrs.");
            FileManager.writeLog("Match rules set for Job ID " + jobId + " (preferred: " + cleaned
                    + ", min experience: " + minExperience + ")");
        } else {
            System.out.println("Failed to save match rules.");
        }
        return saved;
    }

    public MatchScorer.Result scoreFor(Candidate c, Job j) {
        JobRule rule = ruleRepo.findRule(j.getJobId());
        return MatchScorer.score(c.getSkill(), c.getExperience(), j.getRequiredSkill(),
                rule.preferredSkills(), rule.minExperience());
    }

    // ---------------------------------------------------------------- weighted matching engine

    /** Scores every candidate against the company's jobs and auto-applies those at or above the cut-off. */
    public int runWeightedMatching(String company, int cutoff) {
        if (cutoff < 0 || cutoff > 100) {
            System.out.println("Cut-off must be between 0 and 100.");
            return 0;
        }
        ArrayList<Candidate> candidates = candidateRepo.getAllCandidates();
        List<Job> jobs = jobsOfCompany(company);
        if (candidates.isEmpty() || jobs.isEmpty()) {
            System.out.println("Weighted matching needs registered candidates and at least one job of your company.");
            return 0;
        }

        Set<String> existing = new HashSet<>();
        for (Application a : applicationRepo.getAllApplications()) {
            existing.add(a.getCandidateId() + ":" + a.getJobId());
        }

        System.out.println("\nWEIGHTED SKILL MATCHING (cut-off " + cutoff + "%)");
        System.out.println("===================================================================================================");
        System.out.printf("| %-18s | %-22s | %-7s | %-24s | %-16s |%n",
                "CANDIDATE", "JOB", "MATCH", "MISSING REQUIRED", "ACTION");
        System.out.println("===================================================================================================");

        int created = 0;
        for (Job j : jobs) {
            for (Candidate c : candidates) {
                MatchScorer.Result r = scoreFor(c, j);
                String action;
                if (r.percent() < cutoff) {
                    action = "below cut-off";
                } else if (existing.contains(c.getId() + ":" + j.getJobId())) {
                    action = "already applied";
                } else {
                    int appId = applicationRepo.getNextId();
                    if (applicationRepo.addApplication(new Application(appId, c.getId(), j.getJobId(), "Applied"))) {
                        existing.add(c.getId() + ":" + j.getJobId());
                        created++;
                        action = "AUTO-APPLIED";
                        FileManager.writeLog("Weighted matching applied Candidate " + c.getName() + " (" + r.percent()
                                + "%) to Job " + j.getTitle() + " [Job ID " + j.getJobId() + ", App ID " + appId
                                + ", cut-off " + cutoff + "%]");
                    } else {
                        action = "failed";
                    }
                }
                System.out.printf("| %-18s | %-22s | %5d %% | %-24s | %-16s |%n",
                        cut(c.getName(), 18), cut(j.getTitle(), 22), r.percent(),
                        cut(r.missingRequired().isEmpty() ? "-" : String.join(", ", r.missingRequired()), 24), action);
            }
        }
        System.out.println("===================================================================================================");
        System.out.println("Summary: " + created + " new application(s) created at or above " + cutoff + "%.");
        return created;
    }

    // ---------------------------------------------------------------- ranked shortlist

    /** All applicants of a job, scored and sorted: match % desc, experience desc, earlier application first. */
    public List<RankedApplicant> getRankedApplicants(int jobId) {
        Job job = jobRepo.findById(jobId);
        if (job == null) return new ArrayList<>();
        Map<Integer, Long> appliedAt = ruleRepo.getAppliedAtByJob(jobId);
        List<RankedApplicant> list = new ArrayList<>();
        for (Application app : applicationRepo.getAllApplications()) {
            if (app.getJobId() != jobId) continue;
            Candidate c = candidateRepo.findById(app.getCandidateId());
            if (c == null) continue;
            MatchScorer.Result r = scoreFor(c, job);
            list.add(new RankedApplicant(app.getApplicationId(), c.getId(), c.getName(), c.getSkill(),
                    c.getExperience(), r.percent(), app.getStatus(),
                    appliedAt.getOrDefault(app.getApplicationId(), Long.MAX_VALUE), r.missingRequired()));
        }
        return RankedApplicant.rank(list);
    }

    /** Shows the ranked list of one job after checking that the job belongs to the recruiter's company. */
    public List<RankedApplicant> showRankedShortlist(int jobId, String company) {
        Job job = jobRepo.findById(jobId);
        if (job == null) {
            System.out.println("Job ID " + jobId + " does not exist.");
            return null;
        }
        if (!ownsJob(job, company)) {
            System.out.println("Access denied: Job ID " + jobId + " belongs to another company.");
            return null;
        }
        List<RankedApplicant> ranked = getRankedApplicants(jobId);
        System.out.println("\nRANKED SHORTLIST - " + job.getTitle() + " (Job ID " + jobId + ")");
        System.out.println("=========================================================================================================");
        System.out.printf("| %-4s | %-18s | %-22s | %-5s | %-7s | %-24s | %-10s |%n",
                "RANK", "NAME", "SKILLS", "EXP", "MATCH", "MISSING REQUIRED", "STATUS");
        System.out.println("=========================================================================================================");
        if (ranked.isEmpty()) {
            System.out.println("No applicants for this job yet.");
        }
        int rank = 1;
        for (RankedApplicant a : ranked) {
            System.out.printf("| %-4d | %-18s | %-22s | %-5.1f | %5d %% | %-24s | %-10s |%n",
                    rank++, cut(a.name(), 18), cut(a.skills(), 22), a.experience(), a.matchPercent(),
                    cut(a.missingRequired().isEmpty() ? "-" : String.join(", ", a.missingRequired()), 24), a.status());
        }
        System.out.println("=========================================================================================================");
        return ranked;
    }

    /** Moves the top n ranked applicants that are still Applied to Interview. Returns how many were moved. */
    public int shortlistTop(int jobId, String company, int n) {
        Job job = jobRepo.findById(jobId);
        if (job == null || !ownsJob(job, company)) {
            System.out.println("Shortlisting is only allowed for existing jobs of your company.");
            return 0;
        }
        if (n <= 0) {
            System.out.println("N must be at least 1.");
            return 0;
        }
        List<RankedApplicant> toMove = RankedApplicant.topToShortlist(getRankedApplicants(jobId), n);
        int moved = 0;
        for (RankedApplicant a : toMove) {
            if (applicationRepo.updateStatus(a.applicationId(), "Interview")) {
                moved++;
                FileManager.writeLog("Shortlisted " + a.name() + " (" + a.matchPercent() + "%) for Job ID " + jobId
                        + ": status Applied -> Interview (App ID " + a.applicationId() + ")");
            }
        }
        System.out.println(moved + " applicant(s) moved from Applied to Interview.");
        return moved;
    }

    /** Writes the ranked list to data/shortlist_job_{id}.csv and returns the file path (null on failure). */
    public Path exportShortlist(int jobId, String company) {
        Job job = jobRepo.findById(jobId);
        if (job == null || !ownsJob(job, company)) {
            System.out.println("Export is only allowed for existing jobs of your company.");
            return null;
        }
        Path file = Paths.get("data", "shortlist_job_" + jobId + ".csv");
        try {
            RankedApplicant.writeCsv(file, getRankedApplicants(jobId));
            System.out.println("Ranked shortlist exported to " + file);
            FileManager.writeLog("Exported ranked shortlist of Job ID " + jobId + " to " + file);
            return file;
        } catch (IOException e) {
            System.out.println("Failed to export shortlist: " + e.getMessage());
            return null;
        }
    }

    // ---------------------------------------------------------------- recruiter preview

    /** Every registered candidate scored against one job, best match first. Creates no applications. */
    public List<RankedApplicant> scoreAllCandidates(int jobId) {
        Job job = jobRepo.findById(jobId);
        if (job == null) return new ArrayList<>();
        Map<Integer, String> applied = new java.util.HashMap<>();
        for (Application a : applicationRepo.getAllApplications()) {
            if (a.getJobId() == jobId) applied.put(a.getCandidateId(), a.getStatus());
        }
        List<RankedApplicant> list = new ArrayList<>();
        for (Candidate c : candidateRepo.getAllCandidates()) {
            MatchScorer.Result r = scoreFor(c, job);
            list.add(new RankedApplicant(0, c.getId(), c.getName(), c.getSkill(), c.getExperience(),
                    r.percent(), applied.getOrDefault(c.getId(), "Not applied"), Long.MAX_VALUE, r.missingRequired()));
        }
        return RankedApplicant.rank(list);
    }

    /** Recruiter-only view: how well each candidate matches a job of the recruiter's company. */
    public List<RankedApplicant> previewMatches(int jobId, String company) {
        Job job = jobRepo.findById(jobId);
        if (job == null) {
            System.out.println("Job ID " + jobId + " does not exist.");
            return null;
        }
        if (!ownsJob(job, company)) {
            System.out.println("Access denied: Job ID " + jobId + " belongs to another company.");
            return null;
        }
        JobRule rule = ruleRepo.findRule(jobId);
        List<RankedApplicant> scored = scoreAllCandidates(jobId);
        System.out.println("\nCANDIDATE MATCH PREVIEW - " + job.getTitle() + " (Job ID " + jobId + ")");
        System.out.println("Required: " + job.getRequiredSkill() + " | Preferred: "
                + (rule.preferredSkills().isEmpty() ? "none" : rule.preferredSkills())
                + " | Min experience: " + rule.minExperience() + " yrs");
        System.out.println("=========================================================================================================");
        System.out.printf("| %-4s | %-18s | %-22s | %-5s | %-7s | %-24s | %-11s |%n",
                "RANK", "NAME", "SKILLS", "EXP", "MATCH", "MISSING REQUIRED", "APPLICATION");
        System.out.println("=========================================================================================================");
        if (scored.isEmpty()) {
            System.out.println("No candidates are registered yet.");
        }
        int rank = 1;
        for (RankedApplicant a : scored) {
            System.out.printf("| %-4d | %-18s | %-22s | %-5.1f | %5d %% | %-24s | %-11s |%n",
                    rank++, cut(a.name(), 18), cut(a.skills(), 22), a.experience(), a.matchPercent(),
                    cut(a.missingRequired().isEmpty() ? "-" : String.join(", ", a.missingRequired()), 24), a.status());
        }
        System.out.println("=========================================================================================================");
        System.out.println("Preview only: no applications were created. Use option 16 to apply candidates above a cut-off.");
        return scored;
    }

    // ---------------------------------------------------------------- helpers

    private List<Job> jobsOfCompany(String company) {
        List<Job> out = new ArrayList<>();
        for (Job j : jobRepo.getAllJobs()) {
            if (ownsJob(j, company)) out.add(j);
        }
        return out;
    }

    private static boolean ownsJob(Job job, String company) {
        return company != null && job.getCompany() != null && job.getCompany().trim().equalsIgnoreCase(company.trim());
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}
