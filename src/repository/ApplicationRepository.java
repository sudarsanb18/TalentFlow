package repository;

import model.Application;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ApplicationRepository manages application records through JDBC.
 * Database foreign keys provide the same cascade-integrity concept as before.
 */
public class ApplicationRepository {

    public int getNextId() {
        String sql = "SELECT COALESCE(MAX(application_id), 1000) + 1 FROM application";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1001;
        } catch (SQLException e) {
            System.err.println("Failed to get next application ID: " + e.getMessage());
            return 1001;
        }
    }

    public boolean addApplication(Application app) {
        if (app == null) return false;
        String sql = "INSERT INTO application (application_id, candidate_id, job_id, status) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, app.getApplicationId());
            ps.setInt(2, app.getCandidateId());
            ps.setInt(3, app.getJobId());
            ps.setString(4, app.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add application: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<Application> getAllApplications() {
        ArrayList<Application> applications = new ArrayList<>();
        String sql = "SELECT application_id, candidate_id, job_id, status FROM application ORDER BY application_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) applications.add(mapApplication(rs));
        } catch (SQLException e) {
            System.err.println("Failed to load applications: " + e.getMessage());
        }
        return applications;
    }

    public Application findById(int id) {
        String sql = "SELECT application_id, candidate_id, job_id, status FROM application WHERE application_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapApplication(rs) : null;
            }
        } catch (SQLException e) {
            System.err.println("Failed to find application: " + e.getMessage());
            return null;
        }
    }

    public List<Application> findByCandidateId(int candidateId) {
        return findByForeignKey("candidate_id", candidateId);
    }

    public List<Application> findByJobId(int jobId) {
        return findByForeignKey("job_id", jobId);
    }

    private List<Application> findByForeignKey(String column, int value) {
        List<Application> result = new ArrayList<>();
        String sql = "SELECT application_id, candidate_id, job_id, status FROM application WHERE " + column + " = ? ORDER BY application_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapApplication(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to search applications: " + e.getMessage());
        }
        return result;
    }

    public void deleteApplicationsByCandidateId(int candidateId) {
        deleteApplications("candidate_id", candidateId);
    }

    public void deleteApplicationsByJobId(int jobId) {
        deleteApplications("job_id", jobId);
    }

    private void deleteApplications(String column, int value) {
        String sql = "DELETE FROM application WHERE " + column + " = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to delete related applications: " + e.getMessage());
        }
    }

    public boolean updateStatus(int applicationId, String newStatus) {
        String sql = "UPDATE application SET status = ? WHERE application_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, applicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update application status: " + e.getMessage());
            return false;
        }
    }

    public int getCount() {
        String sql = "SELECT COUNT(*) FROM application";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Failed to count applications: " + e.getMessage());
            return 0;
        }
    }

    private Application mapApplication(ResultSet rs) throws SQLException {
        return new Application(rs.getInt("application_id"), rs.getInt("candidate_id"),
                rs.getInt("job_id"), rs.getString("status"));
    }
}
