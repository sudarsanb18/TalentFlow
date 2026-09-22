TALENTFLOW JDBC DEPENDENCY

This project uses the official MySQL Connector/J JDBC driver.

1. Download the current MySQL Connector/J release from:
   https://dev.mysql.com/downloads/connector/j/

2. Extract the JDBC JAR and place it in this folder as:
   mysql-connector-j-26.7.0.jar

3. The Eclipse .classpath file is already configured for that filename.

4. Open src/util/DBConnection.java and set your MySQL username/password.

The source code uses only java.sql APIs, so it compiles with the JDK;
the Connector/J JAR is required at runtime for an actual MySQL connection.
