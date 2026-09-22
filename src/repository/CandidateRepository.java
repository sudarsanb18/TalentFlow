package repository;

import model.Candidate;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CandidateRepository handles database access for Candidate entities.
 * The Repository concept is preserved; persistent storage is now MySQL through JDBC.
 */
public class CandidateRepository {

    public int getNextId() {
        String sql = "SELECT COALESCE(MAX(id), 500) + 1 FROM candidate";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 501;
        } catch (SQLException e) {
            System.err.println("Failed to get next candidate ID: " + e.getMessage());
            return 501;
        }
    }

    public boolean isEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String sql = "SELECT 1 FROM candidate WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Failed to check candidate email: " + e.getMessage());
            return false;
        }
    }

    public boolean addCandidate(Candidate candidate) {
        if (candidate == null) return false;
        String sql = "INSERT INTO candidate (id, name, email, phone, password, skill, experience, status, resume_summary) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, candidate.getId());
            ps.setString(2, candidate.getName());
            ps.setString(3, candidate.getEmail());
            ps.setString(4, candidate.getPhone());
            ps.setString(5, candidate.getPassword());
            ps.setString(6, candidate.getSkill());
            ps.setDouble(7, candidate.getExperience());
            ps.setString(8, candidate.getStatus());
            ps.setString(9, candidate.getResumeSummary());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add candidate: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<Candidate> getAllCandidates() {
        ArrayList<Candidate> candidates = new ArrayList<>();
        String sql = "SELECT id, name, email, phone, password, skill, experience, status, resume_summary FROM candidate ORDER BY id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) candidates.add(mapCandidate(rs));
        } catch (SQLException e) {
            System.err.println("Failed to load candidates: " + e.getMessage());
        }
        return candidates;
    }

    public Candidate findById(int id) {
        String sql = "SELECT id, name, email, phone, password, skill, experience, status, resume_summary FROM candidate WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapCandidate(rs) : null;
            }
        } catch (SQLException e) {
            System.err.println("Failed to find candidate: " + e.getMessage());
            return null;
        }
    }

    public List<Candidate> findByName(String name) {
        List<Candidate> result = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) return result;
        String sql = "SELECT id, name, email, phone, password, skill, experience, status, resume_summary FROM candidate WHERE LOWER(name) LIKE LOWER(?) ORDER BY id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + name.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapCandidate(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to search candidates: " + e.getMessage());
        }
        return result;
    }

    public boolean updateCandidate(Candidate updatedCandidate) {
        if (updatedCandidate == null) return false;
        String sql = "UPDATE candidate SET name = ?, email = ?, phone = ?, password = ?, skill = ?, experience = ?, status = ?, resume_summary = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, updatedCandidate.getName());
            ps.setString(2, updatedCandidate.getEmail());
            ps.setString(3, updatedCandidate.getPhone());
            ps.setString(4, updatedCandidate.getPassword());
            ps.setString(5, updatedCandidate.getSkill());
            ps.setDouble(6, updatedCandidate.getExperience());
            ps.setString(7, updatedCandidate.getStatus());
            ps.setString(8, updatedCandidate.getResumeSummary());
            ps.setInt(9, updatedCandidate.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update candidate: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCandidate(int id) {
        String sql = "DELETE FROM candidate WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete candidate: " + e.getMessage());
            return false;
        }
    }

    public int getCount() {
        String sql = "SELECT COUNT(*) FROM candidate";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Failed to count candidates: " + e.getMessage());
            return 0;
        }
    }

    private Candidate mapCandidate(ResultSet rs) throws SQLException {
        return new Candidate(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("password"),
                rs.getString("skill"),
                rs.getDouble("experience"),
                rs.getString("status"),
                rs.getString("resume_summary")
        );
    }
}
