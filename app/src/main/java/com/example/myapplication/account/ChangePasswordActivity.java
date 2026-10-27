
package com.example.myapplication.account;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.myapplication.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
public class ChangePasswordActivity extends AppCompatActivity {

    private EditText edtOldPassword;
    private EditText edtNewPassword;
    private EditText edtConfirmPassword;
    private Button btnChangePassword;

    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập lại",
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            return;
        }

        boolean hasGoogleProvider = false;
        boolean hasPasswordProvider = false;

        for (UserInfo provider : currentUser.getProviderData()) {
            if ("google.com".equals(provider.getProviderId())) {
                hasGoogleProvider = true;
            }

            if ("password".equals(provider.getProviderId())) {
                hasPasswordProvider = true;
            }
        }

        // Tài khoản chỉ đăng nhập bằng Google
        if (hasGoogleProvider && !hasPasswordProvider) {
            showGoogleMessage();
            return;
        }

        // Không có phương thức đăng nhập bằng mật khẩu
        if (!hasPasswordProvider) {
            new AlertDialog.Builder(this)
                    .setTitle("Không thể đổi mật khẩu")
                    .setMessage(
                            "Phương thức đăng nhập hiện tại chưa hỗ trợ " +
                                    "đổi mật khẩu tại SciShare."
                    )
                    .setPositiveButton("Đóng", (dialog, which) -> finish())
                    .setOnCancelListener(dialog -> finish())
                    .show();
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_change_password);
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        insetsController.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.root);
        View statusBarSpacer = findViewById(R.id.status_bar_spacer);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            ViewGroup.LayoutParams params =
                    statusBarSpacer.getLayoutParams();

            params.height = bars.top;
            statusBarSpacer.setLayoutParams(params);

            v.setPadding(
                    bars.left,
                    0,
                    bars.right,
                    bars.bottom
            );

            return WindowInsetsCompat.CONSUMED;
        });

        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Nút Back
        findViewById(R.id.layoutBackChangePassword)
                .setOnClickListener(v -> finish());

        // Nút đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void showGoogleMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Tài khoản Google")
                .setMessage(
                        "Tài khoản SciShare của bạn sử dụng Google Sign-In. " +
                                "Để đổi mật khẩu, vui lòng thay đổi mật khẩu trong " +
                                "tài khoản Google của bạn.\n\n" +
                                "Bạn có thể tiếp tục đăng nhập SciShare bằng Google " +
                                "sau khi đổi mật khẩu."
                )
                .setPositiveButton("Đã hiểu", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void changePassword() {
        String oldPassword =
                edtOldPassword.getText().toString();

        String newPassword =
                edtNewPassword.getText().toString();

        String confirmPassword =
                edtConfirmPassword.getText().toString();

        if (oldPassword.isEmpty()
                || newPassword.isEmpty()
                || confirmPassword.isEmpty()) {
            Toast.makeText(
                    this,
                    "Vui lòng nhập đầy đủ thông tin",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (newPassword.length() < 6) {
            edtNewPassword.setError(
                    "Mật khẩu mới phải có ít nhất 6 ký tự"
            );
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            edtConfirmPassword.setError(
                    "Mật khẩu xác nhận không khớp"
            );
            return;
        }

        if (currentUser.getEmail() == null) {
            Toast.makeText(
                    this,
                    "Không tìm thấy email của tài khoản",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnChangePassword.setEnabled(false);

        AuthCredential credential =
                EmailAuthProvider.getCredential(
                        currentUser.getEmail(),
                        oldPassword
                );

        // Xác thực lại bằng mật khẩu hiện tại
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (!reauthTask.isSuccessful()) {
                        btnChangePassword.setEnabled(true);

                        edtOldPassword.setError(
                                "Mật khẩu hiện tại không đúng"
                        );
                        return;
                    }

                    // Xác thực thành công, cập nhật mật khẩu
                    currentUser.updatePassword(newPassword)
                            .addOnCompleteListener(updateTask -> {
                                btnChangePassword.setEnabled(true);

                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(
                                            this,
                                            "Đổi mật khẩu thành công",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    finish();
                                } else {
                                    Toast.makeText(
                                            this,
                                            "Không thể đổi mật khẩu. " +
                                                    "Vui lòng kiểm tra yêu cầu " +
                                                    "mật khẩu của Firebase và thử lại.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                });
    }
}
