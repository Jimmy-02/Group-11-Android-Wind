package com.example.myapplication.discussions.reports;

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

public class ReportDiscussionActivity extends AppCompatActivity {

    private TextView tvDiscussionTitle;
    private TextView tvDiscussionContent;
    private RadioGroup rgReason;
    private EditText etDetails;
    private Button btnSubmit;
    private Button btnCancel;

    private FirebaseFirestore firestore;

    private String discussionId;
    private String discussionTitle;
    private String discussionContent;

    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_report_discussion);

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

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();

        discussionId = getIntent().getStringExtra("discussionId");
        discussionTitle = getIntent().getStringExtra("title");
        discussionContent = getIntent().getStringExtra("content");

        if (discussionId == null || discussionId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài thảo luận cần báo cáo.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        displayDiscussionInfo();

        btnSubmit.setOnClickListener(v -> submitReport());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvDiscussionTitle = findViewById(R.id.tv_report_discussion_title);
        tvDiscussionContent = findViewById(R.id.tv_report_discussion_content);
        rgReason = findViewById(R.id.rg_report_reason);
        etDetails = findViewById(R.id.et_report_details);
        btnSubmit = findViewById(R.id.btn_submit_report);
        btnCancel = findViewById(R.id.btn_cancel_report);
    }

    private void displayDiscussionInfo() {
        tvDiscussionTitle.setText(valueOrDefault(discussionTitle, "Bài thảo luận"));

        tvDiscussionContent.setText(valueOrDefault(discussionContent, "Không có nội dung"));
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

        if (ReportDiscussionConstants.REASON_OTHER.equals(reason) && details.isEmpty()) {
            etDetails.setError(ReportDiscussionConstants.ERROR_OTHER_REASON_REQUIRED);
            etDetails.requestFocus();
            return;
        }

        String userId = currentUser.getUid();

        DocumentReference reportRef;
        DocumentReference discussionRef = firestore.collection("discussions").document(discussionId);

        try {
            String reportId = createReportId(userId, discussionId);
            reportRef = firestore.collection("discussionReports").document(reportId);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể tạo báo cáo. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("discussionId", discussionId);
        report.put("userId", userId);
        report.put("reason", reason);
        report.put("detailReason", details);
        report.put("status", ReportDiscussionConstants.STATUS_PENDING);
        report.put("createdAt", FieldValue.serverTimestamp());
        report.put("reviewedAt", null);
        report.put("reviewedBy", "");
        report.put("reviewReason", "");
        report.put("discussionTitle", valueOrDefault(discussionTitle, "Bài thảo luận"));

        isSubmitting = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");
        //Giả sử người dùng A báo cáo Discussion X:
        //Lần đầu: tạo report và tăng reportCount từ 0 lên 1.
        //ần thứ hai: phát hiện report đã tồn tại, không tạo thêm và không tăng bộ đếm.
        // Nếu transaction thất bại: cả việc tạo report lẫn tăng bộ đếm repotr đều sẽ không xảy ra.
        // Nên là xài transaction cho việc này
        firestore.runTransaction(transaction -> {
            DocumentSnapshot existingReport = transaction.get(reportRef);

            if (existingReport.exists()) {
                return "DUPLICATE";
            }

            DocumentSnapshot discussionSnapshot = transaction.get(discussionRef);

            if (!discussionSnapshot.exists()) {
                throw new IllegalStateException("Discussion does not exist.");
            }

            transaction.set(reportRef, report);
            transaction.update(discussionRef, "reportCount", FieldValue.increment(1));

            return "CREATED";
        }).addOnSuccessListener(result -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if ("CREATED".equals(result)) {
                Toast.makeText(this, "Đã gửi báo cáo thành công.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Bạn đã báo cáo bài thảo luận này trước đó.", Toast.LENGTH_LONG).show();
                finish();
            }
        }).addOnFailureListener(exception -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            isSubmitting = false;
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Gửi báo cáo");

            android.util.Log.e("ReportDiscussionActivity", "Failed to submit discussion report", exception);

            Toast.makeText(this, "Gửi báo cáo thất bại. Vui lòng kiểm tra mạng và thử lại.", Toast.LENGTH_LONG).show();
        });
    }

    private String getReasonText(int selectedId) {
        if (selectedId == R.id.rb_harassment) {
            return ReportDiscussionConstants.REASON_HARASSMENT;
        }

        if (selectedId == R.id.rb_misinformation) {
            return ReportDiscussionConstants.REASON_MISINFORMATION;
        }

        if (selectedId == R.id.rb_spam) {
            return ReportDiscussionConstants.REASON_SPAM;
        }

        if (selectedId == R.id.rb_inappropriate) {
            return ReportDiscussionConstants.REASON_INAPPROPRIATE_CONTENT;
        }

        if (selectedId == R.id.rb_off_topic) {
            return ReportDiscussionConstants.REASON_OFF_TOPIC;
        }

        if (selectedId == R.id.rb_copyright) {
            return ReportDiscussionConstants.REASON_COPYRIGHT_VIOLATION;
        }

        if (selectedId == R.id.rb_privacy) {
            return ReportDiscussionConstants.REASON_PRIVACY_VIOLATION;
        }

        if (selectedId == R.id.rb_other) {
            return ReportDiscussionConstants.REASON_OTHER;
        }

        return "";
    }

    private String createReportId(String userId, String discussionId) throws Exception {
        String input = userId + ":" + discussionId;
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