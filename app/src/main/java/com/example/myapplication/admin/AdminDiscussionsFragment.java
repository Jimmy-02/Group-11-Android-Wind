package com.example.myapplication.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.admin.adapters.AdminReportAdapter;
import com.example.myapplication.admin.models.AdminReportItem;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDiscussionsFragment extends Fragment {

    private TabLayout tabLayout;
    private EditText etSearch;
    private View btnClearSearch;
    private TextView chipAll, chipPending, chipResolved, chipDismissed;
    private RecyclerView rvReports;
    private ProgressBar pbLoading;
    private View layoutEmptyState;

    private AdminReportAdapter adapter;
    private final List<AdminReportItem> allDiscussionItems = new ArrayList<>();
    private final List<AdminReportItem> allCommentItems = new ArrayList<>();
    private ListenerRegistration discussionReportsListener;
    private ListenerRegistration commentReportsListener;

    private int currentTab = 0; // 0: Bài thảo luận, 1: Bình luận
    private String currentFilter = "ALL"; // ALL, PENDING, REPORTED_CONTENT_DELETED, DISMISSED
    private String currentSearchKeyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_discussions, container, false);

        initViews(view);
        setupTabs();
        setupFilterChips();
        setupSearch();
        setupRecyclerView();
        loadFirestoreReports();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFirestoreReports();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_admin_discussions);
        etSearch = view.findViewById(R.id.et_admin_search_discussions);
        btnClearSearch = view.findViewById(R.id.btn_clear_search_discussions);
        chipAll = view.findViewById(R.id.chip_filter_all_discussions);
        chipPending = view.findViewById(R.id.chip_filter_pending_discussions);
        chipResolved = view.findViewById(R.id.chip_filter_resolved_discussions);
        chipDismissed = view.findViewById(R.id.chip_filter_dismissed_discussions);
        rvReports = view.findViewById(R.id.rv_admin_discussions);
        pbLoading = view.findViewById(R.id.pb_admin_discussions_loading);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state_discussions);
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Bài thảo luận"));
        tabLayout.addTab(tabLayout.newTab().setText("Bình luận"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                applyFilterAndSearch();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFilterChips() {
        View.OnClickListener chipListener = v -> {
            int id = v.getId();
            if (id == R.id.chip_filter_all_discussions) {
                currentFilter = "ALL";
            } else if (id == R.id.chip_filter_pending_discussions) {
                currentFilter = "PENDING";
            } else if (id == R.id.chip_filter_resolved_discussions) {
                currentFilter = "REPORTED_CONTENT_DELETED";
            } else if (id == R.id.chip_filter_dismissed_discussions) {
                currentFilter = "DISMISSED";
            }
            updateChipStyles();
            applyFilterAndSearch();
        };

        chipAll.setOnClickListener(chipListener);
        chipPending.setOnClickListener(chipListener);
        chipResolved.setOnClickListener(chipListener);
        chipDismissed.setOnClickListener(chipListener);

        updateChipStyles();
    }

    private void updateChipStyles() {
        resetChip(chipAll, "ALL".equals(currentFilter));
        resetChip(chipPending, "PENDING".equals(currentFilter));
        resetChip(chipResolved, "REPORTED_CONTENT_DELETED".equals(currentFilter));
        resetChip(chipDismissed, "DISMISSED".equals(currentFilter));
    }

    private void resetChip(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_filter_chip_unselected);
            chip.setTextColor(Color.parseColor("#475569"));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchKeyword = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                btnClearSearch.setVisibility(currentSearchKeyword.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    private void setupRecyclerView() {
        rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminReportAdapter(item -> {
            Intent intent = new Intent(requireContext(), AdminReportDetailActivity.class);
            intent.putExtra(AdminReportDetailActivity.EXTRA_REPORT_ID, item.getId());
            intent.putExtra(AdminReportDetailActivity.EXTRA_TARGET_ID, item.getContentId());
            intent.putExtra(AdminReportDetailActivity.EXTRA_TARGET_TYPE, item.getContentType());
            intent.putExtra(AdminReportDetailActivity.EXTRA_TITLE, item.getTitle());
            intent.putExtra(AdminReportDetailActivity.EXTRA_CONTENT, item.getContent());
            intent.putExtra(AdminReportDetailActivity.EXTRA_PARENT_QUESTION, item.getParentQuestionTitle());
            intent.putExtra(AdminReportDetailActivity.EXTRA_STATUS, item.getStatus());
            intent.putExtra(AdminReportDetailActivity.EXTRA_REPORT_COUNT, item.getReportCount());
            intent.putExtra(AdminReportDetailActivity.EXTRA_AUTHOR_NAME, item.getAuthorName());
            intent.putExtra(AdminReportDetailActivity.EXTRA_AUTHOR_AVATAR, item.getAuthorAvatar());
            intent.putExtra(AdminReportDetailActivity.EXTRA_POST_TIME, item.getTimeAgo());
            String tag = (item.getTags() != null && !item.getTags().isEmpty()) ? android.text.TextUtils.join(" • ", item.getTags()) : "";
            intent.putExtra(AdminReportDetailActivity.EXTRA_TAG, tag);
            intent.putExtra(AdminReportDetailActivity.EXTRA_IMAGE_URL, item.getImageUrl());
            startActivity(intent);
        });
        rvReports.setAdapter(adapter);
    }

    private void loadFirestoreReports() {
        if (discussionReportsListener != null) {
            discussionReportsListener.remove();
            discussionReportsListener = null;
        }
        if (commentReportsListener != null) {
            commentReportsListener.remove();
            commentReportsListener = null;
        }

        pbLoading.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Tải báo cáo Thảo luận (discussionReports) realtime
        discussionReportsListener = db.collection("discussionReports")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    if (error != null || snapshots == null || snapshots.isEmpty()) {
                        if (allDiscussionItems.isEmpty()) {
                            createMockDiscussions();
                        }
                        if (currentTab == 0) {
                            pbLoading.setVisibility(View.GONE);
                            applyFilterAndSearch();
                        }
                        return;
                    }
                    groupDiscussionReports(snapshots.getDocuments());
                });

        // 2. Tải báo cáo Bình luận (commentReports) realtime
        commentReportsListener = db.collection("commentReports")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    if (error != null || snapshots == null || snapshots.isEmpty()) {
                        if (allCommentItems.isEmpty()) {
                            createMockComments();
                        }
                        if (currentTab == 1) {
                            pbLoading.setVisibility(View.GONE);
                            applyFilterAndSearch();
                        }
                        return;
                    }
                    groupCommentReports(snapshots.getDocuments());
                });
    }

    private void groupDiscussionReports(List<DocumentSnapshot> docs) {
        allDiscussionItems.clear();
        Map<String, List<DocumentSnapshot>> map = new HashMap<>();
        for (DocumentSnapshot doc : docs) {
            String dId = doc.getString("discussionId");
            if (dId == null || dId.isEmpty()) continue;
            if (!map.containsKey(dId)) map.put(dId, new ArrayList<>());
            map.get(dId).add(doc);
        }

        if (map.isEmpty()) {
            if (currentTab == 0) {
                pbLoading.setVisibility(View.GONE);
                applyFilterAndSearch();
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (Map.Entry<String, List<DocumentSnapshot>> entry : map.entrySet()) {
            String discussionId = entry.getKey();
            List<DocumentSnapshot> reports = entry.getValue();
            db.collection("discussions").document(discussionId).get().addOnSuccessListener(dDoc -> {
                if (!isAdded()) return;
                String title = dDoc.exists() ? dDoc.getString("title") : "Bài thảo luận vi phạm";
                String category = dDoc.exists() ? dDoc.getString("category") : "Chung";
                String authorName = dDoc.exists() ? dDoc.getString("authorName") : null;
                String authorAvatar = dDoc.exists() ? dDoc.getString("authorAvatar") : null;
                String imageUrl = dDoc.exists() ? dDoc.getString("imageUrl") : null;
                String userId = dDoc.exists() ? dDoc.getString("userId") : null;
                String dStatus = dDoc.exists() ? dDoc.getString("status") : null;

                String statusToUse;
                if ("DELETED".equalsIgnoreCase(dStatus) || (dDoc.exists() && dDoc.get("deletedAt") != null)) {
                    statusToUse = "REPORTED_CONTENT_DELETED";
                } else {
                    boolean hasPending = false;
                    boolean hasDeleted = false;
                    boolean allDismissed = true;
                    for (DocumentSnapshot rDoc : reports) {
                        String s = rDoc.getString("status");
                        if ("PENDING".equalsIgnoreCase(s) || s == null || s.isEmpty()) {
                            hasPending = true;
                            allDismissed = false;
                        } else if ("REPORTED_CONTENT_DELETED".equalsIgnoreCase(s)) {
                            hasDeleted = true;
                            allDismissed = false;
                        } else if (!"DISMISSED".equalsIgnoreCase(s)) {
                            allDismissed = false;
                        }
                    }
                    if (hasPending) {
                        statusToUse = "PENDING";
                    } else if (hasDeleted) {
                        statusToUse = "REPORTED_CONTENT_DELETED";
                    } else if (allDismissed && !reports.isEmpty()) {
                        statusToUse = "DISMISSED";
                    } else {
                        statusToUse = "PENDING";
                    }
                }

                AdminReportItem item = new AdminReportItem(
                        reports.get(0).getId(),
                        discussionId,
                        "DISCUSSION",
                        title,
                        dDoc.exists() ? dDoc.getString("content") : "",
                        null,
                        statusToUse,
                        reports.size(),
                        "Vừa xong",
                        (authorName != null && !authorName.trim().isEmpty()) ? authorName.trim() : "Người dùng",
                        authorAvatar,
                        Arrays.asList(category != null ? category : "Thảo luận")
                );
                item.setImageUrl(imageUrl);
                allDiscussionItems.add(item);
                if (currentTab == 0) applyFilterAndSearch();

                if ((authorName == null || authorName.trim().isEmpty() || authorAvatar == null || authorAvatar.trim().isEmpty())
                        && userId != null && !userId.trim().isEmpty()) {
                    db.collection("users").document(userId).get().addOnSuccessListener(uDoc -> {
                        if (!isAdded() || !uDoc.exists()) return;
                        String uName = uDoc.getString("fullName");
                        if (uName == null || uName.trim().isEmpty()) uName = uDoc.getString("username");
                        if (uName == null || uName.trim().isEmpty()) uName = uDoc.getString("email");
                        String uAvatar = uDoc.getString("avatar");
                        boolean changed = false;
                        if (uName != null && !uName.trim().isEmpty() && "Người dùng".equals(item.getAuthorName())) {
                            item.setAuthorName(uName.trim());
                            changed = true;
                        }
                        if (uAvatar != null && !uAvatar.trim().isEmpty() && item.getAuthorAvatar() == null) {
                            item.setAuthorAvatar(uAvatar.trim());
                            changed = true;
                        }
                        if (changed && currentTab == 0) applyFilterAndSearch();
                    });
                }
            });
        }
    }

    private void groupCommentReports(List<DocumentSnapshot> docs) {
        allCommentItems.clear();
        Map<String, List<DocumentSnapshot>> map = new HashMap<>();
        for (DocumentSnapshot doc : docs) {
            String cId = doc.getString("commentId");
            if (cId == null || cId.isEmpty()) continue;
            if (!map.containsKey(cId)) map.put(cId, new ArrayList<>());
            map.get(cId).add(doc);
        }

if (map.isEmpty()) {
            if (currentTab == 1) {
                pbLoading.setVisibility(View.GONE);
                applyFilterAndSearch();
            }
            return;
        }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (Map.Entry<String, List<DocumentSnapshot>> entry : map.entrySet()) {
            String commentId = entry.getKey();
            List<DocumentSnapshot> reports = entry.getValue();
            db.collection("comments").document(commentId).get().addOnSuccessListener(cDoc -> {
                if (!isAdded()) return;
                String content = cDoc.exists() ? cDoc.getString("content") : "Nội dung bình luận";
                String discussionId = cDoc.exists() ? cDoc.getString("discussionId") : "";
                String authorName = cDoc.exists() ? cDoc.getString("authorName") : null;
                String authorAvatar = cDoc.exists() ? cDoc.getString("authorAvatar") : null;
                String userId = cDoc.exists() ? cDoc.getString("userId") : null;
                String cStatus = cDoc.exists() ? cDoc.getString("status") : null;

                String statusToUse;
                if ("DELETED".equalsIgnoreCase(cStatus) || (cDoc.exists() && cDoc.get("deletedAt") != null)) {
                    statusToUse = "REPORTED_CONTENT_DELETED";
                } else {
                    boolean hasPending = false;
                    boolean hasDeleted = false;
                    boolean allDismissed = true;
                    for (DocumentSnapshot rDoc : reports) {
                        String s = rDoc.getString("status");
                        if ("PENDING".equalsIgnoreCase(s) || s == null || s.isEmpty()) {
                            hasPending = true;
                            allDismissed = false;
                        } else if ("REPORTED_CONTENT_DELETED".equalsIgnoreCase(s)) {
                            hasDeleted = true;
                            allDismissed = false;
                        } else if (!"DISMISSED".equalsIgnoreCase(s)) {
                            allDismissed = false;
                        }
                    }
                    if (hasPending) {
                        statusToUse = "PENDING";
                    } else if (hasDeleted) {
                        statusToUse = "REPORTED_CONTENT_DELETED";
                    } else if (allDismissed && !reports.isEmpty()) {
                        statusToUse = "DISMISSED";
                    } else {
                        statusToUse = "PENDING";
                    }
                }

                AdminReportItem item = new AdminReportItem(
                        reports.get(0).getId(),
                        commentId,
                        "COMMENT",
                        content,
                        content,
                        null,
                        statusToUse,
                        reports.size(),
                        "Vừa xong",
                        (authorName != null && !authorName.trim().isEmpty()) ? authorName.trim() : "Người dùng",
                        authorAvatar,
                        new ArrayList<>()
                );

                if ((authorName == null || authorName.trim().isEmpty() || authorAvatar == null || authorAvatar.trim().isEmpty())
                        && userId != null && !userId.trim().isEmpty()) {
                    db.collection("users").document(userId).get().addOnSuccessListener(uDoc -> {
                        if (!isAdded() || !uDoc.exists()) return;
                        String uName = uDoc.getString("fullName");
                        if (uName == null || uName.trim().isEmpty()) uName = uDoc.getString("username");
                        if (uName == null || uName.trim().isEmpty()) uName = uDoc.getString("email");
                        String uAvatar = uDoc.getString("avatar");
                        boolean changed = false;
                        if (uName != null && !uName.trim().isEmpty() && "Người dùng".equals(item.getAuthorName())) {
                            item.setAuthorName(uName.trim());
                            changed = true;
                        }
                        if (uAvatar != null && !uAvatar.trim().isEmpty() && item.getAuthorAvatar() == null) {
                            item.setAuthorAvatar(uAvatar.trim());
                            changed = true;
                        }
                        if (changed && currentTab == 1) applyFilterAndSearch();
                    });
                }

                if (discussionId != null && !discussionId.isEmpty()) {
                    db.collection("discussions").document(discussionId).get().addOnSuccessListener(dDoc -> {
                        if (dDoc.exists()) {
                            item.setParentQuestionTitle(dDoc.getString("title"));
                        }
                        allCommentItems.add(item);
                        if (currentTab == 1) applyFilterAndSearch();
                    }).addOnFailureListener(e -> {
                        allCommentItems.add(item);
                        if (currentTab == 1) applyFilterAndSearch();
                    });
                } else {
                    allCommentItems.add(item);
                    if (currentTab == 1) applyFilterAndSearch();
                }
            });
        }
    }

    private void createMockDiscussions() {
        allDiscussionItems.clear();
        allDiscussionItems.add(new AdminReportItem(
                "disc_rep_1",
                "disc_1",
                "DISCUSSION",
                "Góc thảo luận: Có nên tin tưởng các kết quả nghiên cứu y sinh không công bố source code?",
                "Hiện nay rất nhiều bài báo khoa học về phân tích dữ liệu lâm sàng từ chối công khai mã nguồn và dataset gốc với lý do bảo mật, liệu tính tái lập (reproducibility) của các nghiên cứu này có đáng tin cậy?",
                null,
                "PENDING",
                3,
                "1 giờ trước",
                "Trần Thanh Phong",
                null,
                Arrays.asList("Khoa học", "Dữ liệu")
        ));

        allDiscussionItems.add(new AdminReportItem(
                "disc_rep_2",
                "disc_2",
                "DISCUSSION",
                "Hội nhóm chia sẻ tài liệu ôn thi và đề thi nội bộ đại học (Kèm link Drive)",
                "Chào các bạn, mình có tổng hợp trọn bộ đề thi giữa kỳ và tài liệu mật của khoa Công nghệ Thông tin, bạn nào cần tải ngay tại link rút gọn này kẻo bị xóa...",
                null,
                "PENDING",
                4,
                "4 giờ trước",
                "Nguyễn Hoàng Long",
                null,
                Arrays.asList("Tài liệu", "Học tập")
        ));

        allDiscussionItems.add(new AdminReportItem(
                "disc_rep_3",
                "disc_3",
                "DISCUSSION",
                "Thảo luận về việc áp dụng blockchain trong xuất bản tạp chí truy cập mở (Open Access)",
                "Bài viết này kêu gọi mua token để ủng hộ nền tảng nghiên cứu mới, hứa hẹn x100 tài khoản cho các nhà nghiên cứu tham gia sớm...",
                null,
                "REPORTED_CONTENT_DELETED",
                6,
                "1 ngày trước",
                "Võ Văn Kiệt",
                null,
                Arrays.asList("Blockchain", "Crypto")
        ));

        allDiscussionItems.add(new AdminReportItem(
                "disc_rep_4",
                "disc_4",
                "DISCUSSION",
                "Review công tâm quy trình bình duyệt peer-review của tạp chí Q1 Nature",
                "Chia sẻ trải nghiệm thực tế sau 6 tháng chỉnh sửa bản thảo và phản hồi các câu hỏi phản biện gắt gao của 3 reviewer độc lập.",
                null,
                "DISMISSED",
                1,
                "2 ngày trước",
                "Bùi Minh Tuấn",
                null,
                Arrays.asList("Học thuật", "Review")
        ));
    }

    private void createMockComments() {
        allCommentItems.clear();
        allCommentItems.add(new AdminReportItem(
                "cmt_rep_1",
                "cmt_1",
                "COMMENT",
                "Tác giả bài viết này không có kiến thức gì mà cũng đòi phân tích, nghiên cứu như rác rưởi...",
                "Tác giả bài viết này không có kiến thức gì mà cũng đòi phân tích, nghiên cứu như rác rưởi...",
                "Góc thảo luận: Có nên tin tưởng các kết quả nghiên cứu y sinh?",
                "PENDING",
                3,
                "2 giờ trước",
                "Lê Hữu Đạt",
                null,
                new ArrayList<>()
        ));

        allCommentItems.add(new AdminReportItem(
                "cmt_rep_2",
                "cmt_2",
                "COMMENT",
                "Cần làm luận văn thạc sĩ trọn gói bao đỗ liên hệ Zalo 09xx-xxx-xxx uy tín giá sinh viên...",
                "Cần làm luận văn thạc sĩ trọn gói bao đỗ liên hệ Zalo 09xx-xxx-xxx uy tín giá sinh viên...",
                "Hội nhóm chia sẻ tài liệu ôn thi và đề thi nội bộ",
                "PENDING",
                5,
                "5 giờ trước",
                "Dịch vụ Học thuật",
                null,
                new ArrayList<>()
        ));

        allCommentItems.add(new AdminReportItem(
                "cmt_rep_3",
                "cmt_3",
                "COMMENT",
                "Nội dung vi phạm xúc phạm thành viên khác trong diễn đàn...",
                "Nội dung vi phạm xúc phạm thành viên khác trong diễn đàn...",
                "Thảo luận về việc áp dụng blockchain trong xuất bản tạp chí",
                "REPORTED_CONTENT_DELETED",
                2,
                "1 ngày trước",
                "Tài khoản Ẩn danh",
                null,
                new ArrayList<>()
        ));

        allCommentItems.add(new AdminReportItem(
                "cmt_rep_4",
                "cmt_4",
                "COMMENT",
                "Cảm ơn tác giả đã chia sẻ bài viết rất hữu ích cho các nghiên cứu sinh!",
                "Cảm ơn tác giả đã chia sẻ bài viết rất hữu ích cho các nghiên cứu sinh!",
                "Review công tâm quy trình bình duyệt peer-review",
                "DISMISSED",
                1,
                "3 ngày trước",
                "Đỗ Lan Anh",
                null,
                new ArrayList<>()
        ));
    }

    private void applyFilterAndSearch() {
        List<AdminReportItem> sourceList = (currentTab == 0) ? allDiscussionItems : allCommentItems;
        List<AdminReportItem> filteredList = new ArrayList<>();

        for (AdminReportItem item : sourceList) {
            // 1. Kiểm tra Filter Chip
            boolean matchesFilter = true;
            if (!"ALL".equals(currentFilter)) {
                matchesFilter = currentFilter.equalsIgnoreCase(item.getStatus());
            }

            // 2. Kiểm tra Search Keyword
            boolean matchesSearch = true;
            if (!currentSearchKeyword.isEmpty()) {
                String title = item.getTitle() != null ? item.getTitle().toLowerCase(Locale.ROOT) : "";
                String content = item.getContent() != null ? item.getContent().toLowerCase(Locale.ROOT) : "";
                String author = item.getAuthorName() != null ? item.getAuthorName().toLowerCase(Locale.ROOT) : "";
                matchesSearch = title.contains(currentSearchKeyword)
                        || content.contains(currentSearchKeyword)
                        || author.contains(currentSearchKeyword);
            }

            if (matchesFilter && matchesSearch) {
                filteredList.add(item);
            }
        }

        adapter.setItems(filteredList);

        if (filteredList.isEmpty()) {
            rvReports.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvReports.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (discussionReportsListener != null) {
            discussionReportsListener.remove();
            discussionReportsListener = null;
        }
        if (commentReportsListener != null) {
            commentReportsListener.remove();
            commentReportsListener = null;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            applyFilterAndSearch();
        }
    }
}
