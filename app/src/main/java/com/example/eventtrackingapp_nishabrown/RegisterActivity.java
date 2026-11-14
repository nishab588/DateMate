package com.example.eventtrackingapp_nishabrown;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Handles account registration for the DateMate application.
 */
public class RegisterActivity extends AppCompatActivity {

    // Registration form fields.
    private EditText editFirstName;
    private EditText editLastName;
    private EditText editEmail;
    private EditText editPhone;
    private EditText editRegisterUsername;
    private EditText editRegisterPassword;
    private EditText editConfirmPassword;

    // Registration buttons.
    private Button buttonRegister;
    private Button buttonBackToLogin;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the registration screen.
        setContentView(R.layout.activity_register);

        // Connect the Java variables to the XML form fields.
        editFirstName = findViewById(R.id.editFirstName);
        editLastName = findViewById(R.id.editLastName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editRegisterUsername = findViewById(R.id.editRegisterUsername);
        editRegisterPassword = findViewById(R.id.editRegisterPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);

        // Connect the registration buttons.
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonBackToLogin = findViewById(R.id.buttonBackToLogin);

        // Initialize database access.
        databaseHelper = new DatabaseHelper(this);

        // Validate the registration form when Create Account is selected.
        buttonRegister.setOnClickListener(view -> validateRegistration());

        // Return to the login screen.
        buttonBackToLogin.setOnClickListener(view -> finish());
    }


    /**
     * Validates the information entered on the registration form.
     * Account creation will be implemented after database updates.
     */
    private void validateRegistration() {

        // Retrieve the information entered by the user.
        String firstName = editFirstName.getText().toString().trim();
        String lastName = editLastName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String username = editRegisterUsername.getText().toString().trim();

        // Preserve the exact password entered, including spaces.
        String password = editRegisterPassword.getText().toString();
        String confirmPassword = editConfirmPassword.getText().toString();

        // Validate the required registration fields.
        if (firstName.isEmpty()) {
            editFirstName.setError("First name is required.");
            editFirstName.requestFocus();
            return;
        }

        // Last name is optional and does not require validation.

        if (email.isEmpty()) {
            editEmail.setError("Email address is required.");
            editEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Enter a valid email address.");
            editEmail.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            editPhone.setError("Phone number is required.");
            editPhone.requestFocus();
            return;
        }

        // Remove common phone-number formatting before checking length.
        String phoneDigits = phone.replaceAll("[^0-9]", "");

        if (phoneDigits.length() != 10) {
            editPhone.setError("Enter a valid 10-digit phone number.");
            editPhone.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            editRegisterUsername.setError("Username is required.");
            editRegisterUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editRegisterPassword.setError("Password is required.");
            editRegisterPassword.requestFocus();
            return;
        }

        // Require 10 characters, a letter, a number, and a special character.
        String passwordPattern =
                "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{10,}$";

        if (!password.matches(passwordPattern)) {
            editRegisterPassword.setError(
                    "Password must have at least 10 characters, " +
                    " and should include at least one letter, one number, and one special character."
            );
            editRegisterPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            editConfirmPassword.setError("Please confirm your password.");
            editConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editConfirmPassword.setError("Passwords do not match.");
            editConfirmPassword.requestFocus();
            return;
        }

        // Check whether the username is already registered.
        if (databaseHelper.usernameExists(username)) {
            editRegisterUsername.setError("This username is already taken.");
            editRegisterUsername.requestFocus();
            return;
        }

        // Check whether the email address is already registered.
        if (databaseHelper.emailExists(email)) {
            editEmail.setError("An account already uses this email address.");
            editEmail.requestFocus();
            return;
        }

        // Create the account with a securely hashed password.
        long userId;

        try {
            userId = databaseHelper.createUser(
                    firstName,
                    lastName,
                    email,
                    phoneDigits,
                    username,
                    password
            );
        } catch (RuntimeException e) {
            Toast.makeText(
                    this,
                    "Unable to create account. Please try again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Stop if the database could not create the account.
        if (userId == -1) {
            Toast.makeText(
                    this,
                    "Account creation failed. Please check your information.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Registration succeeded.
        Toast.makeText(
                this,
                "Account created successfully!",
                Toast.LENGTH_SHORT
        ).show();

        // Open the Home screen for the newly registered user.
        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USERNAME", username);

        // Remove the login and registration screens from the back stack.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();

    }
}