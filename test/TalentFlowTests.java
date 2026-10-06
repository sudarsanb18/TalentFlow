import model.Candidate;
import model.Job;
import repository.CandidateRepository;
import repository.JobRepository;
import service.ApplicationService;
import service.CandidateService;
import service.JobService;
import service.MatchScorer;
import service.RankedApplicant;
import service.SmartMatchService;
import util.Validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiny custom test runner (no JUnit, Core Java only).
 *
 *   java -cp bin;lib/mysql-connector-j-26.7.0.jar TalentFlowTests          -> pure-logic tests, no database needed
 *   java -cp bin;lib/mysql-connector-j-26.7.0.jar TalentFlowTests --db     -> also runs database tests
 *
 * The --db tests write to MySQL using a throwaway company "ZZ_TEST_CO" and delete their own rows afterwards.
 * Do not run them against data you care about without a backup.
 */
public class TalentFlowTests {
    static int pass = 0, fail = 0;
    static final List<String> failures = new ArrayList<>();
    static String currentCategory = "";

    /** Like BooleanSupplier, but a check may throw (for example file I/O). */
    interface Check {
        boolean run() throws Exception;
    }

    static void t(String category, String name, Check check) {
        if (!category.equals(currentCategory)) {
            System.out.println("\n== " + category + " ==");
            currentCategory = category;
        }
        boolean ok;
        try {
            ok = check.run();
        } catch (Throwable e) {
            ok = false;
            System.out.println("     exception: " + e);
        }
        if (ok) pass++;
        else {
            fail++;
            failures.add(category + " / " + name);
        }
        System.out.printf("%-4s %s%n", ok ? "PASS" : "FAIL", name);
    }

    static int pct(String cand, double exp, String req, String pref, double min) {
        return MatchScorer.score(cand, exp, req, pref, min).percent();
    }

    static RankedApplicant ra(int appId, String name, double exp, int percent, String status, long at) {
        return new RankedApplicant(appId, appId, name, "Java", exp, percent, status, at, List.of());
    }

    public static void main(String[] args) throws Exception {
        boolean withDb = args.length > 0 && args[0].equals("--db");

        // ------------------------------------------------------------ Validation (existing behaviour)
        t("Validation", "valid email accepted", () -> Validation.isValidEmail("alice.smith@gmail.com"));
        t("Validation", "email without @ rejected", () -> !Validation.isValidEmail("alicegmail.com"));
        t("Validation", "email with unknown domain rejected", () -> !Validation.isValidEmail("a@b.xyz"));
        t("Validation", "null email rejected", () -> !Validation.isValidEmail(null));
        t("Validation", "valid phone accepted", () -> Validation.isValidPhone("9123456780"));
        t("Validation", "phone starting with 5 rejected", () -> !Validation.isValidPhone("5123456780"));
        t("Validation", "9-digit phone rejected", () -> !Validation.isValidPhone("912345678"));
        t("Validation", "strong password accepted", () -> Validation.isValidPassword("Pass@1234"));
        t("Validation", "password without special char rejected", () -> !Validation.isValidPassword("Pass12345"));
        t("Validation", "short password rejected", () -> !Validation.isValidPassword("Pa@1"));
        t("Validation", "valid name accepted", () -> Validation.isValidName("Alice Smith"));
        t("Validation", "name with digits rejected", () -> !Validation.isValidName("Alice99"));
        t("Validation", "experience 3.5 accepted", () -> Validation.isPositiveExperience(3.5));
        t("Validation", "negative experience rejected", () -> !Validation.isPositiveExperience(-1));
        t("Validation", "blank skill rejected", () -> !Validation.isValidSkill("   "));

        // ------------------------------------------------------------ MatchScorer
        t("MatchScorer", "all required matched, no extras = 100", () -> pct("Java, SQL", 3, "Java, SQL", "", 0) == 100);
        t("MatchScorer", "half of required matched = 50", () -> pct("Java", 3, "Java, SQL", "", 0) == 50);
        t("MatchScorer", "none matched = 0", () -> pct("Python", 3, "Java, SQL", "", 0) == 0);
        t("MatchScorer", "matching is case-insensitive", () -> pct("java, sql", 3, "JAVA, Sql", "", 0) == 100);
        t("MatchScorer", "Java does not match JavaScript", () -> pct("JavaScript", 3, "Java", "", 0) == 0);
        t("MatchScorer", "missing preferred skills cost at most 30 points", () -> pct("Java", 3, "Java", "Docker, AWS", 0) == 70);
        t("MatchScorer", "preferred skills all matched = 100", () -> pct("Java, Docker", 3, "Java", "Docker", 0) == 100);
        t("MatchScorer", "half preferred matched = 85", () -> pct("Java, Docker", 3, "Java", "Docker, AWS", 0) == 85);
        t("MatchScorer", "experience below minimum scales the score", () -> pct("Java", 2, "Java", "", 4) == 50);
        t("MatchScorer", "experience above minimum does not boost", () -> pct("Java", 10, "Java", "", 4) == 100);
        t("MatchScorer", "empty candidate skills = 0", () -> pct("", 5, "Java", "", 0) == 0);
        t("MatchScorer", "null candidate skills = 0", () -> pct(null, 5, "Java", "", 0) == 0);
        t("MatchScorer", "whitespace and trailing commas handled", () -> pct("  Java ,SQL, ", 1, "Java,  SQL ,", "", 0) == 100);
        t("MatchScorer", "duplicate skills do not inflate the score", () -> pct("Java, java", 1, "Java, SQL", "", 0) == 50);
        t("MatchScorer", "result is always within 0..100", () -> {
            int p = pct("Java", 99, "Java", "Java", 0.1);
            return p >= 0 && p <= 100;
        });
        t("MatchScorer", "missing required skills are reported", () ->
                MatchScorer.score("Java", 1, "Java, SQL, Git", "", 0).missingRequired().equals(List.of("SQL", "Git")));
        t("MatchScorer", "ampersand separates skills (React & Java)", () -> pct("React & Java", 5, "Java", "", 0) == 100);
        t("MatchScorer", "slash separates skills (Java/SQL)", () -> pct("Java/SQL", 1, "SQL, Java", "", 0) == 100);
        t("MatchScorer", "parseSkills drops empty tokens", () -> MatchScorer.parseSkills(" a, ,b,, ").size() == 2);

        // ------------------------------------------------------------ Ranking
        List<RankedApplicant> sample = List.of(
                ra(1, "Low", 9, 40, "Applied", 100),
                ra(2, "High", 1, 90, "Applied", 300),
                ra(3, "MidSeniorLate", 8, 70, "Applied", 500),
                ra(4, "MidSeniorEarly", 8, 70, "Applied", 200),
                ra(5, "MidJunior", 2, 70, "Applied", 100));
        List<RankedApplicant> ranked = RankedApplicant.rank(sample);
        t("Ranking", "highest match percentage ranks first", () -> ranked.get(0).name().equals("High"));
        t("Ranking", "lowest match percentage ranks last", () -> ranked.get(4).name().equals("Low"));
        t("Ranking", "tie on match: more experience first", () -> ranked.get(1).experience() == 8 && ranked.get(3).name().equals("MidJunior"));
        t("Ranking", "tie on match and experience: earlier application first", () ->
                ranked.get(1).name().equals("MidSeniorEarly") && ranked.get(2).name().equals("MidSeniorLate"));
        t("Ranking", "cut-off excludes low scorers", () -> RankedApplicant.aboveCutoff(sample, 60).size() == 4);
        t("Ranking", "cut-off of 0 keeps everyone, 100 keeps none here", () ->
                RankedApplicant.aboveCutoff(sample, 0).size() == 5 && RankedApplicant.aboveCutoff(sample, 100).isEmpty());
        t("Ranking", "ranking does not modify the input list", () -> sample.get(0).name().equals("Low"));

        // ------------------------------------------------------------ Shortlist + CSV
        t("Shortlist", "top 2 selected from ranked list", () -> RankedApplicant.topToShortlist(ranked, 2).size() == 2
                && RankedApplicant.topToShortlist(ranked, 2).get(0).name().equals("High"));
        t("Shortlist", "applicants not in Applied stage are skipped", () -> {
            List<RankedApplicant> mixed = RankedApplicant.rank(List.of(
                    ra(1, "A", 1, 90, "Rejected", 1), ra(2, "B", 1, 80, "Applied", 2)));
            return RankedApplicant.topToShortlist(mixed, 2).size() == 1;
        });
        t("Shortlist", "N larger than the list is safe", () -> RankedApplicant.topToShortlist(ranked, 99).size() == 5);
        t("Shortlist", "N of zero or negative selects nobody", () ->
                RankedApplicant.topToShortlist(ranked, 0).isEmpty() && RankedApplicant.topToShortlist(ranked, -3).isEmpty());
        t("Shortlist", "CSV has a header and one row per applicant", () -> {
            Path f = Files.createTempFile("shortlist", ".csv");
            RankedApplicant.writeCsv(f, ranked);
            List<String> lines = Files.readAllLines(f);
            Files.deleteIfExists(f);
            return lines.size() == 6 && lines.get(0).startsWith("Rank,Application ID") && lines.get(1).startsWith("1,2,");
        });
        t("Shortlist", "CSV escapes commas in skills", () -> {
            Path f = Files.createTempFile("shortlist", ".csv");
            RankedApplicant a = new RankedApplicant(1, 1, "Zed", "Java, SQL", 2, 80, "Applied", 1, List.of("Git"));
            RankedApplicant.writeCsv(f, List.of(a));
            String row = Files.readAllLines(f).get(1);
            Files.deleteIfExists(f);
            return row.contains("\"Java, SQL\"");
        });

        // ------------------------------------------------------------ Database tests (opt-in)
        if (withDb) runDbTests();
        else System.out.println("\n(database tests skipped; run with --db to include them)");

        System.out.println("\n====================================");
        System.out.println("TOTAL: " + (pass + fail) + "   PASSED: " + pass + "   FAILED: " + fail);
        for (String f : failures) System.out.println("FAILED: " + f);
        System.out.println("====================================");
        System.exit(fail == 0 ? 0 : 1);
    }

    static void runDbTests() {
        final String CO = "ZZ_TEST_CO";
        CandidateService cs = new CandidateService();
        JobService js = new JobService();
        ApplicationService as = new ApplicationService();
        SmartMatchService sm = new SmartMatchService();
        CandidateRepository cRepo = new CandidateRepository();
        JobRepository jRepo = new JobRepository();

        Candidate strong = cs.addCandidate("Zed Strong", "zz.strong@testco.com", "9000000001", "Test@1234", "Java, SQL, Docker", 5.0, "test");
        Candidate medium = cs.addCandidate("Zed Medium", "zz.medium@testco.com", "9000000002", "Test@1234", "Java", 5.0, "test");
        Candidate weak = cs.addCandidate("Zed Weak", "zz.weak@testco.com", "9000000003", "Test@1234", "Python", 1.0, "test");
        Job job = js.addJob("ZZ Test Backend Engineer", CO, "Chennai", "10 LPA", "Java, SQL");

        try {
            t("Database", "test candidates and job were created", () -> strong != null && medium != null && weak != null && job != null);
            if (job == null || strong == null || medium == null || weak == null) return;

            t("Database", "match rules saved for the job", () -> sm.setJobRule(job.getJobId(), CO, "Docker", 2.0));
            t("Database", "match rules refused for another company", () -> !sm.setJobRule(job.getJobId(), "Other Co", "Docker", 2.0));
            t("Database", "negative minimum experience refused", () -> !sm.setJobRule(job.getJobId(), CO, "Docker", -1));

            t("Database", "preview lists every candidate best match first without applying anyone", () -> {
                int before = sm.getRankedApplicants(job.getJobId()).size();
                List<RankedApplicant> p = sm.previewMatches(job.getJobId(), CO);
                boolean ordered = p != null && p.size() >= 3 && p.get(0).candidateId() == strong.getId()
                        && p.get(1).candidateId() == medium.getId() && p.get(2).candidateId() == weak.getId();
                return ordered && sm.getRankedApplicants(job.getJobId()).size() == before;
            });
            t("Database", "preview is refused for another company", () -> sm.previewMatches(job.getJobId(), "Other Co") == null);
            t("Database", "weighted matching applies only candidates at or above cut-off", () -> {
                // strong = 100%, medium = 35% (1 of 2 required, preferred missing), weak = 0%
                sm.runWeightedMatching(CO, 30);
                List<RankedApplicant> r = sm.getRankedApplicants(job.getJobId());
                boolean hasStrong = r.stream().anyMatch(a -> a.candidateId() == strong.getId());
                boolean hasMedium = r.stream().anyMatch(a -> a.candidateId() == medium.getId());
                boolean hasWeak = r.stream().anyMatch(a -> a.candidateId() == weak.getId());
                return hasStrong && hasMedium && !hasWeak;
            });
            t("Database", "running matching again creates no duplicate applications", () -> {
                int before = sm.getRankedApplicants(job.getJobId()).size();
                sm.runWeightedMatching(CO, 30);
                return sm.getRankedApplicants(job.getJobId()).size() == before;
            });
            t("Database", "manual duplicate application is rejected by the UNIQUE rule", () -> as.applyJob(strong.getId(), job.getJobId()) == null);
            t("Database", "strong candidate ranks above medium candidate", () -> {
                List<RankedApplicant> r = sm.getRankedApplicants(job.getJobId());
                return r.size() >= 2 && r.get(0).candidateId() == strong.getId() && r.get(1).candidateId() == medium.getId();
            });
            t("Database", "shortlist top 1 moves only the first applicant to Interview", () -> {
                int moved = sm.shortlistTop(job.getJobId(), CO, 1);
                List<RankedApplicant> r = sm.getRankedApplicants(job.getJobId());
                return moved == 1 && r.get(0).status().equals("Interview") && r.get(1).status().equals("Applied");
            });
            t("Database", "withdrawn applicant is left out of the ranked shortlist", () -> {
                List<RankedApplicant> r = sm.getRankedApplicants(job.getJobId());
                int medApp = r.stream().filter(a -> a.candidateId() == medium.getId()).findFirst().get().applicationId();
                boolean withdrawn = as.withdrawApplication(medApp, medium.getId());
                boolean gone = sm.getRankedApplicants(job.getJobId()).stream().noneMatch(a -> a.candidateId() == medium.getId());
                return withdrawn && gone;
            });
            t("Database", "recruiter cannot change a withdrawn application", () -> {
                int medApp = as.getAllApplications().stream().filter(a -> a.getCandidateId() == medium.getId()
                        && a.getJobId() == job.getJobId()).findFirst().get().getApplicationId();
                return !as.updateApplicationStatus(medApp, "Interview");
            });
            t("Database", "recruiter can still move an active application to Selected", () -> {
                int strongApp = as.getAllApplications().stream().filter(a -> a.getCandidateId() == strong.getId()
                        && a.getJobId() == job.getJobId()).findFirst().get().getApplicationId();
                return as.updateApplicationStatus(strongApp, "Selected");
            });
            t("Database", "shortlist is refused for another company", () -> sm.shortlistTop(job.getJobId(), "Other Co", 1) == 0);
            t("Database", "CSV export writes a header and one row per applicant", () -> {
                Path f = sm.exportShortlist(job.getJobId(), CO);
                try {
                    List<String> lines = Files.readAllLines(f);
                    return lines.size() == 1 + sm.getRankedApplicants(job.getJobId()).size();
                } catch (Exception e) {
                    return false;
                } finally {
                    try {
                        Files.deleteIfExists(f);
                    } catch (Exception ignored) {
                    }
                }
            });
        } finally {
            // clean-up: deleting the job cascades to its applications and match rules
            if (job != null) jRepo.deleteJob(job.getJobId());
            for (Candidate c : new Candidate[]{strong, medium, weak}) {
                if (c != null) cRepo.deleteCandidate(c.getId());
            }
        }
    }
}
