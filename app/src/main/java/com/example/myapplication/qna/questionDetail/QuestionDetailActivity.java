package com.example.myapplication.qna.questionDetail;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuestionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "question_id";

    private String questionId;
    private String questionStatus = "OPEN";

    private TextView tvDetailTitle;
    private TextView tvDetailStatus;
    private TextView tvDetailAuthorDate;
    private TextView tvDetailBody;
    private TextView tvDetailVoteCount;
    private TextView tvDetailAnswerCount;
    private TextView tvDetailAnswersEmpty;

    private ChipGroup chipGroupDetailCategories;

    private RecyclerView rvDetailAnswers;
    private ProgressBar pbDetailAnswers;

    private TextInputLayout tilDetailAnswer;
    private TextInputEditText edtDetailAnswer;

    private Button btnSubmitAnswer;
    private TextView tvQuestionUpVote;
    private TextView tvQuestionDownVote;

    private AnswerAdapter answerAdapter;

    private final List<Answer> answerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_question_detail);
        View qnaBackLayout = findViewById(R.id.QNAbackLayout);

        qnaBackLayout.setOnClickListener(v -> finish());

        questionId = getIntent().getStringExtra(EXTRA_QUESTION_ID);

        if (questionId == null || questionId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Không tìm thấy mã câu hỏi",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        initViews();
        setupAnswerRecyclerView();
        setupButtons();

        loadQuestion();
        loadAnswers();
    }

    private boolean isActivityActive() {
        return !isFinishing() && !isDestroyed();
    }

    private void initViews() {
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailAuthorDate = findViewById(R.id.tvDetailAuthorDate);
        tvDetailBody = findViewById(R.id.tvDetailBody);
        tvDetailVoteCount = findViewById(R.id.tvDetailVoteCount);
        tvDetailAnswerCount = findViewById(R.id.tvDetailAnswerCount);
        tvDetailAnswersEmpty = findViewById(R.id.tvDetailAnswersEmpty);

        chipGroupDetailCategories =
                findViewById(R.id.chipGroupDetailCategories);

        rvDetailAnswers = findViewById(R.id.rvDetailAnswers);
        pbDetailAnswers = findViewById(R.id.pbDetailAnswers);

        tilDetailAnswer = findViewById(R.id.tilDetailAnswer);
        edtDetailAnswer = findViewById(R.id.edtDetailAnswer);

        btnSubmitAnswer = findViewById(R.id.btnSubmitAnswer);
        tvQuestionUpVote = findViewById(R.id.tvQuestionUpVote);
        tvQuestionDownVote = findViewById(R.id.tvQuestionDownVote);
    }

    private void setupAnswerRecyclerView() {
        rvDetailAnswers.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvDetailAnswers.setNestedScrollingEnabled(false);

        answerAdapter = new AnswerAdapter(
                answerList,
                (answer, value) -> toggleAnswerVote(answer, value)
        );

        rvDetailAnswers.setAdapter(answerAdapter);
    }

    private void setupButtons() {
        btnSubmitAnswer.setOnClickListener(v -> submitAnswer());

        tvQuestionUpVote.setOnClickListener(
                v -> toggleQuestionVote(1)
        );

        tvQuestionDownVote.setOnClickListener(
                v -> toggleQuestionVote(-1)
        );

        loadQuestionVoteState();
    }

    // =========================
    // BÌNH CHỌN CÂU TRẢ LỜI
    // =========================

    private void toggleAnswerVote(Answer answer, int requestedValue) {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập để bình chọn.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (answer == null
                || answer.getId() == null
                || (requestedValue != 1 && requestedValue != -1)) {
            return;
        }

        String userId = user.getUid();
        String answerId = answer.getId();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        com.google.firebase.firestore.DocumentReference answerRef =
                db.collection("answers").document(answerId);

        com.google.firebase.firestore.DocumentReference voteRef =
                db.collection("answerVotes")
                        .document(answerId + "_" + userId);

        db.runTransaction(transaction -> {
            DocumentSnapshot answerSnapshot =
                    transaction.get(answerRef);

            DocumentSnapshot voteSnapshot =
                    transaction.get(voteRef);

            if (!answerSnapshot.exists()) {
                throw new IllegalStateException(
                        "Không tìm thấy câu trả lời."
                );
            }

            Long countValue = answerSnapshot.getLong("voteCount");

            long currentCount =
                    countValue == null ? 0L : countValue;

            Long savedValue = voteSnapshot.getLong("value");

            long oldValue =
                    voteSnapshot.exists() && savedValue != null
                            ? savedValue
                            : 0L;

            long updatedCount;

            if (voteSnapshot.exists()
                    && oldValue == requestedValue) {

                // Nhấn lại cùng lựa chọn: bỏ vote.
                transaction.delete(voteRef);

                updatedCount = currentCount - oldValue;

            } else if (voteSnapshot.exists()) {

                // Đổi UP thành DOWN hoặc DOWN thành UP.
                transaction.update(
                        voteRef,
                        "value",
                        requestedValue
                );

                updatedCount =
                        currentCount + requestedValue - oldValue;

            } else {

                // Tạo lượt bình chọn mới.
                Map<String, Object> voteData = new HashMap<>();

                voteData.put("answerId", answerId);
                voteData.put(
                        "createdAt",
                        FieldValue.serverTimestamp()
                );
                voteData.put("userId", userId);
                voteData.put("value", requestedValue);

                transaction.set(voteRef, voteData);

                updatedCount = currentCount + requestedValue;
            }

            // Tổng điểm có thể âm nếu số DOWN lớn hơn UP.
            transaction.update(
                    answerRef,
                    "voteCount",
                    updatedCount
            );

            return updatedCount;

        }).addOnSuccessListener(updatedCount -> {
            if (!isActivityActive()) {
                return;
            }

            answer.setVoteCount(updatedCount);
            answerAdapter.setAnswers(answerList);

            Toast.makeText(
                    this,
                    "Đã cập nhật bình chọn.",
                    Toast.LENGTH_SHORT
            ).show();

        }).addOnFailureListener(error -> {
            android.util.Log.e(
                    "QNA_ANSWER_VOTE",
                    "Answer vote failed",
                    error
            );

            if (!isActivityActive()) {
                return;
            }

            Toast.makeText(
                    this,
                    "Không thể bình chọn: " + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    // =========================
    // BÌNH CHỌN CÂU HỎI
    // =========================

    private void toggleQuestionVote(int requestedValue) {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || questionId == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập để bình chọn.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String userId = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        com.google.firebase.firestore.DocumentReference questionRef =
                db.collection("questions").document(questionId);

        com.google.firebase.firestore.DocumentReference voteRef =
                db.collection("questionVotes")
                        .document(questionId + "_" + userId);

        db.runTransaction(transaction -> {
            DocumentSnapshot questionSnapshot =
                    transaction.get(questionRef);

            DocumentSnapshot voteSnapshot =
                    transaction.get(voteRef);

            if (!questionSnapshot.exists()) {
                throw new IllegalStateException(
                        "Không tìm thấy câu hỏi."
                );
            }

            Long countValue = questionSnapshot.getLong("voteCount");
            long currentCount =
                    countValue == null ? 0L : countValue;

            Long savedValue = voteSnapshot.getLong("value");

            long oldValue =
                    voteSnapshot.exists() && savedValue != null
                            ? savedValue
                            : 0L;

            long updatedCount;

            if (voteSnapshot.exists()
                    && oldValue == requestedValue) {

                // Nhấn lại cùng lựa chọn: bỏ vote.
                transaction.delete(voteRef);
                updatedCount = currentCount - oldValue;

            } else if (voteSnapshot.exists()) {

                // Đổi UP thành DOWN hoặc DOWN thành UP.
                transaction.update(
                        voteRef,
                        "value",
                        requestedValue
                );

                updatedCount =
                        currentCount + requestedValue - oldValue;

            } else {

                // Tạo vote mới.
                Map<String, Object> voteData = new HashMap<>();
                voteData.put("questionId", questionId);
                voteData.put("userId", userId);
                voteData.put("value", requestedValue);
                voteData.put(
                        "createdAt",
                        FieldValue.serverTimestamp()
                );

                transaction.set(voteRef, voteData);
                updatedCount = currentCount + requestedValue;
            }

            transaction.update(
                    questionRef,
                    "voteCount",
                    updatedCount
            );

            return updatedCount;

        }).addOnSuccessListener(updatedCount -> {
            if (!isActivityActive()) return;

            tvDetailVoteCount.setText(updatedCount + " vote");

            loadQuestionVoteState();

            Intent result = new Intent();
            result.putExtra("questionId", questionId);
            setResult(RESULT_OK, result);

            Toast.makeText(
                    this,
                    "Đã cập nhật bình chọn.",
                    Toast.LENGTH_SHORT
            ).show();

        }).addOnFailureListener(error -> {
            android.util.Log.e(
                    "QNA_VOTE",
                    "Question vote failed",
                    error
            );

            if (!isActivityActive()) return;

            Toast.makeText(
                    this,
                    "Không thể bình chọn: " + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private void loadQuestionVoteState() {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || questionId == null) {
            tvQuestionUpVote.setAlpha(1f);
            tvQuestionDownVote.setAlpha(1f);
            return;
        }

        String userId = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("questionVotes")
                .document(questionId + "_" + userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isActivityActive()) return;

                    Long value = snapshot.getLong("value");

                    long selectedValue =
                            snapshot.exists() && value != null
                                    ? value
                                    : 0L;

                    tvQuestionUpVote.setAlpha(
                            selectedValue == 1L ? 1f : 0.6f
                    );

                    tvQuestionDownVote.setAlpha(
                            selectedValue == -1L ? 1f : 0.6f
                    );
                });
    }

    // =========================
    // TẢI CHI TIẾT CÂU HỎI
    // =========================

    private void loadQuestion() {
        FirebaseFirestore.getInstance()
                .collection("questions")
                .document(questionId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!isActivityActive()) {
                        return;
                    }

                    if (!document.exists()) {
                        Toast.makeText(
                                this,
                                "Câu hỏi không tồn tại",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                        return;
                    }

                    tvDetailTitle.setText(
                            safeString(
                                    document.getString("title")
                            )
                    );

                    tvDetailBody.setText(
                            safeString(
                                    document.getString("content")
                            )
                    );

                    String status =
                            document.getString("status");

                    questionStatus =
                            status == null || status.trim().isEmpty()
                                    ? "OPEN"
                                    : status.trim().toUpperCase(
                                    Locale.ROOT
                            );

                    tvDetailStatus.setText(questionStatus);

                    if ("CLOSED".equals(questionStatus)) {
                        tvDetailStatus.setBackgroundTintList(
                                ColorStateList.valueOf(
                                        Color.parseColor("#E5E7EB")
                                )
                        );

                        tvDetailStatus.setTextColor(
                                Color.parseColor("#475569")
                        );
                    }

                    Timestamp createdAt =
                            document.getTimestamp("createdAt");

                    String authorName =
                            document.getString("authorName");

                    if (authorName == null
                            || authorName.trim().isEmpty()) {

                        authorName = "Người dùng";
                    }

                    String dateText = formatDate(createdAt);

                    tvDetailAuthorDate.setText(
                            dateText.isEmpty()
                                    ? authorName
                                    : authorName + " • " + dateText
                    );

                    Long voteCount =
                            document.getLong("voteCount");

                    tvDetailVoteCount.setText(
                            (voteCount == null ? 0L : voteCount)
                                    + " vote"
                    );

                    loadCategories(document);

                    setAnswerInputEnabled(
                            "OPEN".equals(questionStatus)
                    );
                })
                .addOnFailureListener(error -> {
                    if (!isActivityActive()) {
                        return;
                    }

                    Toast.makeText(
                            this,
                            "Không thể tải câu hỏi",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================
    // HIỂN THỊ DANH MỤC
    // =========================

    private void loadCategories(DocumentSnapshot document) {
        chipGroupDetailCategories.removeAllViews();

        Object categoryValue = document.get("category");

        if (categoryValue instanceof String) {
            addCategoryChip((String) categoryValue);

        } else if (categoryValue instanceof List<?>) {
            for (Object item : (List<?>) categoryValue) {
                if (item instanceof String) {
                    addCategoryChip((String) item);
                }
            }
        }
    }

    private void addCategoryChip(String category) {
        if (category == null || category.trim().isEmpty()) {
            return;
        }

        Chip chip = new Chip(this);

        chip.setText(category.trim());
        chip.setCheckable(false);
        chip.setClickable(false);

        chipGroupDetailCategories.addView(chip);
    }

    // =========================
    // TẢI CÂU TRẢ LỜI
    // =========================

    private void loadAnswers() {
        pbDetailAnswers.setVisibility(View.VISIBLE);
        tvDetailAnswersEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("answers")
                .whereEqualTo("questionId", questionId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isActivityActive()) {
                        return;
                    }

                    answerList.clear();

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {

                        Answer answer = new Answer();

                        answer.setId(document.getId());
                        answer.setQuestionId(questionId);

                        answer.setUserId(
                                readFirstString(
                                        document,
                                        "userId",
                                        "authorId",
                                        "uid"
                                )
                        );

                        answer.setAuthorName(
                                readFirstString(
                                        document,
                                        "authorName",
                                        "username",
                                        "userName"
                                )
                        );

                        answer.setContent(
                                document.getString("content")
                        );

                        answer.setCreatedAt(
                                document.getTimestamp("createdAt")
                        );

                        Long voteCount =
                                document.getLong("voteCount");

                        answer.setVoteCount(
                                voteCount == null ? 0L : voteCount
                        );

                        answerList.add(answer);
                    }

                    answerAdapter.setAnswers(answerList);

                    tvDetailAnswerCount.setText(
                            answerList.size() + " câu trả lời"
                    );

                    tvDetailAnswersEmpty.setVisibility(
                            answerList.isEmpty()
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    rvDetailAnswers.setVisibility(
                            answerList.isEmpty()
                                    ? View.GONE
                                    : View.VISIBLE
                    );

                    pbDetailAnswers.setVisibility(View.GONE);
                })
                .addOnFailureListener(error -> {
                    if (!isActivityActive()) {
                        return;
                    }

                    pbDetailAnswers.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Không thể tải câu trả lời",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================
    // GỬI CÂU TRẢ LỜI
    // =========================

    private void submitAnswer() {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập để trả lời",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String content = edtDetailAnswer.getText() == null
                ? ""
                : edtDetailAnswer.getText()
                .toString()
                .trim();

        if (content.isEmpty()) {
            tilDetailAnswer.setError(
                    "Vui lòng nhập câu trả lời"
            );
            return;
        }

        tilDetailAnswer.setError(null);
        btnSubmitAnswer.setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Kiểm tra lại trạng thái câu hỏi trước khi ghi dữ liệu.
        db.collection("questions")
                .document(questionId)
                .get()
                .addOnSuccessListener(questionDocument -> {
                    if (!isActivityActive()) {
                        return;
                    }

                    String latestStatus =
                            questionDocument.getString("status");

                    if (!questionDocument.exists()
                            || "CLOSED".equalsIgnoreCase(latestStatus)
                            || "DELETED".equalsIgnoreCase(latestStatus)) {

                        questionStatus = latestStatus == null
                                ? "CLOSED"
                                : latestStatus.toUpperCase(
                                Locale.ROOT
                        );

                        setAnswerInputEnabled(false);

                        Toast.makeText(
                                this,
                                "Câu hỏi đã đóng, không nhận câu trả lời mới",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    Map<String, Object> answerData =
                            new HashMap<>();

                    answerData.put("questionId", questionId);
                    answerData.put("userId", user.getUid());
                    answerData.put("content", content);
                    answerData.put(
                            "createdAt",
                            FieldValue.serverTimestamp()
                    );
                    answerData.put("voteCount", 0);

                    db.collection("answers")
                            .add(answerData)
                            .addOnSuccessListener(reference -> {
                                if (!isActivityActive()) {
                                    return;
                                }

                                edtDetailAnswer.setText("");

                                Toast.makeText(
                                        this,
                                        "Đã gửi câu trả lời",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadAnswers();
                            })
                            .addOnFailureListener(error -> {
                                android.util.Log.e(
                                        "QNA_ANSWER",
                                        "Send answer failed",
                                        error
                                );

                                if (!isActivityActive()) {
                                    return;
                                }

                                Toast.makeText(
                                        this,
                                        "Lỗi: " + error.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            })
                            .addOnCompleteListener(task -> {
                                if (!isActivityActive()) {
                                    return;
                                }

                                btnSubmitAnswer.setEnabled(
                                        "OPEN".equals(questionStatus)
                                );
                            });
                })
                .addOnFailureListener(error -> {
                    if (!isActivityActive()) {
                        return;
                    }

                    btnSubmitAnswer.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Không thể kiểm tra trạng thái câu hỏi",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void setAnswerInputEnabled(boolean enabled) {
        tilDetailAnswer.setEnabled(enabled);
        edtDetailAnswer.setEnabled(enabled);
        btnSubmitAnswer.setEnabled(enabled);

        if (!enabled) {
            edtDetailAnswer.setHint(
                    "Câu hỏi đã đóng, không nhận câu trả lời mới"
            );
        } else {
            edtDetailAnswer.setHint(
                    "Nhập câu trả lời..."
            );
        }
    }

    // =========================
    // HÀM HỖ TRỢ
    // =========================

    private String readFirstString(
            DocumentSnapshot document,
            String... fields
    ) {
        for (String field : fields) {
            Object value = document.get(field);

            if (value instanceof String) {
                String text = ((String) value).trim();

                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return "Người dùng";
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        Date date = timestamp.toDate();

        return new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
        ).format(date);
    }
}