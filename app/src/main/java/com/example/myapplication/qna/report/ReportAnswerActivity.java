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
import com.example.myapplication.qna.report.ReportAnswerConstants;
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

public class ReportAnswerActivity extends AppCompatActivity {

    public static final String EXTRA_ANSWER_ID = "answerId";
    public static final String EXTRA_CONTENT = "content";

    private TextView tvAnswerTitle;
    private TextView tvAnswerContent;
    private RadioGroup rgReason;
    private EditText etDetails;
    private Button btnSubmit;
    private Button btnCancel;

    private FirebaseFirestore firestore;

    private String answerId;
    private String answerContent;

    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_report_answer);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);

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

        answerId = getIntent().getStringExtra(EXTRA_ANSWER_ID);
        answerContent = getIntent().getStringExtra(EXTRA_CONTENT);

        if (answerId == null || answerId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Không tìm thấy câu trả lời cần báo cáo.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        initViews();
        displayAnswerInfo();

        btnSubmit.setOnClickListener(v -> submitReport());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvAnswerTitle = findViewById(R.id.tv_report_answer_title);
        tvAnswerContent = findViewById(R.id.tv_report_answer_content);
        rgReason = findViewById(R.id.rg_report_reason);
        etDetails = findViewById(R.id.et_report_details);
        btnSubmit = findViewById(R.id.btn_submit_report);
        btnCancel = findViewById(R.id.btn_cancel_report);
    }

    private void displayAnswerInfo() {
        tvAnswerTitle.setText("Câu trả lời");

        tvAnswerContent.setText(
                valueOrDefault(answerContent, "Không có nội dung")
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

        if (ReportAnswerConstants.REASON_OTHER.equals(reason)
                && details.isEmpty()) {
            etDetails.setError(
                    ReportAnswerConstants.ERROR_OTHER_REASON_REQUIRED
            );
            etDetails.requestFocus();
            return;
        }

        String userId = currentUser.getUid();

        DocumentReference answerRef =
                firestore.collection("answers")
                        .document(answerId);

        final DocumentReference reportRef;

        try {
            String reportId = createReportId(userId, answerId);

            reportRef = firestore.collection("answerReports")
                    .document(reportId);

        } catch (Exception exception) {
            Log.e(
                    "ReportAnswerActivity",
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

        isSubmitting = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        firestore.runTransaction(transaction -> {
            DocumentSnapshot existingReport =
                    transaction.get(reportRef);

            DocumentSnapshot answerSnapshot =
                    transaction.get(answerRef);

            if (existingReport.exists()) {
                return "DUPLICATE";
            }

            if (!answerSnapshot.exists()) {
                throw new IllegalStateException(
                        "Không tìm thấy câu trả lời."
                );
            }

            String parentQuestionId =
                    answerSnapshot.getString("questionId");

            Map<String, Object> report = new HashMap<>();

            report.put("answerId", answerId);
            report.put(
                    "questionId",
                    parentQuestionId == null ? "" : parentQuestionId
            );
            report.put("userId", userId);
            report.put("reason", reason);
            report.put("detailReason", details);
            report.put(
                    "status",
                    ReportAnswerConstants.STATUS_PENDING
            );
            report.put("createdAt", FieldValue.serverTimestamp());
            report.put("reviewedAt", null);
            report.put("reviewedBy", "");
            report.put("reviewReason", "");
            report.put(
                    "answerContent",
                    valueOrDefault(answerContent, "Câu trả lời")
            );

            transaction.set(reportRef, report);

            transaction.update(
                    answerRef,
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
                        "Đã gửi báo cáo câu trả lời thành công.",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        this,
                        "Bạn đã báo cáo câu trả lời này trước đó.",
                        Toast.LENGTH_LONG
                ).show();
            }

            finish();

        }).addOnFailureListener(exception -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            Log.e(
                    "ReportAnswerActivity",
                    "Failed to submit answer report",
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
            return ReportAnswerConstants.REASON_HARASSMENT;
        }

        if (selectedId == R.id.rb_misinformation) {
            return ReportAnswerConstants.REASON_MISINFORMATION;
        }

        if (selectedId == R.id.rb_spam) {
            return ReportAnswerConstants.REASON_SPAM;
        }

        if (selectedId == R.id.rb_inappropriate) {
            return ReportAnswerConstants.REASON_INAPPROPRIATE_CONTENT;
        }

        if (selectedId == R.id.rb_off_topic) {
            return ReportAnswerConstants.REASON_OFF_TOPIC;
        }

        if (selectedId == R.id.rb_copyright) {
            return ReportAnswerConstants.REASON_COPYRIGHT_VIOLATION;
        }

        if (selectedId == R.id.rb_privacy) {
            return ReportAnswerConstants.REASON_PRIVACY_VIOLATION;
        }

        if (selectedId == R.id.rb_other) {
            return ReportAnswerConstants.REASON_OTHER;
        }

        return "";
    }

    private String createReportId(
            String userId,
            String answerId
    ) throws Exception {
        String input = userId + ":" + answerId;

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

