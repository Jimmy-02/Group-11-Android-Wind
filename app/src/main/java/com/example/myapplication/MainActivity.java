package com.example.myapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.content.Intent;
import android.widget.Toast;

import com.example.myapplication.authentication.login.LoginActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.example.myapplication.account.AccountActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Fragment homeFragment;
    private Fragment qnaFragment;
    private Fragment accountFragment;
    private boolean isCheckingAccount = false;
    private ListenerRegistration accountListener;
    private boolean isRedirectingToLogin = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();

        homeFragment = fm.findFragmentByTag("HOME");
        qnaFragment = fm.findFragmentByTag("QNA");
        accountFragment = fm.findFragmentByTag("ACCOUNT");

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }

        if (qnaFragment == null) {
            qnaFragment = new QnaFragment();
        }

        if (accountFragment == null) {
            accountFragment = new AccountActivity();
        }

        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.content_container, homeFragment, "HOME")
                    .add(R.id.content_container, qnaFragment, "QNA")
                    .hide(qnaFragment)
                    .add(R.id.content_container, accountFragment, "ACCOUNT")
                    .hide(accountFragment)
                    .commit();
        }

        BottomNavigationView bottomNavigation =
                findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                showFragment(homeFragment);
            } else if (id == R.id.nav_qna) {
                showFragment(qnaFragment);
            } else if (id == R.id.nav_account) {
                showFragment(accountFragment);
            } else {
                return false;
            }

            return true;
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        checkAccountStatus();
    }
    @Override
    protected void onStart() {
        super.onStart();
        startAccountStatusListener();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (accountListener != null) {
            accountListener.remove();
            accountListener = null;
        }
    }
    private void checkAccountStatus() {
        if (isCheckingAccount) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            redirectToLogin(null);
            return;
        }

        isCheckingAccount = true;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(this, task -> {
                    isCheckingAccount = false;

                    if (isFinishing() || isDestroyed()) return;

                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        redirectToLogin("Không thể xác minh tài khoản. Vui lòng đăng nhập lại.");
                        return;
                    }

                    String status = task.getResult().getString("status");

                    if (status == null || !"ACTIVE".equalsIgnoreCase(status.trim())) {
                        redirectToLogin("Tài khoản của bạn đã bị khóa hoặc không được phép truy cập.");
                    }
                });
    }
    private void startAccountStatusListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            redirectToLogin(null);
            return;
        }

        if (accountListener != null) {
            accountListener.remove();
        }

        accountListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .addSnapshotListener((document, error) -> {
                    if (isFinishing() || isDestroyed() || isRedirectingToLogin) {
                        return;
                    }

                    if (error != null) {
                        Toast.makeText(this, "Không thể kiểm tra trạng thái tài khoản.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (document == null || !document.exists()) {
                        redirectToLogin("Không tìm thấy hồ sơ người dùng.");
                        return;
                    }

                    String status = document.getString("status");
                    String role = document.getString("role");

                    if (!"ACTIVE".equalsIgnoreCase(status == null ? "" : status.trim())) {
                        redirectToLogin("Tài khoản đã bị khóa hoặc không được phép truy cập.");
                        return;
                    }

                    if (!"USER".equalsIgnoreCase(role == null ? "" : role.trim())) {
                        redirectToLogin("Tài khoản không được phép truy cập màn hình này.");
                    }
                });
    }
    private void redirectToLogin(String message) {
        if (isRedirectingToLogin) {
            return;
        }

        isRedirectingToLogin = true;

        if (accountListener != null) {
            accountListener.remove();
            accountListener = null;
        }

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        finish();
    }
    private void showFragment(Fragment target) {
        FragmentManager fm = getSupportFragmentManager();

        fm.beginTransaction()
                .hide(homeFragment)
                .hide(qnaFragment)
                .hide(accountFragment)
                .show(target)
                .commit();
    }
}
