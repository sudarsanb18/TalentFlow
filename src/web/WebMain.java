package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import model.Application;
import model.Candidate;
import model.Job;
import model.Recruiter;
import repository.JobMatchRepository.JobRule;
import service.ApplicationService;
import service.CandidateService;
import service.JobService;
import service.RankedApplicant;
import service.RecruiterService;
import service.SmartMatchService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static web.Html.esc;

/**
 * Browser UI for TalentFlow on http://localhost:8080.
 * Uses only the JDK's built-in HTTP server and the existing services, repositories and JDBC/MySQL database,
 * so the console application (menu.Main) and this UI work on the same data.
 *
 * Run: java -cp "bin;lib/mysql-connector-j-26.7.0.jar" web.WebMain [port]
 */
public class WebMain {

    private static final Set<String> RECRUITER_STATUSES = Set.of("Interview", "Selected", "Rejected");

    private final RecruiterService recruiterService = new RecruiterService();
    private final CandidateService candidateService = new CandidateService();
    private final JobService jobService = new JobService();
    private final ApplicationService applicationService = new ApplicationService();
    private final SmartMatchService smartMatchService = new SmartMatchService();

    /** Logged-in user per browser (cookie value -> session). */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private static final class Session {
        String role;        // "recruiter" or "candidate"
        int userId;
        String flash;
        boolean flashOk;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Html.toInt(args[0], 8080) : 8080;
        WebMain app = new WebMain();
        // Bound to 127.0.0.1 only: the UI is reachable from this computer, not from the network.
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", app::handle);
        server.setExecutor(null); // one request at a time (keeps service output capture safe)
        server.start();
        System.out.println("TalentFlow web UI running at http://localhost:" + port + "  (press Ctrl+C to stop)");
    }

    // ------------------------------------------------------------------ routing

    private void handle(HttpExchange ex) {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            Session s = currentSession(ex);
            Map<String, String> query = Html.parseForm(ex.getRequestURI().getRawQuery());
            Map<String, String> form = "POST".equals(method) ? Html.parseForm(Html.readBody(ex.getRequestBody())) : Map.of();

            switch (path) {
                case "/" -> {
                    if (s != null) redirect(ex, "/" + s.role);
                    else loginPage(ex, null);
                }
                case "/login" -> login(ex, form);
                case "/logout" -> logout(ex);
                case "/recruiter" -> { if (isRole(ex, s, "recruiter")) recruiterHome(ex, s); }
                case "/recruiter/job" -> { if (isRole(ex, s, "recruiter")) recruiterJob(ex, s, Html.toInt(query.get("id"), -1)); }
                case "/recruiter/jobs/add" -> { if (isRole(ex, s, "recruiter") && post(ex, method)) addJob(ex, s, form); }
                case "/recruiter/rules" -> { if (isRole(ex, s, "recruiter") && post(ex, method)) saveRules(ex, s, form); }
                case "/recruiter/match" -> { if (isRole(ex, s, "recruiter") && post(ex, method)) runMatching(ex, s, form); }
                case "/recruiter/shortlist" -> { if (isRole(ex, s, "recruiter") && post(ex, method)) shortlist(ex, s, form); }
                case "/recruiter/status" -> { if (isRole(ex, s, "recruiter") && post(ex, method)) updateStatus(ex, s, form); }
                case "/recruiter/export" -> { if (isRole(ex, s, "recruiter")) exportCsv(ex, s, Html.toInt(query.get("id"), -1)); }
                case "/candidate" -> { if (isRole(ex, s, "candidate")) candidateHome(ex, s); }
                case "/candidate/apply" -> { if (isRole(ex, s, "candidate") && post(ex, method)) apply(ex, s, form); }
                case "/candidate/withdraw" -> { if (isRole(ex, s, "candidate") && post(ex, method)) withdraw(ex, s, form); }
                default -> send(ex, 404, Html.page("Not found", null, "Page not found.", false,
                        "<p><a class=\"btn\" href=\"/\">Go to start</a></p>"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                send(ex, 500, Html.page("Error", null, "Something went wrong: " + e.getMessage(), false,
                        "<p><a class=\"btn\" href=\"/\">Go to start</a></p>"));
            } catch (IOException ignored) {
            }
        } finally {
            ex.close();
        }
    }

    // ------------------------------------------------------------------ login

    private void loginPage(HttpExchange ex, String error) throws IOException {
        String body = """
                <div class="card login">
                  <h1>Sign in</h1>
                  <form method="post" action="/login">
                    <label>I am a</label>
                    <select name="role"><option value="recruiter">Recruiter</option><option value="candidate">Candidate</option></select>
                    <label>User ID</label><input name="id" inputmode="numeric" required>
                    <label>Password</label><input name="password" type="password" required>
                    <p><button type="submit">Sign in</button></p>
                  </form>
                  <p class="muted">New candidates register and admins work in the console app (menu.Main). Both use the same database.</p>
                </div>""";
        send(ex, 200, Html.page("Sign in", null, error, false, body));
    }

    private void login(HttpExchange ex, Map<String, String> form) throws IOException {
        String role = form.getOrDefault("role", "");
        int id = Html.toInt(form.get("id"), -1);
        String password = form.getOrDefault("password", "");
        boolean ok = switch (role) {
            case "recruiter" -> recruiterService.authenticateRecruiter(id, password) != null;
            case "candidate" -> candidateService.authenticateCandidate(id, password) != null;
            default -> false;
        };
        if (!ok) {
            loginPage(ex, "Invalid ID or password.");
            return;
        }
        Session s = new Session();
        s.role = role;
        s.userId = id;
        String token = UUID.randomUUID().toString();
        sessions.put(token, s);
        ex.getResponseHeaders().add("Set-Cookie", "TF_SESSION=" + token + "; Path=/; HttpOnly; SameSite=Strict");
        redirect(ex, "/" + role);
    }

    private void logout(HttpExchange ex) throws IOException {
        String token = cookie(ex);
        if (token != null) sessions.remove(token);
        ex.getResponseHeaders().add("Set-Cookie", "TF_SESSION=; Path=/; Max-Age=0");
        redirect(ex, "/");
    }

    // ------------------------------------------------------------------ recruiter pages

    private void recruiterHome(HttpExchange ex, Session s) throws IOException {
        Recruiter r = recruiterService.getRecruiterById(s.userId);
        if (r == null) { logout(ex); return; }
        Map<Integer, Integer> activeCount = new HashMap<>();
        for (Application a : applicationService.getAllApplications()) {
            if (!"Withdrawn".equalsIgnoreCase(a.getStatus())) activeCount.merge(a.getJobId(), 1, Integer::sum);
        }
        StringBuilder rows = new StringBuilder();
        int mine = 0;
        for (Job j : jobService.getAllJobs()) {
            if (!jobService.isJobOwnedBy(j.getJobId(), r.getCompany())) continue;
            mine++;
            rows.append("<tr><td>").append(j.getJobId()).append("</td><td>").append(esc(j.getTitle()))
                    .append("</td><td>").append(esc(j.getLocation())).append("</td><td>").append(esc(j.getSalaryRange()))
                    .append("</td><td>").append(esc(j.getRequiredSkill())).append("</td><td>")
                    .append(activeCount.getOrDefault(j.getJobId(), 0))
                    .append("</td><td><a class=\"btn\" href=\"/recruiter/job?id=").append(j.getJobId()).append("\">Open</a></td></tr>");
        }
        String jobsTable = mine == 0 ? "<p class=\"muted\">Your company has not posted any jobs yet.</p>"
                : "<table><tr><th>ID</th><th>Title</th><th>Location</th><th>Salary</th><th>Required skills</th><th>Active applicants</th><th></th></tr>"
                + rows + "</table>";

        String body = "<h1>Recruiter dashboard – " + esc(r.getCompany()) + "</h1>"
                + "<div class=\"card\"><h2>Your company's jobs</h2>" + jobsTable + "</div>"
                + "<div class=\"grid\">"
                + "<div class=\"card\"><h2>Post a new job</h2><form method=\"post\" action=\"/recruiter/jobs/add\">"
                + "<label>Job title</label><input name=\"title\" required style=\"width:100%\">"
                + "<label>Location</label><input name=\"location\" required style=\"width:100%\">"
                + "<label>Salary range</label><input name=\"salary\" placeholder=\"e.g. 10 - 15 LPA\" style=\"width:100%\">"
                + "<label>Required skills (comma separated)</label><input name=\"skills\" required placeholder=\"Java, SQL\" style=\"width:100%\">"
                + "<p><button type=\"submit\">Post job</button></p></form></div>"
                + "<div class=\"card\"><h2>Weighted matching engine</h2>"
                + "<p class=\"muted\">Scores every candidate against your company's jobs and applies those at or above the cut-off.</p>"
                + "<form method=\"post\" action=\"/recruiter/match\" class=\"row\"><div><label>Cut-off match %</label>"
                + "<input name=\"cutoff\" type=\"number\" min=\"0\" max=\"100\" value=\"" + SmartMatchService.DEFAULT_CUTOFF + "\"></div>"
                + "<button type=\"submit\">Run matching</button></form></div>"
                + "</div>";
        sendPage(ex, s, "Recruiter", r.getName() + " · " + r.getCompany(), body);
    }

    private void recruiterJob(HttpExchange ex, Session s, int jobId) throws IOException {
        Recruiter r = recruiterService.getRecruiterById(s.userId);
        if (r == null) { logout(ex); return; }
        if (!jobService.isJobOwnedBy(jobId, r.getCompany())) {
            flash(s, "Job ID " + jobId + " does not exist or belongs to another company.", false);
            redirect(ex, "/recruiter");
            return;
        }
        Job job = null;
        for (Job j : jobService.getAllJobs()) if (j.getJobId() == jobId) job = j;
        JobRule rule = smartMatchService.getJobRule(jobId);
        List<RankedApplicant> preview = smartMatchService.scoreAllCandidates(jobId);
        List<RankedApplicant> ranked = smartMatchService.getRankedApplicants(jobId);

        StringBuilder pv = new StringBuilder();
        int rank = 1;
        for (RankedApplicant a : preview) {
            pv.append("<tr><td>").append(rank++).append("</td><td>").append(esc(a.name())).append(" <span class=\"muted\">#")
                    .append(a.candidateId()).append("</span></td><td>").append(esc(a.skills())).append("</td><td>")
                    .append(a.experience()).append("</td><td>").append(pctCell(a.matchPercent())).append("</td><td>")
                    .append(esc(missing(a))).append("</td><td><span class=\"tag\">").append(esc(a.status())).append("</span></td></tr>");
        }

        StringBuilder rk = new StringBuilder();
        rank = 1;
        for (RankedApplicant a : ranked) {
            String statusForm = "<form method=\"post\" action=\"/recruiter/status\" class=\"inline\">"
                    + "<input type=\"hidden\" name=\"jobId\" value=\"" + jobId + "\">"
                    + "<input type=\"hidden\" name=\"appId\" value=\"" + a.applicationId() + "\">"
                    + "<select name=\"status\"><option>Interview</option><option>Selected</option><option>Rejected</option></select>"
                    + "<button type=\"submit\" class=\"secondary\">Set</button></form>";
            rk.append("<tr><td>").append(rank++).append("</td><td>").append(esc(a.name())).append("</td><td>")
                    .append(a.experience()).append("</td><td>").append(pctCell(a.matchPercent())).append("</td><td>")
                    .append(esc(missing(a))).append("</td><td><span class=\"tag\">").append(esc(a.status()))
                    .append("</span></td><td>").append(statusForm).append("</td></tr>");
        }

        String body = "<p><a href=\"/recruiter\">&larr; Back to dashboard</a></p>"
                + "<h1>" + esc(job.getTitle()) + " <span class=\"muted\">(Job ID " + jobId + ")</span></h1>"
                + "<div class=\"card\"><h2>Match rules</h2><p class=\"muted\">Required skills: <b>" + esc(job.getRequiredSkill())
                + "</b> (70% of the score). Preferred skills add the other 30%; candidates below the minimum experience are scaled down.</p>"
                + "<form method=\"post\" action=\"/recruiter/rules\" class=\"row\"><input type=\"hidden\" name=\"jobId\" value=\"" + jobId + "\">"
                + "<div><label>Preferred skills (comma separated)</label><input name=\"preferred\" value=\"" + esc(rule.preferredSkills()) + "\" style=\"width:280px\"></div>"
                + "<div><label>Minimum experience (years)</label><input name=\"minExp\" type=\"number\" step=\"0.5\" min=\"0\" value=\"" + rule.minExperience() + "\"></div>"
                + "<button type=\"submit\">Save rules</button></form></div>"

                + "<div class=\"card\"><h2>Ranked shortlist – applicants</h2>"
                + (ranked.isEmpty() ? "<p class=\"muted\">No active applicants yet. Run the matching engine or wait for candidates to apply.</p>"
                : "<table><tr><th>Rank</th><th>Name</th><th>Exp (yrs)</th><th>Match</th><th>Missing required</th><th>Status</th><th>Update status</th></tr>" + rk + "</table>"
                + "<div class=\"row\" style=\"margin-top:12px\"><form method=\"post\" action=\"/recruiter/shortlist\" class=\"inline\">"
                + "<input type=\"hidden\" name=\"jobId\" value=\"" + jobId + "\"><label style=\"margin:0\">Shortlist top</label>"
                + "<input name=\"n\" type=\"number\" min=\"1\" value=\"1\" style=\"width:70px\"><button type=\"submit\">Move to Interview</button></form>"
                + "<a class=\"btn secondary\" href=\"/recruiter/export?id=" + jobId + "\">Download CSV</a></div>"
                + "<p class=\"muted\">Order: match % high to low, then more experience, then earlier application. Only applicants still in 'Applied' are moved.</p>")
                + "</div>"

                + "<div class=\"card\"><h2>Candidate match preview</h2><p class=\"muted\">Every registered candidate scored for this job. Nothing is applied from this view.</p>"
                + (preview.isEmpty() ? "<p class=\"muted\">No candidates registered yet.</p>"
                : "<table><tr><th>Rank</th><th>Candidate</th><th>Skills</th><th>Exp</th><th>Match</th><th>Missing required</th><th>Application</th></tr>" + pv + "</table>")
                + "</div>";
        sendPage(ex, s, "Job " + jobId, r.getName() + " · " + r.getCompany(), body);
    }

    private void addJob(HttpExchange ex, Session s, Map<String, String> f) throws IOException {
        Recruiter r = recruiterService.getRecruiterById(s.userId);
        Job[] created = new Job[1];
        String out = Html.capture(() -> created[0] = jobService.addJob(f.get("title"), r.getCompany(), f.get("location"),
                f.get("salary"), f.get("skills")));
        flash(s, Html.lastLine(out), created[0] != null);
        redirect(ex, "/recruiter");
    }

    private void saveRules(HttpExchange ex, Session s, Map<String, String> f) throws IOException {
        Recruiter r = recruiterService.getRecruiterById(s.userId);
        int jobId = Html.toInt(f.get("jobId"), -1);
        boolean[] ok = new boolean[1];
        String out = Html.capture(() -> ok[0] = smartMatchService.setJobRule(jobId, r.getCompany(), f.get("preferred"),
                Html.toDouble(f.get("minExp"), -1)));
        flash(s, Html.lastLine(out), ok[0]);
        redirect(ex, "/recruiter/job?id=" + jobId);
    }

    private void runMatching(HttpExchange ex, Session s, Map<String, String> f) throws IOException {
        Recruiter r = recruiterService.getRecruiterById(s.userId);
        int cutoff = Html.toInt(f.get("cutoff"), SmartMatchService.DEFAULT_CUTOFF);
        int[] created = new int[]{-1};
        String out = Html.capture(() -> created[0] = smartMatchService.runWeightedMatching(r.getCompany(), cutoff));
        flash(s, Html.lastLine(out) + (created[0] > 0 ? " Open a job to see the ranked shortlist." : ""), created[0] >= 0 && cutoff >= 0 && cutoff <= 100);
        redirect(ex, "/recruiter");
    }

    private void shortlist(HttpExchange ex, Session s, Map<String, String> f) throws IOException {
        Recruiter r = recruiterService.getRecruiterById(s.userId);
        int jobId = Html.toInt(f.get("jobId"), -1);
        int[] moved = new int[1];
        String out = Html.capture(() -> moved[0] = smartMatchService.shortlistTop(jobId, r.getCompany(), Html.toInt(f.get("n"), 0)));
        flash(s, Html.lastLine(out), moved[0] > 0);
        redirect(ex, "/recruiter/job?id=" + jobId);
    }

    private void updateStatus(HttpExchange ex, Session s, Map<String, String> f) throws IOException {
        Recruiter r = recruiterService.getRecruiterById(s.userId);
        int jobId = Html.toInt(f.get("jobId"), -1);
        int appId = Html.toInt(f.get("appId"), -1);
        String status = f.getOrDefault("status", "");
        boolean belongs = false;
        for (Application a : applicationService.getAllApplications()) {
            if (a.getApplicationId() == appId && a.getJobId() == jobId) belongs = true;
        }
        if (!RECRUITER_STATUSES.contains(status) || !belongs || !jobService.isJobOwnedBy(jobId, r.getCompany())) {
            flash(s, "That status change is not allowed.", false);
        } else {
            boolean[] ok = new boolean[1];
            String out = Html.capture(() -> ok[0] = applicationService.updateApplicationStatus(appId, status));
            flash(s, Html.lastLine(out), ok[0]);
        }
        redirect(ex, "/recruiter/job?id=" + jobId);
    }

    private void exportCsv(HttpExchange ex, Session s, int jobId) throws IOException {
        Recruiter r = recruiterService.getRecruiterById(s.userId);
        Path[] file = new Path[1];
        String out = Html.capture(() -> file[0] = smartMatchService.exportShortlist(jobId, r.getCompany()));
        if (file[0] == null) {
            flash(s, Html.lastLine(out), false);
            redirect(ex, "/recruiter");
            return;
        }
        byte[] bytes = Files.readAllBytes(file[0]);
        ex.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"shortlist_job_" + jobId + ".csv\"");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ------------------------------------------------------------------ candidate pages

    private void candidateHome(HttpExchange ex, Session s) throws IOException {
        Candidate c = candidateService.getCandidateById(s.userId);
        if (c == null) { logout(ex); return; }
        Map<Integer, Job> jobs = new HashMap<>();
        for (Job j : jobService.getAllJobs()) jobs.put(j.getJobId(), j);

        StringBuilder apps = new StringBuilder();
        Map<Integer, String> appliedStatus = new HashMap<>();
        for (Application a : applicationService.getAllApplications()) {
            if (a.getCandidateId() != c.getId()) continue;
            appliedStatus.put(a.getJobId(), a.getStatus());
            if ("Withdrawn".equalsIgnoreCase(a.getStatus())) continue;
            Job j = jobs.get(a.getJobId());
            boolean canWithdraw = "Applied".equalsIgnoreCase(a.getStatus()) || "Interview".equalsIgnoreCase(a.getStatus());
            apps.append("<tr><td>").append(a.getApplicationId()).append("</td><td>").append(esc(j == null ? "Unknown" : j.getTitle()))
                    .append("</td><td>").append(esc(j == null ? "" : j.getCompany())).append("</td><td><span class=\"tag\">")
                    .append(esc(a.getStatus())).append("</span></td><td>")
                    .append(canWithdraw ? "<form method=\"post\" action=\"/candidate/withdraw\" class=\"inline\" onsubmit=\"return confirm('Withdraw this application?')\">"
                            + "<input type=\"hidden\" name=\"appId\" value=\"" + a.getApplicationId() + "\"><button class=\"danger\" type=\"submit\">Withdraw</button></form>" : "")
                    .append("</td></tr>");
        }

        StringBuilder open = new StringBuilder();
        for (Job j : jobs.values()) {
            String st = appliedStatus.get(j.getJobId());
            String action = st == null
                    ? "<form method=\"post\" action=\"/candidate/apply\" class=\"inline\"><input type=\"hidden\" name=\"jobId\" value=\""
                    + j.getJobId() + "\"><button type=\"submit\">Apply</button></form>"
                    : "<span class=\"muted\">" + esc(st.equalsIgnoreCase("Withdrawn") ? "Withdrawn" : "Applied") + "</span>";
            open.append("<tr><td>").append(j.getJobId()).append("</td><td>").append(esc(j.getTitle())).append("</td><td>")
                    .append(esc(j.getCompany())).append("</td><td>").append(esc(j.getLocation())).append("</td><td>")
                    .append(esc(j.getSalaryRange())).append("</td><td>").append(action).append("</td></tr>");
        }

        String body = "<h1>Welcome, " + esc(c.getName()) + "</h1>"
                + "<div class=\"card\"><h2>My profile</h2><p>Candidate ID <b>" + c.getId() + "</b> · " + esc(c.getEmail())
                + " · Skills: <b>" + esc(c.getSkill()) + "</b> · Experience: <b>" + c.getExperience() + " yrs</b> · Status: <span class=\"tag\">"
                + esc(c.getStatus()) + "</span></p><p class=\"muted\">To change your profile or delete your account, use the console app (Candidate portal).</p></div>"
                + "<div class=\"card\"><h2>My applications</h2>"
                + (apps.length() == 0 ? "<p class=\"muted\">You currently have 0 active job applications.</p>"
                : "<table><tr><th>App ID</th><th>Job</th><th>Company</th><th>Status</th><th></th></tr>" + apps + "</table>")
                + "</div><div class=\"card\"><h2>Job openings</h2>"
                + (open.length() == 0 ? "<p class=\"muted\">No job openings right now.</p>"
                : "<table><tr><th>ID</th><th>Title</th><th>Company</th><th>Location</th><th>Salary</th><th></th></tr>" + open + "</table>")
                + "</div>";
        sendPage(ex, s, "Candidate", c.getName() + " (Candidate #" + c.getId() + ")", body);
    }

    private void apply(HttpExchange ex, Session s, Map<String, String> f) throws IOException {
        int jobId = Html.toInt(f.get("jobId"), -1);
        Application[] app = new Application[1];
        String out = Html.capture(() -> app[0] = applicationService.applyJob(s.userId, jobId));
        flash(s, Html.lastLine(out), app[0] != null);
        redirect(ex, "/candidate");
    }

    private void withdraw(HttpExchange ex, Session s, Map<String, String> f) throws IOException {
        int appId = Html.toInt(f.get("appId"), -1);
        boolean[] ok = new boolean[1];
        String out = Html.capture(() -> ok[0] = applicationService.withdrawApplication(appId, s.userId));
        flash(s, Html.lastLine(out), ok[0]);
        redirect(ex, "/candidate");
    }

    // ------------------------------------------------------------------ helpers

    private static String pctCell(int p) {
        String cls = p >= 70 ? "hi" : p >= 40 ? "mid" : "lo";
        return "<div class=\"row\" style=\"gap:8px;align-items:center\"><span class=\"pct " + cls + "\">" + p
                + "%</span><div class=\"bar\" style=\"flex:1\"><i style=\"width:" + p + "%\"></i></div></div>";
    }

    private static String missing(RankedApplicant a) {
        return a.missingRequired().isEmpty() ? "–" : String.join(", ", a.missingRequired());
    }

    private Session currentSession(HttpExchange ex) {
        String token = cookie(ex);
        return token == null ? null : sessions.get(token);
    }

    private static String cookie(HttpExchange ex) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) return null;
        for (String h : headers) {
            for (String part : h.split(";")) {
                String p = part.trim();
                if (p.startsWith("TF_SESSION=")) return p.substring("TF_SESSION=".length());
            }
        }
        return null;
    }

    private boolean isRole(HttpExchange ex, Session s, String role) throws IOException {
        if (s == null || !role.equals(s.role)) {
            redirect(ex, "/");
            return false;
        }
        return true;
    }

    private static boolean post(HttpExchange ex, String method) throws IOException {
        if (!"POST".equals(method)) {
            redirect(ex, "/");
            return false;
        }
        return true;
    }

    private static void flash(Session s, String message, boolean ok) {
        s.flash = message;
        s.flashOk = ok;
    }

    private void sendPage(HttpExchange ex, Session s, String title, String userLine, String body) throws IOException {
        String flash = s.flash;
        boolean ok = s.flashOk;
        s.flash = null;
        send(ex, 200, Html.page(title, userLine, flash, ok, body));
    }

    private static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(303, -1);
    }

    private static void send(HttpExchange ex, int code, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
