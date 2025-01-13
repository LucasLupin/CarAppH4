package com.example.carapph4.Login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carapph4.CameraActivity;
import com.example.carapph4.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    // Firebase references
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Authentication and Database
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // UI components
        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.signInButton);
        Button signUpButton = findViewById(R.id.signUpButton); // Add reference to the Sign Up button

        // Login button functionality
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredemail = emailInput.getText().toString();
                String enteredPassword = passwordInput.getText().toString();

                if (enteredemail.isEmpty() || enteredPassword.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    validateLoginWithDatabase(enteredemail, enteredPassword);
                }
            }
        });

        // Sign Up button functionality
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to the SignupActivity
                navigateToSignupActivity();
            }
        });
    }

    /**
     * Validate login credentials using Firebase Realtime Database.
     */
    private void validateLoginWithDatabase(String email, String password) {
        // Convert email to Firebase-compatible key
        String firebaseKey = email.replace(".", "_");

        databaseReference.child("users").child(firebaseKey).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Retrieve stored password And Fullname
                    String storedPassword = snapshot.child("password").getValue(String.class);
                    String storedFullname = snapshot.child("fullname").getValue(String.class);

                    if (storedPassword != null && storedPassword.equals(password)) {
                        // Successful login
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToCameraActivity(storedFullname);
                    } else {
                        // Incorrect password
                        Toast.makeText(LoginActivity.this, "Invalid password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // User does not exist
                    Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Navigate to the CameraActivity after successful login.
     */
    private void navigateToCameraActivity(String fullName) {
        Intent intent = new Intent(LoginActivity.this, CameraActivity.class);
        intent.putExtra("fullname", fullName); // Pass the username to CameraActivity if needed
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to the SignupActivity.
     */
    private void navigateToSignupActivity() {
        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(intent);
    }
}
