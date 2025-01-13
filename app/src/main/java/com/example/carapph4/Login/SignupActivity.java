package com.example.carapph4.Login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carapph4.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    private EditText fullNameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button signUpButton, signInButton;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize views
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput); // Username (email)
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        signUpButton = findViewById(R.id.signUpButton);
        signInButton = findViewById(R.id.signInButton);

        // Handle Sign Up button click
        signUpButton.setOnClickListener(v -> registerUser());

        // Handle Sign In button click
        signInButton.setOnClickListener(v -> navigateToLoginActivity());
    }

    private void registerUser() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim(); // Email as username
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(SignupActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(SignupActivity.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert email to Firebase-compatible key
        String firebaseKey = email.replace(".", "_");

        // Check if the email already exists
        databaseReference.child(firebaseKey).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    // Email already exists
                    Toast.makeText(SignupActivity.this, "Email already registered", Toast.LENGTH_SHORT).show();
                } else {
                    // Save user data in the database
                    DatabaseReference userRef = databaseReference.child(firebaseKey);
                    userRef.child("fullname").setValue(fullName);
                    userRef.child("password").setValue(password)
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Toast.makeText(SignupActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                                    navigateToLoginActivity();
                                } else {
                                    Toast.makeText(SignupActivity.this, "Database error: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } else {
                Toast.makeText(SignupActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToLoginActivity() {
        // Navigate to LoginActivity
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
