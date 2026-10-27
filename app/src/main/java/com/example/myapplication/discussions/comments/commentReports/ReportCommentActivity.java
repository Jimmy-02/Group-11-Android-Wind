package com.example.myapplication.discussions.comments.commentReports;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.myapplication.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ReportCommentActivity extends AppCompatActivity {

    private TextView tvCommentAuthor;
    private TextView tvCommentContent;
    private RadioGroup rgReason;
    private EditText etDetails;
    private Button btnSubmit;
    private Button btnCancel;

    private FirebaseFirestore firestore;

    private String commentId;
    private String discussionId;
    private String commentAuthor;
    private String commentContent;

    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_report_comment);

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.root);
        View statusBarSpacer = findViewById(R.id.status_bar_spacer);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.LayoutParams params = statusBarSpacer.getLayoutParams();
            params.height = bars.top;
            statusBarSpacer.setLayoutParams(params);

            v.setPadding(bars.left, 0, bars.right, bars.bottom);

            return WindowInsetsCompat.CONSUMED;
        });

        firestore = FirebaseFirestore.getInstance();

        commentId = getIntent().getStringExtra("commentId");
        discussionId = getIntent().getStringExtra("discussionId");
        commentAuthor = getIntent().getStringExtra("authorName");
        commentContent = getIntent().getStringExtra("content");

        if (commentId == null || commentId.trim().isEmpty() || discussionId == null || discussionId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bình luận cần báo cáo.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        displayCommentInfo();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> submitReport());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvCommentAuthor = findViewById(R.id.tv_report_comment_author);
        tvCommentContent = findViewById(R.id.tv_report_comment_content);
        rgReason = findViewById(R.id.rg_report_reason);
        etDetails = findViewById(R.id.et_report_details);
        btnSubmit = findViewById(R.id.btn_submit_report);
        btnCancel = findViewById(R.id.btn_cancel_report);
    }

    private void displayCommentInfo() {
        tvCommentAuthor.setText(valueOrDefault(commentAuthor, "Người dùng"));
        tvCommentContent.setText(valueOrDefault(commentContent, "Không có nội dung"));
    }

    private void submitReport() {
        if (isSubmitting) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để gửi báo cáo.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = rgReason.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Vui lòng chọn lý do báo cáo.", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = getReasonText(selectedId);
        String details = etDetails.getText().toString().trim();

        if (reason.isEmpty()) {
            Toast.makeText(this, "Lý do báo cáo không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        etDetails.setError(null);

        if (CommentReportConstants.REASON_OTHER.equals(reason) && details.isEmpty()) {
            etDetails.setError(CommentReportConstants.ERROR_OTHER_REASON_REQUIRED);
            etDetails.requestFocus();
            return;
        }

        String userId = currentUser.getUid();

        DocumentReference reportRef;
        DocumentReference commentRef = firestore.collection("comments").document(commentId);

        try {
            String reportId = createReportId(userId, commentId);
            reportRef = firestore.collection("commentReports").document(reportId);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể tạo báo cáo. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("commentId", commentId);
        report.put("discussionId", discussionId);
        report.put("userId", userId);
        report.put("reason", reason);
        report.put("detailReason", details);
        report.put("status", CommentReportConstants.STATUS_PENDING);
        report.put("createdAt", FieldValue.serverTimestamp());
        report.put("reviewedAt", null);
        report.put("reviewedBy", "");
        report.put("reviewReason", "");

        isSubmitting = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        firestore.runTransaction(transaction -> {
            DocumentSnapshot existingReport = transaction.get(reportRef);

            if (existingReport.exists()) {
                return "DUPLICATE";
            }

            DocumentSnapshot commentSnapshot = transaction.get(commentRef);

            if (!commentSnapshot.exists()
                    || !"ACTIVE".equals(commentSnapshot.getString("status"))) {
                throw new IllegalStateException("Comment does not exist or is inactive.");
            }

            String storedDiscussionId = commentSnapshot.getString("discussionId");

            if (!discussionId.equals(storedDiscussionId)) {
                throw new IllegalStateException("Discussion ID does not match the comment.");
            }

            Long storedCount = commentSnapshot.getLong("reportCount");
            long reportCount = storedCount != null ? Math.max(0, storedCount) : 0;

            transaction.set(reportRef, report);
            transaction.update(commentRef, "reportCount", reportCount + 1);

            return "CREATED";
        }).addOnSuccessListener(result -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if ("CREATED".equals(result)) {
                Toast.makeText(this, "Đã gửi báo cáo thành công.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bạn đã báo cáo bình luận này trước đó.", Toast.LENGTH_LONG).show();
            }

            finish();
        }).addOnFailureListener(exception -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            isSubmitting = false;
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Gửi báo cáo");

            android.util.Log.e("ReportCommentActivity", "Failed to submit comment report", exception);

            Toast.makeText(this, "Gửi báo cáo thất bại. Vui lòng kiểm tra mạng và thử lại.", Toast.LENGTH_LONG).show();
        });
    }

    private String getReasonText(int selectedId) {
        if (selectedId == R.id.rb_harassment) {
            return CommentReportConstants.REASON_HARASSMENT;
        }

        if (selectedId == R.id.rb_misinformation) {
            return CommentReportConstants.REASON_MISINFORMATION;
        }

        if (selectedId == R.id.rb_spam) {
            return CommentReportConstants.REASON_SPAM;
        }

        if (selectedId == R.id.rb_inappropriate) {
            return CommentReportConstants.REASON_INAPPROPRIATE_CONTENT;
        }

        if (selectedId == R.id.rb_off_topic) {
            return CommentReportConstants.REASON_OFF_TOPIC;
        }

        if (selectedId == R.id.rb_privacy) {
            return CommentReportConstants.REASON_PRIVACY_VIOLATION;
        }

        if (selectedId == R.id.rb_other) {
            return CommentReportConstants.REASON_OTHER;
        }

        return "";
    }

    private String createReportId(String userId, String commentId) throws Exception {
        String input = userId + ":" + commentId;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();

        for (byte b : hash) {
            result.append(String.format("%02x", b & 0xff));
        }

        return result.toString();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}