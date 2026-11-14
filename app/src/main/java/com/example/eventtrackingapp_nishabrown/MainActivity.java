package com.example.eventtrackingapp_nishabrown;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Handles user login and account creation for the Date Mate application.
 */
public class MainActivity extends AppCompatActivity {

    private EditText editUsername;
    private EditText editPassword;
    private Button buttonLogin;
    private Button buttonCreateAccount;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Reference the login screen controls.
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);

        // Create access to the application's SQLite database.
        databaseHelper = new DatabaseHelper(this);

        // Attempt to authenticate the user when Log In is selected.
        buttonLogin.setOnClickListener(view -> loginUser());

        // Open the registration screen when Create New Account is selected.
        buttonCreateAccount.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Adjust the layout to prevent content from overlapping system bars.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    /**
     * Validates the entered credentials against the users table.
     */
    private void loginUser() {

        String username = editUsername.getText().toString();
        String password = editPassword.getText().toString();

        // Require both fields before attempting a database lookup.
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter a username and password.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Check the credentials stored in the SQLite database.
        if (databaseHelper.checkUser(username, password)) {

            // Retrieve the authenticated user's database ID.
            long userId = databaseHelper.getUserId(username);

            if (userId == -1) {
                Toast.makeText(
                        this,
                        "Unable to retrieve account information.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Toast.makeText(
                    this,
                    "Login successful.",
                    Toast.LENGTH_SHORT
            ).show();

            // Open HomeActivity with the user's information.
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USERNAME", username);
            startActivity(intent);

        } else {
            Toast.makeText(
                    this,
                    "Invalid username or password.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Creates a new user account using the entered credentials.
     */
    private void createAccount() {

        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // Require both fields before creating an account.
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter a username and password.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Prevent duplicate usernames from being stored.
        if (databaseHelper.usernameExists(username)) {
            Toast.makeText(
                    this,
                    "That username already exists.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Save the new account to the users table.
        if (databaseHelper.createUser(username, password)) {
            Toast.makeText(
                    this,
                    "Account created successfully.",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "Unable to create account.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}