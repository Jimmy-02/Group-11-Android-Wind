
package com.example.myapplication.authentication.forgotpassword;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.authentication.AuthConstant;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSendResetLink;
    private TextView tvBackToLogin;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        firebaseAuth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        btnSendResetLink = findViewById(R.id.btn_send_reset_link);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);

        btnSendResetLink.setOnClickListener(v -> sendResetEmail());

        tvBackToLogin.setOnClickListener(v -> finish());
    }


    private void sendResetEmail() {
        tilEmail.setError(null);

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();

        // Validate empty email
        if (email.isEmpty()) {
            tilEmail.setError("Please enter your email address");
            etEmail.requestFocus();
            return;
        }

        // Validate maximum email length
        if (email.length() > AuthConstant.MAX_EMAIL_LENGTH) {
            tilEmail.setError("Email cannot exceed 254 characters");
            etEmail.requestFocus();
            return;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        setLoading(true);

        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(this, task -> {
            setLoading(false);

            if (task.isSuccessful()) {
                Toast.makeText(this, "If an account exists for this email, a reset link will be sent.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Unable to send the reset email. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }


    private void setLoading(boolean loading) {
        btnSendResetLink.setEnabled(!loading);
        btnSendResetLink.setText(loading ? "Sending..." : "Send Reset Link");
    }
}