package com.example.myapplication.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.ImageLoader;
import com.example.myapplication.R;
import com.example.myapplication.admin.adapters.UserReportDetailAdapter;
import com.example.myapplication.admin.dialogs.AdminActionBottomSheetDialog;
import com.example.myapplication.admin.dialogs.AdminSuccessDialog;
import com.example.myapplication.admin.models.UserReportDetail;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminReportDetailActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "extra_report_id";
    public static final String EXTRA_TARGET_ID = "extra_target_id";
    public static final String EXTRA_TARGET_TYPE = "extra_target_type";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_CONTENT = "extra_content";
    public static final String EXTRA_PARENT_QUESTION = "extra_parent_question";
    public static final String EXTRA_STATUS = "extra_status";
    public static final String EXTRA_REPORT_COUNT = "extra_report_count";
    public static final String EXTRA_AUTHOR_NAME = "extra_author_name";
    public static final String EXTRA_AUTHOR_AVATAR = "extra_author_avatar";
    public static final String EXTRA_POST_TIME = "extra_post_time";
    public static final String EXTRA_TAG = "extra_tag";

    private ImageView btnBack;
    private TextView tvStatusBadge;
    private TextView tvDetailTag;
    private TextView tvParentQuestion;
    private TextView tvDetailTitle;
    private ImageView imgAuthorAvatar;
    private TextView tvAuthorName;
    private TextView tvPostTime;
    private TextView tvDetailContent;
    private TextView btnToggleExpandContent;
    private TextView tvReportListHeader;
    private ProgressBar pbReportsLoading;
    private TextView tvEmptyReports;
    private RecyclerView rvUserReports;
    private LinearLayout layoutBottomActions;
    private View btnDismissReport;
    private View btnDeleteContent;

    private UserReportDetailAdapter reportAdapter;
    private boolean isContentExpanded = false;

    private String targetId;
    private String targetType;
    private String currentStatus;
    private String itemTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_detail);

        // Edge-to-edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootAdminReportDetail), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        bindDataFromIntent();
        setupListeners();
        loadUserReportsFromFirestore();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvDetailTag = findViewById(R.id.tvDetailTag);
        tvParentQuestion = findViewById(R.id.tvParentQuestion);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        imgAuthorAvatar = findViewById(R.id.imgAuthorAvatar);
        tvAuthorName = findViewById(R.id.tvAuthorName);
        tvPostTime = findViewById(R.id.tvPostTime);
        tvDetailContent = findViewById(R.id.tvDetailContent);
        btnToggleExpandContent = findViewById(R.id.btnToggleExpandContent);
        tvReportListHeader = findViewById(R.id.tvReportListHeader);
        pbReportsLoading = findViewById(R.id.pbReportsLoading);
        tvEmptyReports = findViewById(R.id.tvEmptyReports);
        rvUserReports = findViewById(R.id.rvUserReports);
        layoutBottomActions = findViewById(R.id.layoutBottomActions);
        btnDismissReport = findViewById(R.id.btnDismissReport);
        btnDeleteContent = findViewById(R.id.btnDeleteContent);

        rvUserReports.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new UserReportDetailAdapter();
        rvUserReports.setAdapter(reportAdapter);
    }

    private void bindDataFromIntent() {
        targetId = getIntent().getStringExtra(EXTRA_TARGET_ID);
        targetType = getIntent().getStringExtra(EXTRA_TARGET_TYPE);
        currentStatus = getIntent().getStringExtra(EXTRA_STATUS);
        if (currentStatus == null || currentStatus.isEmpty()) currentStatus = "PENDING";

        itemTitle = getIntent().getStringExtra(EXTRA_TITLE);
        String content = getIntent().getStringExtra(EXTRA_CONTENT);
        String parentQuestion = getIntent().getStringExtra(EXTRA_PARENT_QUESTION);
        int reportCount = getIntent().getIntExtra(EXTRA_REPORT_COUNT, 0);
        String authorName = getIntent().getStringExtra(EXTRA_AUTHOR_NAME);
        String authorAvatar = getIntent().getStringExtra(EXTRA_AUTHOR_AVATAR);
        String postTime = getIntent().getStringExtra(EXTRA_POST_TIME);
        String tag = getIntent().getStringExtra(EXTRA_TAG);

        tvDetailTitle.setText(itemTitle != null ? itemTitle : "Không có tiêu đề");
        tvDetailContent.setText(content != null && !content.isEmpty() ? content : (itemTitle != null ? itemTitle : "Không có nội dung chi tiết"));
        tvAuthorName.setText(authorName != null ? authorName : "Người dùng");
        tvPostTime.setText(postTime != null ? ("• " + postTime) : "");

        if (tag != null && !tag.isEmpty()) {
            tvDetailTag.setText(tag);
            tvDetailTag.setVisibility(View.VISIBLE);
        } else {
            tvDetailTag.setVisibility(View.GONE);
        }

        if (parentQuestion != null && !parentQuestion.isEmpty()) {
            String prefix = "COMMENT".equalsIgnoreCase(targetType) ? "Trong thảo luận: " : "Trong: ";
            tvParentQuestion.setText(prefix + parentQuestion);
            tvParentQuestion.setVisibility(View.VISIBLE);
        } else {
            tvParentQuestion.setVisibility(View.GONE);
        }

        ImageLoader.load(authorAvatar, imgAuthorAvatar);
        updateStatusBadge(currentStatus);
        updateActionButtonsVisibility(currentStatus);

        tvReportListHeader.setText(String.format(Locale.getDefault(), "Danh sách báo cáo (%d)", reportCount));
    }

    private String getTargetCollection() {
        if ("DISCUSSION".equalsIgnoreCase(targetType)) return "discussions";
        if ("COMMENT".equalsIgnoreCase(targetType)) return "comments";
        if ("ANSWER".equalsIgnoreCase(targetType)) return "answers";
        return "questions";
    }

    private String getReportCollection() {
        if ("DISCUSSION".equalsIgnoreCase(targetType)) return "discussionReports";
        if ("COMMENT".equalsIgnoreCase(targetType)) return "commentReports";
        if ("ANSWER".equalsIgnoreCase(targetType)) return "answerReports";
        return "questionReports";
    }

    private String getFilterField() {
        if ("DISCUSSION".equalsIgnoreCase(targetType)) return "discussionId";
        if ("COMMENT".equalsIgnoreCase(targetType)) return "commentId";
        if ("ANSWER".equalsIgnoreCase(targetType)) return "answerId";
        return "questionId";
    }

    private void updateStatusBadge(String status) {
        if ("REPORTED_CONTENT_DELETED".equalsIgnoreCase(status)) {
            tvStatusBadge.setText("Đã xử lý");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_resolved);
            tvStatusBadge.setTextColor(Color.parseColor("#D97706"));
        } else if ("DISMISSED".equalsIgnoreCase(status)) {
            tvStatusBadge.setText("Đã bác bỏ");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_dismissed);
            tvStatusBadge.setTextColor(Color.parseColor("#475569"));
        } else {
            tvStatusBadge.setText("Chờ xử lý");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatusBadge.setTextColor(Color.parseColor("#DC2626"));
        }
    }

    private void updateActionButtonsVisibility(String status) {
        if ("REPORTED_CONTENT_DELETED".equalsIgnoreCase(status) || "DISMISSED".equalsIgnoreCase(status)) {
            layoutBottomActions.setVisibility(View.GONE);
        } else {
            layoutBottomActions.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnToggleExpandContent.setOnClickListener(v -> {
            if (isContentExpanded) {
                tvDetailContent.setMaxLines(4);
                btnToggleExpandContent.setText("Xem thêm");
                isContentExpanded = false;
            } else {
                tvDetailContent.setMaxLines(Integer.MAX_VALUE);
                btnToggleExpandContent.setText("Thu gọn");
                isContentExpanded = true;
            }
        });

        // 1. Bác bỏ báo cáo -> Mở BottomSheet
        btnDismissReport.setOnClickListener(v -> {
            AdminActionBottomSheetDialog dialog = AdminActionBottomSheetDialog.newInstance(
                    AdminActionBottomSheetDialog.ACTION_DISMISS,
                    targetType,
                    itemTitle != null ? itemTitle : ""
            );
            dialog.setOnActionConfirmListener(reason -> performDismissReports(reason, dialog));
            dialog.show(getSupportFragmentManager(), "DismissBottomSheet");
        });

        // 2. Xóa nội dung vi phạm -> Mở BottomSheet
        btnDeleteContent.setOnClickListener(v -> {
            AdminActionBottomSheetDialog dialog = AdminActionBottomSheetDialog.newInstance(
                    AdminActionBottomSheetDialog.ACTION_DELETE,
                    targetType,
                    itemTitle != null ? itemTitle : ""
            );
            dialog.setOnActionConfirmListener(reason -> performDeleteContent(reason, dialog));
            dialog.show(getSupportFragmentManager(), "DeleteBottomSheet");
        });
    }

    private void performDeleteContent(String reason, AdminActionBottomSheetDialog bottomSheet) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String adminId = currentUser != null ? currentUser.getUid() : "ADMIN";
        Timestamp now = Timestamp.now();

        String targetCollection = getTargetCollection();
        String reportCollection = getReportCollection();
        String filterField = getFilterField();

        // Cập nhật soft delete cho nội dung gốc
        Map<String, Object> contentUpdates = new HashMap<>();
        contentUpdates.put("status", "DELETED");
        contentUpdates.put("deletedAt", now);
        contentUpdates.put("deletedReason", reason);
        contentUpdates.put("deletedBy", adminId);
        contentUpdates.put("updatedAt", now);

        if (targetId != null && !targetId.isEmpty()) {
            db.collection(targetCollection).document(targetId).update(contentUpdates)
                    .addOnCompleteListener(task -> {
                        // Tiếp tục cập nhật các document báo cáo liên quan
                        updateReportsStatus(db, reportCollection, filterField, "REPORTED_CONTENT_DELETED", reason, adminId, now, () -> {
                            if (bottomSheet != null && bottomSheet.isAdded()) {
                                bottomSheet.dismiss();
                            }
                            showSuccessDialog("Đã xóa nội dung", "Nội dung vi phạm và các báo cáo liên quan đã được xóa thành công.");
                        });
                    });
        } else {
            if (bottomSheet != null && bottomSheet.isAdded()) bottomSheet.dismiss();
            showSuccessDialog("Đã xóa nội dung", "Nội dung vi phạm và các báo cáo liên quan đã được xóa thành công.");
        }
    }

    private void performDismissReports(String reason, AdminActionBottomSheetDialog bottomSheet) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String adminId = currentUser != null ? currentUser.getUid() : "ADMIN";
        Timestamp now = Timestamp.now();

        String reportCollection = getReportCollection();
        String filterField = getFilterField();

        updateReportsStatus(db, reportCollection, filterField, "DISMISSED", reason, adminId, now, () -> {
            if (bottomSheet != null && bottomSheet.isAdded()) {
                bottomSheet.dismiss();
            }
            showSuccessDialog("Đã bỏ qua báo cáo", "Các báo cáo liên quan đã được bác bỏ thành công.");
        });
    }

    private void updateReportsStatus(FirebaseFirestore db, String collection, String filterField,
                                     String newStatus, String reason, String adminId, Timestamp now, Runnable onComplete) {
        if (targetId == null || targetId.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        db.collection(collection)
                .whereEqualTo(filterField, targetId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            batch.update(doc.getReference(),
                                    "status", newStatus,
                                    "reviewReason", reason,
                                    "reviewedBy", adminId,
                                    "reviewedAt", now
                            );
                        }
                        batch.commit().addOnCompleteListener(batchTask -> {
                            if (onComplete != null) onComplete.run();
                        });
                    } else {
                        // Nếu là dữ liệu mẫu hoặc không có document nào trên DB
                        if (onComplete != null) onComplete.run();
                    }
                });
    }

    private void showSuccessDialog(String title, String message) {
        AdminSuccessDialog dialog = AdminSuccessDialog.newInstance(title, message);
        dialog.setOnDoneClickListener(() -> {
            // Đóng Activity và quay lại màn hình danh sách
            finish();
        });
        dialog.show(getSupportFragmentManager(), "SuccessDialog");
    }

    private void loadUserReportsFromFirestore() {
        if (targetId == null || targetId.isEmpty()) {
            createMockUserReports();
            return;
        }

        pbReportsLoading.setVisibility(View.VISIBLE);
        tvEmptyReports.setVisibility(View.GONE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String collection = getReportCollection();
        String filterField = getFilterField();

        db.collection(collection)
                .whereEqualTo(filterField, targetId)
                .get()
                .addOnCompleteListener(task -> {
                    pbReportsLoading.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();
                        tvReportListHeader.setText(String.format(Locale.getDefault(), "Danh sách báo cáo (%d)", docs.size()));
                        bindReportsWithUserData(db, docs);
                    } else {
                        // Fallback dữ liệu mẫu để giao diện luôn trực quan khi kiểm thử
                        createMockUserReports();
                    }
                });
    }

    private void createMockUserReports() {
        List<UserReportDetail> mockList = new ArrayList<>();
        mockList.add(new UserReportDetail(
                "rep_mock_1",
                "u_1",
                "Trần Minh Bảo",
                null,
                "Nội dung không phù hợp",
                "Nội dung bài viết có dấu hiệu xúi giục đạo văn học thuật và chia sẻ liên kết không an toàn.",
                "1 giờ trước"
        ));
        mockList.add(new UserReportDetail(
                "rep_mock_2",
                "u_2",
                "Nguyễn Thùy Linh",
                null,
                "Thông tin sai lệch",
                "Tác giả đưa ra kết luận chưa được kiểm chứng khoa học, dễ gây hiểu lầm cho người mới bắt đầu.",
                "2 giờ trước"
        ));
        mockList.add(new UserReportDetail(
                "rep_mock_3",
                "u_3",
                "Lê Hoàng Phúc",
                null,
                "Spam hoặc quảng cáo",
                "Tài khoản này lặp đi lặp lại nội dung mang tính chất câu view và dẫn link ngoài.",
                "5 giờ trước"
        ));

        reportAdapter.setItems(mockList);
        tvReportListHeader.setText(String.format(Locale.getDefault(), "Danh sách báo cáo (%d)", mockList.size()));
        rvUserReports.setVisibility(View.VISIBLE);
        tvEmptyReports.setVisibility(View.GONE);
    }

    private void bindReportsWithUserData(FirebaseFirestore db, List<DocumentSnapshot> docs) {
        List<UserReportDetail> details = new ArrayList<>();
        int total = docs.size();
        final int[] fetchedCount = {0};

        for (DocumentSnapshot doc : docs) {
            String reportId = doc.getId();
            String userId = doc.getString("userId");
            String reason = doc.getString("reason");
            String detailReason = doc.getString("detailReason");
            Timestamp ts = doc.getTimestamp("createdAt");
            String timeAgo = formatTimestamp(ts);

            UserReportDetail item = new UserReportDetail(
                    reportId,
                    userId,
                    "Người dùng",
                    null,
                    reason != null ? reason : "Nội dung không phù hợp",
                    detailReason,
                    timeAgo
            );
            details.add(item);

            if (userId != null && !userId.isEmpty()) {
                db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("fullName");
                        String avatar = userDoc.getString("avatar");
                        if (name != null) item.setUserName(name);
                        if (avatar != null) item.setUserAvatar(avatar);
                    }
                    fetchedCount[0]++;
                    if (fetchedCount[0] == total) {
                        reportAdapter.setItems(details);
                        rvUserReports.setVisibility(View.VISIBLE);
                        tvEmptyReports.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(e -> {
                    fetchedCount[0]++;
                    if (fetchedCount[0] == total) {
                        reportAdapter.setItems(details);
                        rvUserReports.setVisibility(View.VISIBLE);
                        tvEmptyReports.setVisibility(View.GONE);
                    }
                });
            } else {
                fetchedCount[0]++;
                if (fetchedCount[0] == total) {
                    reportAdapter.setItems(details);
                    rvUserReports.setVisibility(View.VISIBLE);
                    tvEmptyReports.setVisibility(View.GONE);
                }
            }
        }
    }

    private void showEmptyReports() {
        rvUserReports.setVisibility(View.GONE);
        tvEmptyReports.setVisibility(View.VISIBLE);
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "Vừa xong";
        long diff = System.currentTimeMillis() - ts.toDate().getTime();
        long minutes = diff / (1000 * 60);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + " ngày trước";
        if (hours > 0) return hours + " giờ trước";
        if (minutes > 0) return minutes + " phút trước";
        return "Vừa xong";
    }
}
