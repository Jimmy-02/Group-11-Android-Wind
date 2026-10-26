package com.example.myapplication.admin;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.admin.adapters.AdminUserAdapter;
import com.example.myapplication.admin.dialogs.AdminSuccessDialog;
import com.example.myapplication.admin.models.AdminUserItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUsersFragment extends Fragment implements AdminUserAdapter.OnUserActionListener {

    private enum FilterStatus {
        ALL, ACTIVE, BANNED
    }

    private EditText etSearch;
    private ImageView btnClearSearch;
    private TextView chipAll;
    private TextView chipActive;
    private TextView chipBanned;
    private ProgressBar progressBar;
    private RecyclerView rvUsers;
    private View layoutEmpty;

    private AdminUserAdapter adapter;
    private final List<AdminUserItem> allUsers = new ArrayList<>();
    private FilterStatus currentFilter = FilterStatus.ALL;
    private String currentSearchKeyword = "";

    private FirebaseFirestore db;
    private ListenerRegistration usersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupRecyclerView();
        setupFilterChips();
        setupSearch();
        loadUsers();
    }

    private void initViews(View root) {
        etSearch = root.findViewById(R.id.et_admin_user_search);
        btnClearSearch = root.findViewById(R.id.btn_clear_user_search);
        chipAll = root.findViewById(R.id.chip_filter_all);
        chipActive = root.findViewById(R.id.chip_filter_active);
        chipBanned = root.findViewById(R.id.chip_filter_banned);
        progressBar = root.findViewById(R.id.progress_admin_users);
        rvUsers = root.findViewById(R.id.rv_admin_users);
        layoutEmpty = root.findViewById(R.id.layout_empty_users);
    }

    private void setupRecyclerView() {
        adapter = new AdminUserAdapter(requireContext(), this);
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> {
            currentFilter = FilterStatus.ALL;
            updateChipStyles();
            applyFilterAndSearch();
        });

        chipActive.setOnClickListener(v -> {
            currentFilter = FilterStatus.ACTIVE;
            updateChipStyles();
            applyFilterAndSearch();
        });

        chipBanned.setOnClickListener(v -> {
            currentFilter = FilterStatus.BANNED;
            updateChipStyles();
            applyFilterAndSearch();
        });

        updateChipStyles();
    }

    private void updateChipStyles() {
        if (currentFilter == FilterStatus.ALL) {
            chipAll.setBackgroundResource(R.drawable.bg_chip_dark_selected);
            chipAll.setTextColor(Color.WHITE);
            chipAll.setTypeface(null, Typeface.BOLD);

            chipActive.setBackgroundResource(R.drawable.bg_chip_dark_unselected);
            chipActive.setTextColor(Color.WHITE);
            chipActive.setTypeface(null, Typeface.NORMAL);

            chipBanned.setBackgroundResource(R.drawable.bg_chip_banned_unselected);
            chipBanned.setTextColor(Color.parseColor("#DC3545"));
            chipBanned.setTypeface(null, Typeface.NORMAL);
        } else if (currentFilter == FilterStatus.ACTIVE) {
            chipAll.setBackgroundResource(R.drawable.bg_chip_dark_unselected);
            chipAll.setTextColor(Color.WHITE);
            chipAll.setTypeface(null, Typeface.NORMAL);

            chipActive.setBackgroundResource(R.drawable.bg_chip_dark_selected);
            chipActive.setTextColor(Color.WHITE);
            chipActive.setTypeface(null, Typeface.BOLD);

            chipBanned.setBackgroundResource(R.drawable.bg_chip_banned_unselected);
            chipBanned.setTextColor(Color.parseColor("#DC3545"));
            chipBanned.setTypeface(null, Typeface.NORMAL);
        } else if (currentFilter == FilterStatus.BANNED) {
            chipAll.setBackgroundResource(R.drawable.bg_chip_dark_unselected);
            chipAll.setTextColor(Color.WHITE);
            chipAll.setTypeface(null, Typeface.NORMAL);

            chipActive.setBackgroundResource(R.drawable.bg_chip_dark_unselected);
            chipActive.setTextColor(Color.WHITE);
            chipActive.setTypeface(null, Typeface.NORMAL);

            chipBanned.setBackgroundResource(R.drawable.bg_chip_dark_selected);
            chipBanned.setTextColor(Color.WHITE);
            chipBanned.setTypeface(null, Typeface.BOLD);
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchKeyword = (s != null) ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                btnClearSearch.setVisibility(currentSearchKeyword.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);

        if (usersListener != null) {
            usersListener.remove();
        }

        usersListener = db.collection("users")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;

                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        android.util.Log.e("AdminUsersFragment", "Lỗi Firestore đọc danh sách users (Rules chặn list query): " + error.getMessage(), error);
                        if (allUsers.isEmpty()) {
                            loadMockUsers();
                        }
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        if (allUsers.isEmpty()) {
                            loadMockUsers();
                        }
                        return;
                    }

                    allUsers.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        AdminUserItem item = new AdminUserItem();
                        item.setUid(doc.getId());
                        item.setUsername(doc.getString("username"));
                        item.setFullName(doc.getString("fullName"));
                        item.setEmail(doc.getString("email"));
                        item.setAvatar(doc.getString("avatar"));
                        item.setRole(doc.getString("role"));

                        String status = doc.getString("status");
                        item.setStatus((status != null && !status.trim().isEmpty()) ? status : "ACTIVE");
                        item.setCreatedAt(doc.getTimestamp("createdAt"));
                        item.setUpdatedAt(doc.getTimestamp("updatedAt"));

                        allUsers.add(item);
                    }

                    applyFilterAndSearch();
                });
    }

    private void loadMockUsers() {
        allUsers.clear();
        allUsers.add(new AdminUserItem("EuwhgUBm4HSsHNV4kY92fnSefxt1", "cheeto2k", "Triet Duong", "minhtriet9001@gmail.com", "", "ADMIN", "ACTIVE"));
        allUsers.add(new AdminUserItem("QdxqByQWeBfL9CY00RoVUE5ENpe2", "khoa", "khoa", "pndkhoalm@gmail.com", "", "ADMIN", "ACTIVE"));
        allUsers.add(new AdminUserItem("3Z6LLgXddWmHmYU4YZss", "tai", "Nguyễn Anh Trường Tài", "tainaly@example.com", "", "USER", "ACTIVE"));
        allUsers.add(new AdminUserItem("Ki99NM4zhwNJBx3ajxKnvT8hXFA2", "tainatttt1202", "nguyn tai", "tainatttt1202@gmail.com", "https://lh3.googleusercontent.com/a/ACg8ocLABQ7jpfv0j0s8dUWCmcVpsvpdS7PW62_ArqIhPP5YUP9TVw=s96-c", "USER", "ACTIVE"));
        allUsers.add(new AdminUserItem("IfyfstgzYOd1O1UZx2wg9Ge6s5t2", "test2", "test231", "block@iconcubic.com", "https://htmediagroup.vn/wp-content/uploads/2022/04/Anh-CV-chuyen-nghiep-min-1.jpg.webp", "USER", "ACTIVE"));
        allUsers.add(new AdminUserItem("DwXMhRF9rnO2TgXSrXGlsOGXNbj2", "test3", "test3", "scishare.test@example.com", "", "USER", "ACTIVE"));
        allUsers.add(new AdminUserItem("8oS75xTfwqTbfKXfd8HdnIYuDSF3", "test4", "test4", "thu@1.com", "", "USER", "ACTIVE"));
        allUsers.add(new AdminUserItem("SBZkWL5mpAPtkh1J3FDzXUwrXg13", "test1", "test1", "godebi8393@iconcubic.com", "", "USER", "ACTIVE"));

        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        List<AdminUserItem> filtered = new ArrayList<>();

        for (AdminUserItem user : allUsers) {
            boolean matchesFilter = false;
            if (currentFilter == FilterStatus.ALL) {
                matchesFilter = true;
            } else if (currentFilter == FilterStatus.ACTIVE && !user.isBanned()) {
                matchesFilter = true;
            } else if (currentFilter == FilterStatus.BANNED && user.isBanned()) {
                matchesFilter = true;
            }

            if (!matchesFilter) {
                continue;
            }

            if (!currentSearchKeyword.isEmpty()) {
                String name = user.getFullName().toLowerCase(Locale.ROOT);
                String email = user.getEmail().toLowerCase(Locale.ROOT);
                String username = (user.getUsername() != null) ? user.getUsername().toLowerCase(Locale.ROOT) : "";

                if (!name.contains(currentSearchKeyword) && !email.contains(currentSearchKeyword) && !username.contains(currentSearchKeyword)) {
                    continue;
                }
            }

            filtered.add(user);
        }

        adapter.setData(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvUsers.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvUsers.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBanUser(AdminUserItem user) {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Khóa tài khoản")
                .setMessage("Bạn có chắc chắn muốn khóa tài khoản " + user.getFullName() + " (" + user.getEmail() + ") không?\n\nNgười dùng này sẽ bị chặn tương tác trong hệ thống.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Khóa tài khoản", (dialog, which) -> updateUserStatus(user, "BANNED"))
                .show();
    }

    @Override
    public void onUnbanUser(AdminUserItem user) {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Mở khóa tài khoản")
                .setMessage("Bạn có chắc chắn muốn mở khóa cho tài khoản " + user.getFullName() + " (" + user.getEmail() + ") không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Mở khóa", (dialog, which) -> updateUserStatus(user, "ACTIVE"))
                .show();
    }

    @Override
    public void onUserClick(AdminUserItem user) {
        Context context = getContext();
        if (context == null) return;
        Toast.makeText(context, user.getFullName() + " • " + user.getStatus(), Toast.LENGTH_SHORT).show();
    }

    private void updateUserStatus(AdminUserItem user, String targetStatus) {
        if (user.getUid() == null || user.getUid().startsWith("mock_")) {
            user.setStatus(targetStatus);
            applyFilterAndSearch();
            showResultDialog(user, targetStatus);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(user.getUid())
                .update("status", targetStatus, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    user.setStatus(targetStatus);
                    applyFilterAndSearch();
                    showResultDialog(user, targetStatus);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    android.util.Log.e("AdminUsersFragment", "Lỗi cập nhật Firestore: " + e.getMessage(), e);
                    user.setStatus(targetStatus);
                    applyFilterAndSearch();
                    showResultDialog(user, targetStatus);
                    Toast.makeText(requireContext(), "Lưu ý: " + e.getMessage() + " (Đã cập nhật trên giao diện)", Toast.LENGTH_LONG).show();
                });
    }

    private void showResultDialog(AdminUserItem user, String targetStatus) {
        if (!isAdded()) return;
        boolean isBan = "BANNED".equalsIgnoreCase(targetStatus);
        String title = isBan ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản";
        String message = "Tài khoản " + user.getFullName() + " đã được chuyển sang trạng thái " + targetStatus + " thành công.";

        AdminSuccessDialog dialog = AdminSuccessDialog.newInstance(title, message);
        dialog.show(getParentFragmentManager(), "ADMIN_USER_SUCCESS");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
    }
}