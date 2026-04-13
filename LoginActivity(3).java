package com.example.cs360_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


// Handles login and registration for the app.
public class LoginActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        dbHelper = new DBHelper(this);
        usernameEditText = findViewById(R.id.editUsername);
        passwordEditText = findViewById(R.id.editPassword);
        loginButton = findViewById(R.id.buttonLogin);
        registerButton = findViewById(R.id.buttonRegister);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(view -> handleLogin());
        registerButton.setOnClickListener(view -> handleRegister());
    }

    private void handleLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!ValidationUtils.isValidUsername(username)) {
            showToast("Enter a username with at least 3 characters.");
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showToast("Enter a password with at least 4 characters.");
            return;
        }

        if (dbHelper.checkUser(username, password)) {
            showToast("Login successful.");

            Intent intent = new Intent(this, SmsPermissionActivity.class);
            startActivity(intent);
            finish();
        } else {
            showToast("Invalid username or password.");
        }
    }

    private void handleRegister() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!ValidationUtils.isValidUsername(username)) {
            showToast("Username must be at least 3 characters.");
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showToast("Password must be at least 4 characters.");
            return;
        }

        if (dbHelper.checkUserExists(username)) {
            showToast("Username already exists.");
            return;
        }

        if (dbHelper.registerUser(username, password)) {
            showToast("Registration successful. You can log in now.");
            clearInputFields();
        } else {
            showToast("Registration failed.");
        }
    }

    private void clearInputFields() {
        usernameEditText.setText("");
        passwordEditText.setText("");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
