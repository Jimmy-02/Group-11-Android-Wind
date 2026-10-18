
package com.example.myapplication.authentication.login;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import android.content.Intent;
import android.widget.TextView;

import com.example.myapplication.authentication.forgotpassword.ForgotPasswordActivity;
import com.example.myapplication.authentication.register.RegisterActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        TextView tvSignUp = findViewById(R.id.tv_sign_up);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }
}