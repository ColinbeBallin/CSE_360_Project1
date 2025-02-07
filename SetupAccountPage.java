package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import passwordEvaluationTestbed.PasswordEvaluator;

import java.sql.SQLException;

import databasePart1.*;

/**
 * SetupAccountPage class handles the account setup process for new users.
 * Users provide their userName, password, and a valid invitation code to register.
 */
public class SetupAccountPage {

    private final DatabaseHelper databaseHelper;

    // Constructor to initialize DatabaseHelper
    public SetupAccountPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    /**
     * Displays the Setup Account page in the provided stage.
     * @param primaryStage The primary stage where the scene will be displayed.
     */
    public void show(Stage primaryStage) {
        // Input fields for userName, password, and invitation code
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter userName");
        userNameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);

        TextField inviteCodeField = new TextField();
        inviteCodeField.setPromptText("Enter Invitation Code");
        inviteCodeField.setMaxWidth(250);

        // Label to display error messages for invalid input or registration issues
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        Button setupButton = new Button("Setup");

        setupButton.setOnAction(a -> {
            // Retrieve user input
            String userName = userNameField.getText(); // get the entered username
            String password = passwordField.getText(); // get the entered password
            String code = inviteCodeField.getText(); // get the entered invitation code

            // Validate the username using a dedicated utility
            String usernameValidation = UserNameRecognizer.checkForValidUserName(userName);
            // Validate the password strenfgth using a password evaluation utility
            String passwordValidation = PasswordEvaluator.evaluatePassword(password);
            
            // If username validation fails, display an error message on account page
            if (!usernameValidation.isEmpty()) {
                errorLabel.setText("Username error: " + usernameValidation);
                return; 
            }
            // If the password validation fails, display an error message on the account page
            if (!passwordValidation.isEmpty()) {
                errorLabel.setText("Password error: " + passwordValidation);
                return;
            }

            try {
                // Check if the user already exists
                if (!databaseHelper.doesUserExist(userName)) {

                    // Validate the invitation code with expiration check
                    if (databaseHelper.validateInvitationCode(code)) {
                        // Create a new user and register them in the database
                        User user = new User(userName, password, "user");
                        databaseHelper.register(user);

                        // Mark the invitation code as used only after successful registration
                        databaseHelper.markInvitationCodeAsUsed(code);

                        // Navigate to the Welcome Login Page
                        new WelcomeLoginPage(databaseHelper).show(primaryStage, user);
                    } else {
                        errorLabel.setText("Invalid or expired invitation code. Please request a new code.");
                    }
                } else {
                    errorLabel.setText("This userName is taken! Please use another to set up an account.");
                }

            } catch (SQLException e) {
                errorLabel.setText("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        layout.getChildren().addAll(userNameField, passwordField, inviteCodeField, setupButton, errorLabel);

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Account Setup");
        primaryStage.show();
    }
}
