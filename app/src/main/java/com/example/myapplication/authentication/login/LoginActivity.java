package com.example.myapplication.authentication.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.authentication.forgotpassword.ForgotPasswordActivity;
import com.example.myapplication.authentication.register.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        TextView tvSignUp = findViewById(R.id.tv_sign_up);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);

        btnLogin.setOnClickListener(v -> login());

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty()
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Vui lòng nhập email hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                btnLogin.setEnabled(true);

                Toast.makeText(this, "Đăng nhập thất bại. Vui lòng kiểm tra email và mật khẩu.", Toast.LENGTH_LONG).show();
                return;
            }

            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                rejectLogin("Không thể xác định tài khoản.");
                return;
            }

            db.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnCompleteListener(this, profileTask -> {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (!profileTask.isSuccessful()) {
                            rejectLogin("Không thể kiểm tra tài khoản. Vui lòng thử lại.");
                            return;
                        }

                        DocumentSnapshot document = profileTask.getResult();

                        if (document == null || !document.exists()) {
                            rejectLogin("Không tìm thấy hồ sơ người dùng.");
                            return;
                        }

                        checkUserStatus(document);
                    });
        });
    }


    private void checkUserStatus(DocumentSnapshot document) {
        String status = document.getString("status");

        if (status == null) {
            rejectLogin("Tài khoản thiếu trạng thái.");
            return;
        }

        status = status.trim().toUpperCase(Locale.ROOT);

        if ("BANNED".equals(status)) {
            rejectLogin("Tài khoản của bạn đã bị khóa.");
            return;
        }

        if (!"ACTIVE".equals(status)) {
            rejectLogin("Tài khoản chưa được phép truy cập.");
            return;
        }

        // Chỉ kiểm tra role khi status là ACTIVE
        checkUserRole(document);
    }

    private void checkUserRole(DocumentSnapshot document) {
        String role = document.getString("role");

        if (role == null) {
            rejectLogin("Tài khoản thiếu quyền truy cập.");
            return;
        }

        role = role.trim().toUpperCase(Locale.ROOT);

        if ("USER".equals(role)) {
            openMainScreen();
            return;
        }

        if ("ADMIN".equals(role)) {
            // TODO: Bỏ comment khi AdminActivity sẵn sàng.
            //
            // Intent intent = new Intent(
            //         LoginActivity.this,
            //         AdminActivity.class
            // );
            // intent.setFlags(
            //         Intent.FLAG_ACTIVITY_NEW_TASK
            //                 | Intent.FLAG_ACTIVITY_CLEAR_TASK
            // );
            // startActivity(intent);
            // finish();

            rejectLogin("Đăng nhập Admin hiện chưa được hỗ trợ.");
            return;
        }

        rejectLogin("Tài khoản không có quyền truy cập.");
    }

    private void rejectLogin(String message) {
        auth.signOut();
        btnLogin.setEnabled(true);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void openMainScreen() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}