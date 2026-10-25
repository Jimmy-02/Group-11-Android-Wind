package com.example.myapplication.articles.reports;

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

public class ReportArticleActivity extends AppCompatActivity {

    private TextView tvArticleTitle;
    private TextView tvArticleAuthor;
    private RadioGroup rgReason;
    private EditText etDetails;
    private Button btnSubmit;
    private Button btnCancel;

    private FirebaseFirestore firestore;

    private String articleId;
    private String articleTitle;
    private String authorName;

    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_report_article);

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.root);
        View statusBarSpacer = findViewById(R.id.status_bar_spacer);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.LayoutParams params = statusBarSpacer.getLayoutParams();
            params.height = bars.top;
            statusBarSpacer.setLayoutParams(params);

            v.setPadding(bars.left, 0, bars.right, bars.bottom);

            return WindowInsetsCompat.CONSUMED;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();

        articleId = getIntent().getStringExtra("articleId");
        articleTitle = getIntent().getStringExtra("title");
        authorName = getIntent().getStringExtra("authorName");

        if (articleId == null || articleId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài báo cần báo cáo.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        displayArticleInfo();

        btnSubmit.setOnClickListener(v -> submitReport());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvArticleTitle = findViewById(R.id.tv_report_article_title);
        tvArticleAuthor = findViewById(R.id.tv_report_article_author);
        rgReason = findViewById(R.id.rg_report_reason);
        etDetails = findViewById(R.id.et_report_details);
        btnSubmit = findViewById(R.id.btn_submit_report);
        btnCancel = findViewById(R.id.btn_cancel_report);
    }

    private void displayArticleInfo() {
        tvArticleTitle.setText(articleTitle != null && !articleTitle.trim().isEmpty() ? articleTitle : "Bài báo khoa học");
        tvArticleAuthor.setText(authorName != null && !authorName.trim().isEmpty() ? "Tác giả: " + authorName : "Chưa có thông tin tác giả");
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

        if (ReportArticleConstants.REASON_OTHER.equals(reason) && details.isEmpty()) {
            etDetails.setError(ReportArticleConstants.ERROR_OTHER_REASON_REQUIRED);
            etDetails.requestFocus();
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference reportRef;

        try {
            String reportId = createReportId(userId, articleId);
            reportRef = firestore.collection("articleReports").document(reportId);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể tạo báo cáo. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("articleId", articleId);
        report.put("userId", userId);
        report.put("reason", reason);
        report.put("details", details);
        report.put("status", ReportArticleConstants.STATUS_PENDING);
        report.put("createdAt", FieldValue.serverTimestamp());
        report.put("reviewedAt", null);
        report.put("reviewedBy", "");
        report.put("reviewReason", "");

        if (articleTitle != null) {
            report.put("articleTitle", articleTitle);
        }

        isSubmitting = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        firestore.runTransaction(transaction -> {
            DocumentSnapshot existing = transaction.get(reportRef);

            if (existing.exists()) {
                return false;
            }

            transaction.set(reportRef, report);
            return true;
        }).addOnSuccessListener(created -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (Boolean.TRUE.equals(created)) {
                Toast.makeText(this, "Đã gửi báo cáo thành công.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Bạn đã báo cáo bài viết này trước đó.", Toast.LENGTH_LONG).show();
                finish();
            }
        }).addOnFailureListener(exception -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            isSubmitting = false;
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Gửi báo cáo");

            android.util.Log.e("ReportArticleActivity", "Failed to submit article report", exception);
            Toast.makeText(this, "Gửi báo cáo thất bại. Vui lòng kiểm tra mạng và thử lại.", Toast.LENGTH_LONG).show();
        });
    }

    private String getReasonText(int selectedId) {
        if (selectedId == R.id.rb_misinformation) {
            return ReportArticleConstants.REASON_MISINFORMATION;
        }

        if (selectedId == R.id.rb_plagiarism) {
            return ReportArticleConstants.REASON_PLAGIARISM;
        }

        if (selectedId == R.id.rb_fake_publication) {
            return ReportArticleConstants.REASON_FAKE_PUBLICATION;
        }

        if (selectedId == R.id.rb_irrelevant) {
            return ReportArticleConstants.REASON_IRRELEVANT_CONTENT;
        }

        if (selectedId == R.id.rb_inappropriate) {
            return ReportArticleConstants.REASON_INAPPROPRIATE_CONTENT;
        }

        if (selectedId == R.id.rb_spam) {
            return ReportArticleConstants.REASON_SPAM;
        }

        if (selectedId == R.id.rb_broken_link) {
            return ReportArticleConstants.REASON_BROKEN_LINK;
        }

        if (selectedId == R.id.rb_other) {
            return ReportArticleConstants.REASON_OTHER;
        }

        return "";
    }

    //Sử dụng SHA-256 trong trường hợp này có ưu điểm là tạo ID xác định từ hai giá trị đầu vào, giúp kiểm tra trùng lặp dễ dàng.
    //
    //Lưu ý rằng SHA-256 ở đây chỉ được sử dụng để tạo ID, không phải để mã hóa dữ liệu hay bảo vệ thông tin người dùng.
    private String createReportId(String userId, String articleId) throws Exception {
        String input = userId + ":" + articleId;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();

        for (byte b : hash) {
            result.append(String.format("%02x", b & 0xff));
        }

        return result.toString();
    }
}