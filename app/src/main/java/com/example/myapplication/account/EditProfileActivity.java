
package com.example.myapplication.account;
import android.text.InputType;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.EmailAuthProvider;
import android.os.Bundle;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;
import com.example.myapplication.ImageLoader;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputLayout tilFullName;
    private TextInputLayout tilUsername;
    private ImageView ivAvatarPreview;
    private TextInputEditText etFullName;
    private TextInputEditText etUsername;
    private TextInputEditText etAvatar;
    private TextInputEditText etEmail;
    private MaterialButton btnSaveProfile;
    private DocumentReference userRef;
    private String originalFullName = "";
    private String originalUsername = "";
    private String originalAvatar = "";
    private String originalEmail = "";
    private TextInputLayout tilEmail;
    private boolean canChangeEmail = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Khởi tạo giao diện trước
        setContentView(R.layout.activity_edit_profile);

        // 2. Ánh xạ các View
        tilFullName = findViewById(R.id.til_full_name);
        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);

        etAvatar = findViewById(R.id.et_avatar);
        etFullName = findViewById(R.id.et_full_name);
        etUsername = findViewById(R.id.et_username);
        etAvatar = findViewById(R.id.et_avatar);
        etEmail = findViewById(R.id.et_email);
        ivAvatarPreview = findViewById(R.id.iv_avatar_preview);
        etAvatar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

                String url = s.toString().trim();

                boolean validUrl =
                        (url.startsWith("https://")
                                || url.startsWith("http://"))
                                && android.util.Patterns.WEB_URL
                                .matcher(url)
                                .matches();

                if (validUrl) {
                    ImageLoader.load(url, ivAvatarPreview);
                } else {
                    ImageLoader.load("", ivAvatarPreview);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        btnSaveProfile = findViewById(R.id.btn_save_profile);

        findViewById(R.id.layoutBack).setOnClickListener(v -> finish());

        // 3. Kiểm tra tài khoản Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập trước.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // 4. Trỏ đến hồ sơ của tài khoản đang đăng nhập
        userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid());

        // 5. Tải dữ liệu hồ sơ từ Firestore
        loadProfile(user);

        // 6. Xử lý nút lưu
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && userRef != null) {
            syncVerifiedEmail(user);
        }
    }

    private void loadProfile(FirebaseUser user) {
        btnSaveProfile.setEnabled(false);

        userRef.get().addOnSuccessListener(document -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (!document.exists()) {
                Toast.makeText(
                        this,
                        "Không tìm thấy hồ sơ người dùng.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            String fullName = document.getString("fullName");
            String username = document.getString("username");
            String avatar = document.getString("avatar");

            String authProvider = document.getString("authProvider");
            // Chỉ tài khoản EMAIL_PASSWORD mới được đổi email
            canChangeEmail =
                    "EMAIL_PASSWORD".equalsIgnoreCase(authProvider);
            // Khóa ô Email Address đối với tài khoản Google
            tilEmail.setEnabled(canChangeEmail);
            etEmail.setEnabled(canChangeEmail);


            originalFullName = fullName != null ? fullName : "";
            originalUsername = username != null ? username : "";
            originalAvatar = avatar != null ? avatar : "";

            etAvatar.setText(avatar != null ? avatar : "");
            etFullName.setText(fullName != null ? fullName : "");
            etUsername.setText(username != null ? username : "");
            etAvatar.setText(avatar != null ? avatar : "");

            ImageLoader.load(avatar, ivAvatarPreview);

            String email = user.getEmail();

            if (email == null || email.isEmpty()) {
                email = document.getString("email");
            }

            originalEmail = email != null ? email.trim() : "";
            etEmail.setText(originalEmail);

            btnSaveProfile.setEnabled(true);

        }).addOnFailureListener(e -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            btnSaveProfile.setEnabled(true);

            Toast.makeText(
                    this,
                    "Không thể tải hồ sơ. Vui lòng thử lại.",
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private void saveProfile() {
        tilFullName.setError(null);
        tilUsername.setError(null);
        etAvatar.setError(null);

        String fullName = getText(etFullName);
        String username = getText(etUsername);
        String avatar = getText(etAvatar);
        String newEmail = canChangeEmail
                ? getText(etEmail)
                : originalEmail;

        if (fullName.isEmpty()) {
            tilFullName.setError("Vui lòng nhập họ tên.");
            etFullName.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            tilUsername.setError("Vui lòng nhập username.");
            etUsername.requestFocus();
            return;
        }

        if (newEmail.isEmpty()
                || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(newEmail)
                .matches()) {

            etEmail.setError("Vui lòng nhập email hợp lệ.");
            etEmail.requestFocus();
            return;
        }

        etEmail.setError(null);

        // Kiểm tra URL ảnh đại diện nếu có nhập
        if (!avatar.isEmpty()) {
            boolean hasHttpScheme =
                    avatar.startsWith("https://")
                            || avatar.startsWith("http://");

            boolean isValidUrl =
                    android.util.Patterns.WEB_URL
                            .matcher(avatar)
                            .matches();

            if (!hasHttpScheme || !isValidUrl) {
                etAvatar.setError(
                        "Vui lòng nhập URL ảnh bắt đầu bằng http:// hoặc https://."
                );
                etAvatar.requestFocus();
                return;
            }
        }

        // Chỉ đưa những trường thực sự thay đổi vào danh sách cập nhật
        Map<String, Object> updates = new HashMap<>();
        if (!fullName.equals(originalFullName)) {
            updates.put("fullName", fullName);
        }
        if (!username.equals(originalUsername)) {
            updates.put("username", username);
        }
        if (!avatar.equals(originalAvatar)) {
            updates.put("avatar", avatar);
        }

        // Kiểm tra email có thay đổi không
        boolean emailChanged =
                !newEmail.equalsIgnoreCase(originalEmail);

// Nếu không có bất kỳ thay đổi nào
        if (updates.isEmpty() && !emailChanged) {
            Toast.makeText(
                    this,
                    "Thông tin chưa thay đổi.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

// Cập nhật thời gian sửa hồ sơ
        updates.put("updatedAt", FieldValue.serverTimestamp());

        btnSaveProfile.setEnabled(false);

        userRef.update(updates)
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (emailChanged) {
                        FirebaseUser currentUser =
                                FirebaseAuth.getInstance().getCurrentUser();

                        if (currentUser == null) {
                            btnSaveProfile.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Vui lòng đăng nhập lại.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        // Gửi email xác minh địa chỉ mới
                        requestEmailChange(currentUser, newEmail);

                    } else {
                        Toast.makeText(
                                this,
                                "Cập nhật hồ sơ thành công.",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnSaveProfile.setEnabled(true);

                    android.util.Log.e(
                            "EditProfile",
                            "Failed to update profile",
                            e
                    );

                    Toast.makeText(
                            this,
                            "Lỗi cập nhật: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
    private void requestEmailChange(FirebaseUser user, String newEmail) {
        String normalizedEmail =
                newEmail.trim().toLowerCase(java.util.Locale.ROOT);

        FirebaseFirestore.getInstance()
                .collection("emailIndex")
                .document(normalizedEmail)
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    String indexedUid = document.getString("uid");

                    if (document.exists()
                            && !user.getUid().equals(indexedUid)) {
                        btnSaveProfile.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Email này đã được tài khoản khác sử dụng.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    showReauthenticationDialog(user, newEmail);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnSaveProfile.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Không thể kiểm tra email. Vui lòng thử lại.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void syncVerifiedEmail(FirebaseUser user) {
        user.reload()
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    String email = user.getEmail();

                    if (email == null || email.trim().isEmpty()) {
                        return;
                    }

                    final String verifiedEmail = email.trim();

                    userRef.get().addOnSuccessListener(profile -> {
                        if (isFinishing() || isDestroyed()
                                || !profile.exists()) {
                            return;
                        }

                        String oldEmail = profile.getString("email");

                        if (oldEmail != null
                                && oldEmail.trim()
                                .equalsIgnoreCase(verifiedEmail)) {
                            return;
                        }

                        FirebaseFirestore db =
                                FirebaseFirestore.getInstance();

                        String newKey = verifiedEmail.toLowerCase(
                                java.util.Locale.ROOT
                        );

                        DocumentReference newIndexRef = db
                                .collection("emailIndex")
                                .document(newKey);

                        String oldKey = oldEmail == null
                                ? ""
                                : oldEmail.trim().toLowerCase(
                                java.util.Locale.ROOT
                        );

                        DocumentReference oldIndexRef = oldKey.isEmpty()
                                ? null
                                : db.collection("emailIndex")
                                .document(oldKey);

                        db.runTransaction(transaction -> {
                            DocumentSnapshot newIndex =
                                    transaction.get(newIndexRef);

                            DocumentSnapshot oldIndex = null;

                            if (oldIndexRef != null) {
                                oldIndex = transaction.get(oldIndexRef);
                            }

                            String indexedUid =
                                    newIndex.getString("uid");

                            if (newIndex.exists()
                                    && !user.getUid().equals(indexedUid)) {
                                throw new IllegalStateException(
                                        "Email đã được tài khoản khác sử dụng."
                                );
                            }

                            Map<String, Object> indexData =
                                    new HashMap<>();

                            indexData.put("uid", user.getUid());

                            transaction.set(newIndexRef, indexData);

                            if (oldIndexRef != null
                                    && !oldIndexRef.getId()
                                    .equals(newIndexRef.getId())
                                    && oldIndex != null
                                    && oldIndex.exists()
                                    && user.getUid().equals(
                                    oldIndex.getString("uid"))) {
                                transaction.delete(oldIndexRef);
                            }

                            Map<String, Object> updates =
                                    new HashMap<>();

                            updates.put("email", verifiedEmail);
                            updates.put(
                                    "updatedAt",
                                    FieldValue.serverTimestamp()
                            );

                            transaction.update(userRef, updates);

                            return null;
                        }).addOnSuccessListener(result -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            originalEmail = verifiedEmail;
                            etEmail.setText(verifiedEmail);

                            Toast.makeText(
                                    this,
                                    "Email đã được xác minh và đồng bộ.",
                                    Toast.LENGTH_LONG
                            ).show();

                        }).addOnFailureListener(e -> {
                            android.util.Log.e(
                                    "EditProfile",
                                    "Không thể đồng bộ email",
                                    e
                            );

                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(
                                        this,
                                        "Không thể đồng bộ email: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    });
                })
                .addOnFailureListener(e ->
                        android.util.Log.e(
                                "EditProfile",
                                "Không thể tải lại tài khoản Firebase",
                                e
                        )
                );
    }

    private void showReauthenticationDialog(
            FirebaseUser user,
            String newEmail
    ) {
        EditText passwordInput = new EditText(this);

        passwordInput.setSingleLine(true);
        passwordInput.setHint("Mật khẩu hiện tại");
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        new AlertDialog.Builder(this)
                .setTitle("Xác thực tài khoản")
                .setMessage(
                        "Nhập mật khẩu hiện tại để tiếp tục đổi email."
                )
                .setView(passwordInput)
                .setCancelable(false)
                .setNegativeButton("Hủy", (dialog, which) -> {
                    btnSaveProfile.setEnabled(true);
                })
                .setPositiveButton("Xác thực", (dialog, which) -> {
                    String password = passwordInput.getText() == null
                            ? ""
                            : passwordInput.getText().toString();

                    if (password.isEmpty()) {
                        btnSaveProfile.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Vui lòng nhập mật khẩu.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String currentEmail = user.getEmail();

                    if (currentEmail == null || currentEmail.isEmpty()) {
                        btnSaveProfile.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Không tìm thấy email tài khoản hiện tại.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    com.google.firebase.auth.AuthCredential credential =
                            EmailAuthProvider.getCredential(
                                    currentEmail,
                                    password
                            );

                    user.reauthenticate(credential)
                            .addOnSuccessListener(unused -> {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                user.updateEmail(newEmail)
                                        .addOnSuccessListener(updateResult -> {
                                            if (isFinishing() || isDestroyed()) {
                                                return;
                                            }

                                            // Đồng bộ email mới sang Firestore và emailIndex
                                            syncVerifiedEmail(user);

                                            btnSaveProfile.setEnabled(true);

                                            Toast.makeText(
                                                    this,
                                                    "Đã đổi email. Đang đồng bộ hồ sơ.",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            if (isFinishing() || isDestroyed()) {
                                                return;
                                            }

                                            btnSaveProfile.setEnabled(true);

                                            Toast.makeText(
                                                    this,
                                                    "Không thể đổi email: " + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                btnSaveProfile.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        "Xác thực thất bại. Hãy kiểm tra mật khẩu hiện tại.",
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .show();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null
                ? ""
                : editText.getText().toString().trim();
    }

}
