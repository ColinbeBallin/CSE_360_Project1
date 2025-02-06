package databasePart1;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import application.User;


/**
 * The DatabaseHelper class is responsible for managing the connection to the database,
 * performing operations such as user registration, login validation, and handling invitation codes.
 */
public class DatabaseHelper {

	// JDBC driver name and database URL 
	static final String JDBC_DRIVER = "org.h2.Driver";   
	static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

	//  Database credentials 
	static final String USER = "sa"; 
	static final String PASS = ""; 

	private Connection connection = null;
	private Statement statement = null; 
	//	PreparedStatement pstmt

	public void connectToDatabase() throws SQLException {
		try {
			Class.forName(JDBC_DRIVER); // Load the JDBC driver
			System.out.println("Connecting to database...");
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connection.createStatement(); 
			// You can use this command to clear the database and restart from fresh.
			//statement.execute("DROP ALL OBJECTS");

			createTables();  // Create the necessary tables if they don't exist
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver not found: " + e.getMessage());
		}
	}

	 private void createTables() throws SQLException {
	        String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
	                + "id INT AUTO_INCREMENT PRIMARY KEY, "
	                + "userName VARCHAR(255) UNIQUE, "
	                + "password VARCHAR(255), "
	                + "role VARCHAR(20))";
	        statement.execute(userTable);

	        // Create the invitation codes table
	        String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
	                + "code VARCHAR(10) PRIMARY KEY, "
	                + "isUsed BOOLEAN DEFAULT FALSE, "
	                + "deadline TIMESTAMP DEFAULT DATEADD('MINUTE', 2, CURRENT_TIMESTAMP))"; // modified column to store a timestamp that represents the expiration time of invitation code.
					// DEFAULT DATEADD - adds a deadline or expiry time of 2 minutes.
	        statement.execute(invitationCodesTable);

	        System.out.println("Invitation codes table created successfully with deadline feature.");  // prints the message to the console
	    }



	// Check if the database is empty
	public boolean isDatabaseEmpty1() throws SQLException {
	    System.out.println("Checking if database is empty...");  // prints message to console indicating the method is going to check the database
	    String query = "SELECT COUNT(*) AS count FROM cse360users";
	    ResultSet resultSet = statement.executeQuery(query);
	    if (resultSet.next()) {
	        int count = resultSet.getInt("count"); // extracts the value of the count column from the ResultSet, giving the number of rows in the cse360users table
	        System.out.println("Number of users: " + count); //displays the number of users found in the table.
	        return count == 0;
	    }
	    return true;
	}


	// Registers a new user in the database.
	public void register(User user) throws SQLException {
		String insertUser = "INSERT INTO cse360users (userName, password, role) VALUES (?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			pstmt.setString(3, user.getRole());
			pstmt.executeUpdate();
		}
	}

	// Validates a user's login credentials.
	public boolean login(User user) throws SQLException {
		String query = "SELECT * FROM cse360users WHERE userName = ? AND password = ? AND role = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			pstmt.setString(3, user.getRole());
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		}
	}
	
	// Checks if a user already exists in the database based on their userName.
	public boolean doesUserExist(String userName) {
	    String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            // If the count is greater than 0, the user exists
	            return rs.getInt(1) > 0;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false; // If an error occurs, assume user doesn't exist
	}
	
	// Retrieves the role of a user from the database using their UserName.
	public String getUserRole(String userName) {
	    String query = "SELECT role FROM cse360users WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getString("role"); // Return the role if user exists
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return null; // If no user exists or an error occurs
	}
	
	// Generates a new invitation code and inserts it into the database.
	public String generateInvitationCode() {
	    String code = UUID.randomUUID().toString().substring(0, 4); // Generate a random 4-character code

	    String query = "INSERT INTO InvitationCodes (code) VALUES (?)";

	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.executeUpdate();
	        System.out.println("Generated invitation code: " + code); // prints the generated invitation code to the console, provi
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return code;
	}

	
	// Validates an invitation code to check if it is unused.
	public boolean validateInvitationCode(String code) {
	    String query = "SELECT isUsed FROM InvitationCodes WHERE code = ? " + // exists in the database
	                   "AND isUsed = FALSE " + // it has not been used
	                   "AND deadline > CURRENT_TIMESTAMP"; // not expired

	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {     // Checks if the query returned a row, moves th cursor
	            // Valid code, mark it as used
	            markInvitationCodeAsUsed(code);
	            return true;
	        } else {
	            System.out.println("Invitation code is either expired or invalid.");  // if no matchign invitation code was found, this message is printed to inform the user
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}

	
	// Marks the invitation code as used in the database.
	public void markInvitationCodeAsUsed(String code) {
	    String query = "UPDATE InvitationCodes SET isUsed = TRUE WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	// Updates the password of a user in the database.
    public void updatePassword(String userName, String newPassword) throws SQLException { 
		// Defines a SQL query to update password for a specific user
        String query = "UPDATE cse360users SET password = ? WHERE userName = ?";

		// Using a try- with- resources block to create a PreparedStatement and ensure resources are closed
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			// Set the first placeholder(?) to the new password
            pstmt.setString(1, newPassword);

			// Set the second placeholder(?) to the userName
            pstmt.setString(2, userName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

	// Closes the database connection and statement.
	public void closeConnection() {
		try{ 
			if(statement!=null) statement.close(); 
		} catch(SQLException se2) { 
			se2.printStackTrace();
		} 
		try { 
			if(connection!=null) connection.close(); 
		} catch(SQLException se){ 
			se.printStackTrace(); 
		} 
	}
		
}

