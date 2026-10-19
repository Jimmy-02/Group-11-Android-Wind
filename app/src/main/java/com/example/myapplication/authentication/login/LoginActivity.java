package com.example.myapplication.authentication.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.authentication.forgotpassword.ForgotPasswordActivity;
import com.example.myapplication.authentication.register.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.Credential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;
    private Button btnGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

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

        checkExistingSession();
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

    private void signInWithGoogle() {
        GetSignInWithGoogleOption googleOption = new GetSignInWithGoogleOption.Builder(getString(R.string.default_web_client_id)).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(googleOption).build();

        btnGoogleSignIn.setEnabled(false);

        credentialManager.getCredentialAsync(this, request, null, getMainExecutor(), new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleGoogleCredential(result.getCredential());
            }

            @Override
            public void onError(GetCredentialException e) {
                btnGoogleSignIn.setEnabled(true);
                btnLogin.setEnabled(true);
                android.util.Log.e("GoogleSignIn", "Google sign-in failed", e);
                Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void handleGoogleCredential(Credential credential) {
        if (!(credential instanceof CustomCredential)) {
            btnGoogleSignIn.setEnabled(true);
            Toast.makeText(this, "Loại thông tin đăng nhập Google không hợp lệ.", Toast.LENGTH_LONG).show();
            return;
        }

        CustomCredential customCredential = (CustomCredential) credential;

        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
            btnGoogleSignIn.setEnabled(true);
            Toast.makeText(this, "Thông tin đăng nhập Google không hợp lệ.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());
            String idToken = googleCredential.getIdToken();
            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);

            auth.signInWithCredential(firebaseCredential).addOnCompleteListener(this, task -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (!task.isSuccessful()) {
                    btnGoogleSignIn.setEnabled(true);
                    Toast.makeText(this, "Đăng nhập Google thất bại. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                    return;
                }

                FirebaseUser user = auth.getCurrentUser();

                if (user == null) {
                    rejectGoogleLogin("Không thể xác định tài khoản Google.");
                    return;
                }

                AuthResult authResult = task.getResult();
                boolean isNewUser = authResult != null && authResult.getAdditionalUserInfo() != null && authResult.getAdditionalUserInfo().isNewUser();
                checkGoogleUserProfile(user, isNewUser);
            });
        } catch (Exception e) {
            btnGoogleSignIn.setEnabled(true);
            Toast.makeText(this, "Không thể đọc thông tin tài khoản Google.", Toast.LENGTH_LONG).show();
        }
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
    private void checkGoogleUserProfile(FirebaseUser user, boolean isNewUser) {
        db.collection("users").document(user.getUid()).get().addOnCompleteListener(this, task -> {
            if (isFinishing() || isDestroyed()) return;

            if (!task.isSuccessful()) {
                rejectGoogleLogin("Không thể kiểm tra hồ sơ người dùng.");
                return;
            }

            DocumentSnapshot document = task.getResult();

            if (document != null && document.exists()) {
                checkUserStatus(document);
                return;
            }

            if (!isNewUser) {
                rejectGoogleLogin("Không tìm thấy hồ sơ người dùng.");
                return;
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("authProvider", "GOOGLE");
            profile.put("email", user.getEmail() == null ? "" : user.getEmail());
            profile.put("fullName", user.getDisplayName() == null ? "" : user.getDisplayName());
            profile.put("avatar", user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString());
            profile.put("username", user.getEmail() == null ? user.getUid() : user.getEmail().split("@")[0]);
            profile.put("role", "USER");
            profile.put("status", "ACTIVE");
            profile.put("createdAt", FieldValue.serverTimestamp());
            profile.put("updatedAt", FieldValue.serverTimestamp());

            db.collection("users").document(user.getUid()).set(profile).addOnCompleteListener(this, createTask -> {
                if (isFinishing() || isDestroyed()) return;

                if (!createTask.isSuccessful()) {
                    rejectGoogleLogin("Không thể tạo hồ sơ người dùng.");
                    return;
                }

                openMainScreen();
            });
        });
    }

    private void checkExistingSession() {
        FirebaseUser user = auth.getCurrentUser();

        // Chưa đăng nhập thì giữ nguyên màn hình Login.
        if (user == null) {
            return;
        }

        // Tạm khóa nút trong lúc kiểm tra phiên đăng nhập.
        btnLogin.setEnabled(false);
        btnGoogleSignIn.setEnabled(false);

        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(this, task -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!task.isSuccessful()) {
                        rejectLogin("Không thể kiểm tra tài khoản. Vui lòng thử lại.");
                        return;
                    }

                    DocumentSnapshot document = task.getResult();

                    if (document == null || !document.exists()) {
                        rejectLogin("Không tìm thấy hồ sơ người dùng.");
                        return;
                    }

                    checkUserStatus(document);
                });
    }

    private void rejectGoogleLogin(String message) {
        auth.signOut();
        btnLogin.setEnabled(true);
        btnGoogleSignIn.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void rejectLogin(String message) {
        auth.signOut();
        btnLogin.setEnabled(true);
        btnGoogleSignIn.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    private void openMainScreen() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}