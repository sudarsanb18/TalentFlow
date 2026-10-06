package repository;

import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * JobMatchRepository stores the optional matching rules of a job (preferred skills, minimum experience)
 * in its own table, so the original job table and its SQL stay untouched.
 * The table is created automatically the first time this class is used.
 */
public class JobMatchRepository {

    /** Matching rules of one job. */
    public record JobRule(int jobId, String preferredSkills, double minExperience) {
    }

    public JobMatchRepository() {
        ensureTable();
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS job_match_rule ("
                + "job_id INT PRIMARY KEY, "
                + "preferred_skills VARCHAR(255) NOT NULL DEFAULT '', "
                + "min_experience DECIMAL(4,1) NOT NULL DEFAULT 0.0, "
                + "FOREIGN KEY (job_id) REFERENCES job(job_id) ON DELETE CASCADE)";
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Failed to prepare job_match_rule table: " + e.getMessage());
        }
    }

    public boolean saveRule(JobRule rule) {
        if (rule == null) return false;
        String sql = "INSERT INTO job_match_rule (job_id, preferred_skills, min_experience) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE preferred_skills = VALUES(preferred_skills), min_experience = VALUES(min_experience)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rule.jobId());
            ps.setString(2, rule.preferredSkills());
            ps.setDouble(3, rule.minExperience());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to save job match rule: " + e.getMessage());
            return false;
        }
    }

    /** Rules of a job; a job without saved rules gets no preferred skills and no minimum experience. */
    public JobRule findRule(int jobId) {
        String sql = "SELECT preferred_skills, min_experience FROM job_match_rule WHERE job_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new JobRule(jobId, rs.getString(1), rs.getDouble(2));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load job match rule: " + e.getMessage());
        }
        return new JobRule(jobId, "", 0.0);
    }

    /** Application ID -> time of application, used as the final ranking tie-break. */
    public Map<Integer, Long> getAppliedAtByJob(int jobId) {
        Map<Integer, Long> out = new HashMap<>();
        String sql = "SELECT application_id, applied_at FROM application WHERE job_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp t = rs.getTimestamp(2);
                    out.put(rs.getInt(1), t == null ? Long.MAX_VALUE : t.getTime());
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load application times: " + e.getMessage());
        }
        return out;
    }
}
