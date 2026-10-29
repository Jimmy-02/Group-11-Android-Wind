package com.example.myapplication.admin;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.myapplication.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    private Fragment articlesFragment;
    private Fragment usersFragment;
    private Fragment discussionsFragment;
    private Fragment qnaFragment;
    private static final String KEY_SELECTED_TAB = "admin_selected_tab";
    private int currentTabId = R.id.nav_admin_articles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        setupStatusBar();
        verifyAdminAccess();

        findViewById(R.id.btn_admin_back).setOnClickListener(v -> finish());

        FragmentManager fm = getSupportFragmentManager();

        articlesFragment = fm.findFragmentByTag("ADMIN_ARTICLES");
        usersFragment = fm.findFragmentByTag("ADMIN_USERS");
        discussionsFragment = fm.findFragmentByTag("ADMIN_DISCUSSIONS");
        qnaFragment = fm.findFragmentByTag("ADMIN_QNA");

        if (articlesFragment == null) articlesFragment = new AdminArticlesFragment();
        if (usersFragment == null) usersFragment = new AdminUsersFragment();
        if (discussionsFragment == null) discussionsFragment = new AdminDiscussionsFragment();
        if (qnaFragment == null) qnaFragment = new AdminQnaFragment();

        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.admin_content_container, articlesFragment, "ADMIN_ARTICLES")
                    .add(R.id.admin_content_container, usersFragment, "ADMIN_USERS")
                    .hide(usersFragment)
                    .add(R.id.admin_content_container, discussionsFragment, "ADMIN_DISCUSSIONS")
                    .hide(discussionsFragment)
                    .add(R.id.admin_content_container, qnaFragment, "ADMIN_QNA")
                    .hide(qnaFragment)
                    .commit();
        } else {
            currentTabId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_admin_articles);
        }

        NavigationBarView navBar = findViewById(R.id.admin_bottom_navigation);
        if (navBar != null) {
            navBar.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                currentTabId = id;
                if (id == R.id.nav_admin_articles) {
                    showTab(articlesFragment);
                    return true;
                } else if (id == R.id.nav_admin_users) {
                    showTab(usersFragment);
                    return true;
                } else if (id == R.id.nav_admin_discussions) {
                    showTab(discussionsFragment);
                    return true;
                } else if (id == R.id.nav_admin_qna) {
                    showTab(qnaFragment);
                    return true;
                }
                return false;
            });

            if (savedInstanceState != null) {
                navBar.setSelectedItemId(currentTabId);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, currentTabId);
    }

    private void setupStatusBar() {
        TypedValue typedValue = new TypedValue();
        int color = Color.parseColor("#6750A4");
        if (getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            color = typedValue.data;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color);
        }

        View statusBarSpacer = findViewById(R.id.status_bar_spacer);
        statusBarSpacer.setBackgroundColor(color);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.LayoutParams params = statusBarSpacer.getLayoutParams();
            params.height = insets.top;
            statusBarSpacer.setLayoutParams(params);
            return windowInsets;
        });
    }

    private void showTab(Fragment target) {
        getSupportFragmentManager().beginTransaction()
                .hide(articlesFragment)
                .hide(usersFragment)
                .hide(discussionsFragment)
                .hide(qnaFragment)
                .show(target)
                .commit();
    }

    private void verifyAdminAccess() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this, "Không tìm thấy thông tin tài khoản.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String role = doc.getString("role");
                    if (!"ADMIN".equalsIgnoreCase(role != null ? role.trim() : "")) {
                        Toast.makeText(this, "Bạn không có quyền truy cập Không gian Quản trị.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Không thể xác minh quyền quản trị.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
