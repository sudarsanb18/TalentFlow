package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central JDBC connection utility for TalentFlow.
 * Demonstrates JDBC DriverManager, Connection and SQLException handling.
 */
public final class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/talentflow_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "SudarsanSQL123";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
