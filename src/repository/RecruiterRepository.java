package repository;

import model.Recruiter;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * RecruiterRepository handles database access for Recruiter entities using JDBC.
 */
public class RecruiterRepository {

    public int getNextId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM recruiter";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) {
            System.err.println("Failed to get next recruiter ID: " + e.getMessage());
            return 1;
        }
    }

    public boolean isEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String sql = "SELECT 1 FROM recruiter WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Failed to check recruiter email: " + e.getMessage());
            return false;
        }
    }

    public boolean addRecruiter(Recruiter recruiter) {
        if (recruiter == null) return false;
        String sql = "INSERT INTO recruiter (id, name, email, phone, password, company) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, recruiter.getId());
            ps.setString(2, recruiter.getName());
            ps.setString(3, recruiter.getEmail());
            ps.setString(4, recruiter.getPhone());
            ps.setString(5, recruiter.getPassword());
            ps.setString(6, recruiter.getCompany());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add recruiter: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<Recruiter> getAllRecruiters() {
        ArrayList<Recruiter> recruiters = new ArrayList<>();
        String sql = "SELECT id, name, email, phone, password, company FROM recruiter ORDER BY id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) recruiters.add(mapRecruiter(rs));
        } catch (SQLException e) {
            System.err.println("Failed to load recruiters: " + e.getMessage());
        }
        return recruiters;
    }

    public Recruiter findById(int id) {
        String sql = "SELECT id, name, email, phone, password, company FROM recruiter WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRecruiter(rs) : null;
            }
        } catch (SQLException e) {
            System.err.println("Failed to find recruiter: " + e.getMessage());
            return null;
        }
    }

    public int getCount() {
        String sql = "SELECT COUNT(*) FROM recruiter";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Failed to count recruiters: " + e.getMessage());
            return 0;
        }
    }

    private Recruiter mapRecruiter(ResultSet rs) throws SQLException {
        return new Recruiter(rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("password"), rs.getString("company"));
    }
}
