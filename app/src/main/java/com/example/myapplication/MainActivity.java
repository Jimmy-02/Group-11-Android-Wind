package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.myapplication.account.AccountActivity;
import com.example.myapplication.authentication.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Fragment homeFragment;
    private Fragment discussionFragment;
    private Fragment qnaFragment;
    private Fragment accountFragment;

    private boolean isCheckingAccount = false;
    private boolean isRedirectingToLogin = false;
    private ListenerRegistration accountListener;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvToolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);

        findViewById(R.id.btn_open_drawer).setOnClickListener(v -> drawerLayout.openDrawer(navigationView));

        FragmentManager fm = getSupportFragmentManager();

        homeFragment = fm.findFragmentByTag("HOME");
        discussionFragment = fm.findFragmentByTag("DISCUSSION");
        qnaFragment = fm.findFragmentByTag("QNA");
        accountFragment = fm.findFragmentByTag("ACCOUNT");

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }

        if (discussionFragment == null) {
            discussionFragment = new DiscussionFragment();
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
                    .add(R.id.content_container, discussionFragment, "DISCUSSION")
                    .hide(discussionFragment)
                    .add(R.id.content_container, qnaFragment, "QNA")
                    .hide(qnaFragment)
                    .add(R.id.content_container, accountFragment, "ACCOUNT")
                    .hide(accountFragment)
                    .commit();
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                showFragment(homeFragment, "Home");
            } else if (id == R.id.nav_discuss) {
                showFragment(discussionFragment, "Discuss");
            } else if (id == R.id.nav_qna) {
                showFragment(qnaFragment, "Q&A");
            } else if (id == R.id.nav_account) {
                showFragment(accountFragment, "Account");
            } else {
                return false;
            }

            return true;
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.drawer_home) {
                bottomNavigation.setSelectedItemId(R.id.nav_home);
            } else if (id == R.id.drawer_discuss) {
                bottomNavigation.setSelectedItemId(R.id.nav_discuss);
            } else if (id == R.id.drawer_qna) {
                bottomNavigation.setSelectedItemId(R.id.nav_qna);
            } else if (id == R.id.drawer_account) {
                bottomNavigation.setSelectedItemId(R.id.nav_account);
            } else if (id == R.id.drawer_admin) {
                Intent intent = new Intent(MainActivity.this, com.example.myapplication.admin.AdminActivity.class);
                startActivity(intent);
            } else if (id == R.id.drawer_about) {
                showAboutDialog();
            } else if (id == R.id.drawer_logout) {
                showLogoutConfirmation();
            } else {
                return false;
            }

            drawerLayout.closeDrawer(navigationView);
            return true;
        });

        setupDrawerHeader();

        int selectedItem = savedInstanceState != null
                ? savedInstanceState.getInt("selected_nav_item", R.id.nav_home)
                : R.id.nav_home;

        bottomNavigation.setSelectedItemId(selectedItem);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        outState.putInt("selected_nav_item", bottomNavigation.getSelectedItemId());
    }

    private void setupDrawerHeader() {
        TextView tvEmail = navigationView.getHeaderView(0).findViewById(R.id.tv_drawer_email);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getEmail() != null) {
            tvEmail.setText(user.getEmail());
        }
    }

    private void showFragment(Fragment target, String title) {
        FragmentManager fm = getSupportFragmentManager();

        fm.beginTransaction()
                .hide(homeFragment)
                .hide(discussionFragment)
                .hide(qnaFragment)
                .hide(accountFragment)
                .show(target)
                .commit();

        findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        tvToolbarTitle.setText(title);

        if (target == homeFragment) {
            navigationView.setCheckedItem(R.id.drawer_home);
        } else if (target == discussionFragment) {
            navigationView.setCheckedItem(R.id.drawer_discuss);
        } else if (target == qnaFragment) {
            navigationView.setCheckedItem(R.id.drawer_qna);
        } else if (target == accountFragment) {
            navigationView.setCheckedItem(R.id.drawer_account);
        }

        if (target == accountFragment && drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Đăng xuất", (dialog, which) -> logoutAndNavigateToLogin())
                .show();
    }

    public void logoutAndNavigateToLogin() {
        if (isRedirectingToLogin || isFinishing() || isDestroyed()) {
            return;
        }

        isRedirectingToLogin = true;

        removeAccountStatusListener();

        if (homeFragment instanceof HomeFragment) {
            ((HomeFragment) homeFragment).removeArticleListener();
        }

        if (accountFragment instanceof AccountActivity) {
            ((AccountActivity) accountFragment).removeFirestoreListenersForLogout();
        }

        if (qnaFragment instanceof QnaFragment) {
            ((QnaFragment) qnaFragment).removeQuestionsListener();
        }

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
        removeAccountStatusListener();
        super.onStop();
    }

    private void removeAccountStatusListener() {
        if (accountListener != null) {
            accountListener.remove();
            accountListener = null;
        }
    }

    private void updateAdminDrawerVisibility(String role) {
        if (navigationView != null) {
            android.view.MenuItem adminItem = navigationView.getMenu().findItem(R.id.drawer_admin);

            if (adminItem != null) {
                boolean isAdmin = "ADMIN".equalsIgnoreCase(role != null ? role.trim() : "");
                adminItem.setVisible(isAdmin);
            }
        }
    }

    private void checkAccountStatus() {
        if (isCheckingAccount || isRedirectingToLogin) {
            return;
        }

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

                    if (isFinishing() || isDestroyed() || isRedirectingToLogin) {
                        return;
                    }

                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        redirectToLogin("Không thể xác minh tài khoản. Vui lòng đăng nhập lại.");
                        return;
                    }

                    DocumentSnapshot document = task.getResult();
                    String status = document.getString("status");
                    String role = document.getString("role");

                    updateAdminDrawerVisibility(role);

                    if (!"ACTIVE".equalsIgnoreCase(status == null ? "" : status.trim())) {
                        redirectToLogin("Tài khoản của bạn đã bị khóa hoặc không được phép truy cập.");
                        return;
                    }

                    String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);

                    if (!"USER".equals(normalizedRole) && !"ADMIN".equals(normalizedRole)) {
                        redirectToLogin("Tài khoản không được phép truy cập màn hình này.");
                    }
                });
    }

    private void startAccountStatusListener() {
        removeAccountStatusListener();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || isRedirectingToLogin) {
            return;
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

                    updateAdminDrawerVisibility(role);

                    if (!"ACTIVE".equalsIgnoreCase(status == null ? "" : status.trim())) {
                        redirectToLogin("Tài khoản đã bị khóa hoặc không được phép truy cập.");
                        return;
                    }

                    String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);

                    if (!"USER".equals(normalizedRole) && !"ADMIN".equals(normalizedRole)) {
                        redirectToLogin("Tài khoản không được phép truy cập màn hình này.");
                    }
                });
    }

    private void showAboutDialog() {
        String members = "Nguyễn Anh Trường Tài\n"
                + "Phan Nhựt Đăng Khoa\n"
                + "Dương Minh Triết\n"
                + "Nguyễn Thị Minh Nguyệt";

        new MaterialAlertDialogBuilder(this)
                .setTitle("About SciShare")
                .setMessage(members)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void redirectToLogin(String message) {
        if (isRedirectingToLogin) {
            return;
        }

        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        logoutAndNavigateToLogin();
    }
}