package com.example.myapplication.authentication.register;

import android.os.Bundle;
import android.content.Intent;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.authentication.login.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName;
    private TextInputLayout tilUsername;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;

    private TextInputEditText etFullName;
    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;

    private MaterialButton btnRegister;
    private TextView tvLoginLink;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Connect XML views
        tilFullName = findViewById(R.id.til_full_name);
        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etFullName = findViewById(R.id.et_full_name);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);

        // Register button
        btnRegister.setOnClickListener(v -> registerUser());

        // Return to Login screen
        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        clearErrors();

        String fullName = getText(etFullName);
        String username = getText(etUsername);
        String email = getText(etEmail);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        // Validate full name
        if (fullName.isEmpty()) {
            tilFullName.setError("Please enter your full name");
            etFullName.requestFocus();
            return;
        }

        // Validate username
        if (username.isEmpty()) {
            tilUsername.setError("Please enter a username");
            etUsername.requestFocus();
            return;
        }

        // Validate email
        if (email.isEmpty()) {
            tilEmail.setError("Please enter your email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Validate password
        if (password.isEmpty()) {
            tilPassword.setError("Please enter a password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("Password must contain at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // Prevent repeated taps while registering
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating account...");

        // Create Firebase Authentication account
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Create Account");

                Exception exception = task.getException();

                if (exception instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    tilEmail.setError("This email is already registered");
                } else {
                    String message = exception != null ? exception.getLocalizedMessage() : "Please try again";

                    Toast.makeText(this, "Registration failed: " + message, Toast.LENGTH_LONG).show();
                }
                return;
            }

            if (firebaseAuth.getCurrentUser() == null) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Create Account");

                Toast.makeText(this, "Unable to retrieve the new account", Toast.LENGTH_LONG).show();
                return;
            }

            String uid = firebaseAuth.getCurrentUser().getUid();

            // Create user profile in Firestore
            Map<String, Object> user = new HashMap<>();
            user.put("fullName", fullName);
            user.put("username", username);
            user.put("email", email);
            user.put("avatar", "");
            user.put("role", "USER");
            user.put("status", "ACTIVE");
            user.put("createdAt", FieldValue.serverTimestamp());
            user.put("updatedAt", FieldValue.serverTimestamp());

            firestore.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                    // Return to Login screen
                    firebaseAuth.signOut();

                    finish();
                }).addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Create Account");

                    Toast.makeText(this, "Account was created, but saving the profile failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
            });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }
}