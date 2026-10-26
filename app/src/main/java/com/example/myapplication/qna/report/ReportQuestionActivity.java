
package com.example.myapplication.qna.report;

import android.os.Bundle;
import android.util.Log;
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
import com.example.myapplication.qna.report.ReportQuestionConstants;
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

public class ReportQuestionActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "questionId";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CONTENT = "content";

    private TextView tvQuestionTitle;
    private TextView tvQuestionContent;
    private RadioGroup rgReason;
    private EditText etDetails;
    private Button btnSubmit;
    private Button btnCancel;

    private FirebaseFirestore firestore;

    private String questionId;
    private String questionTitle;
    private String questionContent;

    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_report_question);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        insetsController.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.root);
        View statusBarSpacer = findViewById(R.id.status_bar_spacer);

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {
                    Insets bars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout()
                    );

                    ViewGroup.LayoutParams params =
                            statusBarSpacer.getLayoutParams();

                    params.height = bars.top;
                    statusBarSpacer.setLayoutParams(params);

                    view.setPadding(
                            bars.left,
                            0,
                            bars.right,
                            bars.bottom
                    );

                    return WindowInsetsCompat.CONSUMED;
                }
        );

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();

        questionId = getIntent().getStringExtra(EXTRA_QUESTION_ID);
        questionTitle = getIntent().getStringExtra(EXTRA_TITLE);
        questionContent = getIntent().getStringExtra(EXTRA_CONTENT);

        if (questionId == null || questionId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Không tìm thấy câu hỏi cần báo cáo.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        initViews();
        displayQuestionInfo();

        btnSubmit.setOnClickListener(v -> submitReport());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvQuestionTitle =
                findViewById(R.id.tv_report_question_title);

        tvQuestionContent =
                findViewById(R.id.tv_report_question_content);

        rgReason = findViewById(R.id.rg_report_reason);
        etDetails = findViewById(R.id.et_report_details);
        btnSubmit = findViewById(R.id.btn_submit_report);
        btnCancel = findViewById(R.id.btn_cancel_report);
    }

    private void displayQuestionInfo() {
        tvQuestionTitle.setText(
                valueOrDefault(questionTitle, "Câu hỏi")
        );

        tvQuestionContent.setText(
                valueOrDefault(questionContent, "Không có nội dung")
        );
    }

    private void submitReport() {
        if (isSubmitting) {
            return;
        }

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập để gửi báo cáo.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int selectedId = rgReason.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(
                    this,
                    "Vui lòng chọn lý do báo cáo.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String reason = getReasonText(selectedId);
        String details = etDetails.getText().toString().trim();

        if (reason.isEmpty()) {
            Toast.makeText(
                    this,
                    "Lý do báo cáo không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (ReportQuestionConstants.REASON_OTHER.equals(reason)
                && details.isEmpty()) {
            etDetails.setError(
                    ReportQuestionConstants.ERROR_OTHER_REASON_REQUIRED
            );
            etDetails.requestFocus();
            return;
        }

        String userId = currentUser.getUid();

        final DocumentReference reportRef;

        DocumentReference questionRef =
                firestore.collection("questions")
                        .document(questionId);

        try {
            String reportId = createReportId(userId, questionId);

            reportRef = firestore.collection("questionReports")
                    .document(reportId);

        } catch (Exception exception) {
            Log.e(
                    "ReportQuestionActivity",
                    "Could not create report ID",
                    exception
            );

            Toast.makeText(
                    this,
                    "Không thể tạo báo cáo. Vui lòng thử lại.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();

        report.put("questionId", questionId);
        report.put("userId", userId);
        report.put("reason", reason);
        report.put("detailReason", details);
        report.put("status", ReportQuestionConstants.STATUS_PENDING);
        report.put("createdAt", FieldValue.serverTimestamp());
        report.put("reviewedAt", null);
        report.put("reviewedBy", "");
        report.put("reviewReason", "");
        report.put(
                "questionTitle",
                valueOrDefault(questionTitle, "Câu hỏi")
        );

        isSubmitting = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        firestore.runTransaction(transaction -> {
            DocumentSnapshot existingReport =
                    transaction.get(reportRef);

            if (existingReport.exists()) {
                return "DUPLICATE";
            }

            DocumentSnapshot questionSnapshot =
                    transaction.get(questionRef);

            if (!questionSnapshot.exists()) {
                throw new IllegalStateException(
                        "Không tìm thấy câu hỏi."
                );
            }

            transaction.set(reportRef, report);

            transaction.update(
                    questionRef,
                    "reportCount",
                    FieldValue.increment(1)
            );

            return "CREATED";

        }).addOnSuccessListener(result -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if ("CREATED".equals(result)) {
                Toast.makeText(
                        this,
                        "Đã gửi báo cáo câu hỏi thành công.",
                        Toast.LENGTH_SHORT
                ).show();

            } else {
                Toast.makeText(
                        this,
                        "Bạn đã báo cáo câu hỏi này trước đó.",
                        Toast.LENGTH_LONG
                ).show();
            }

            finish();

        }).addOnFailureListener(exception -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            Log.e(
                    "ReportQuestionActivity",
                    "Failed to submit question report",
                    exception
            );

            isSubmitting = false;
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Gửi báo cáo");

            Toast.makeText(
                    this,
                    "Gửi báo cáo thất bại. Vui lòng kiểm tra mạng và thử lại.",
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private String getReasonText(int selectedId) {
        if (selectedId == R.id.rb_harassment) {
            return ReportQuestionConstants.REASON_HARASSMENT;
        }

        if (selectedId == R.id.rb_misinformation) {
            return ReportQuestionConstants.REASON_MISINFORMATION;
        }

        if (selectedId == R.id.rb_spam) {
            return ReportQuestionConstants.REASON_SPAM;
        }

        if (selectedId == R.id.rb_inappropriate) {
            return ReportQuestionConstants.REASON_INAPPROPRIATE_CONTENT;
        }

        if (selectedId == R.id.rb_off_topic) {
            return ReportQuestionConstants.REASON_OFF_TOPIC;
        }

        if (selectedId == R.id.rb_copyright) {
            return ReportQuestionConstants.REASON_COPYRIGHT_VIOLATION;
        }

        if (selectedId == R.id.rb_privacy) {
            return ReportQuestionConstants.REASON_PRIVACY_VIOLATION;
        }

        if (selectedId == R.id.rb_other) {
            return ReportQuestionConstants.REASON_OTHER;
        }

        return "";
    }

    private String createReportId(
            String userId,
            String questionId
    ) throws Exception {
        String input = userId + ":" + questionId;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] hash = digest.digest(
                input.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder result = new StringBuilder();

        for (byte value : hash) {
            result.append(String.format("%02x", value & 0xff));
        }

        return result.toString();
    }

    private String valueOrDefault(
            String value,
            String defaultValue
    ) {
        return value == null || value.trim().isEmpty()
                ? defaultValue
                : value;
    }
}
