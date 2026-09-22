package repository;

import model.Job;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JobRepository manages Job persistence through MySQL/JDBC.
 */
public class JobRepository {

    public int getNextId() {
        String sql = "SELECT COALESCE(MAX(job_id), 100) + 1 FROM job";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 101;
        } catch (SQLException e) {
            System.err.println("Failed to get next job ID: " + e.getMessage());
            return 101;
        }
    }

    public boolean addJob(Job job) {
        if (job == null) return false;
        String sql = "INSERT INTO job (job_id, title, company, location, salary_range, required_skill) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, job.getJobId());
            ps.setString(2, job.getTitle());
            ps.setString(3, job.getCompany());
            ps.setString(4, job.getLocation());
            ps.setString(5, job.getSalaryRange());
            ps.setString(6, job.getRequiredSkill());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add job: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<Job> getAllJobs() {
        ArrayList<Job> jobs = new ArrayList<>();
        String sql = "SELECT job_id, title, company, location, salary_range, required_skill FROM job ORDER BY job_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) jobs.add(mapJob(rs));
        } catch (SQLException e) {
            System.err.println("Failed to load jobs: " + e.getMessage());
        }
        return jobs;
    }

    public Job findById(int id) {
        String sql = "SELECT job_id, title, company, location, salary_range, required_skill FROM job WHERE job_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapJob(rs) : null;
            }
        } catch (SQLException e) {
            System.err.println("Failed to find job: " + e.getMessage());
            return null;
        }
    }

    public List<Job> findByTitle(String title) {
        List<Job> result = new ArrayList<>();
        if (title == null || title.trim().isEmpty()) return result;
        String sql = "SELECT job_id, title, company, location, salary_range, required_skill FROM job WHERE LOWER(title) LIKE LOWER(?) ORDER BY job_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + title.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapJob(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to search jobs: " + e.getMessage());
        }
        return result;
    }

    public boolean updateJob(Job updatedJob) {
        if (updatedJob == null) return false;
        String sql = "UPDATE job SET title = ?, company = ?, location = ?, salary_range = ?, required_skill = ? WHERE job_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, updatedJob.getTitle());
            ps.setString(2, updatedJob.getCompany());
            ps.setString(3, updatedJob.getLocation());
            ps.setString(4, updatedJob.getSalaryRange());
            ps.setString(5, updatedJob.getRequiredSkill());
            ps.setInt(6, updatedJob.getJobId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update job: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteJob(int id) {
        String sql = "DELETE FROM job WHERE job_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete job: " + e.getMessage());
            return false;
        }
    }

    public int getCount() {
        String sql = "SELECT COUNT(*) FROM job";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Failed to count jobs: " + e.getMessage());
            return 0;
        }
    }

    private Job mapJob(ResultSet rs) throws SQLException {
        return new Job(rs.getInt("job_id"), rs.getString("title"), rs.getString("company"),
                rs.getString("location"), rs.getString("salary_range"), rs.getString("required_skill"));
    }
}
