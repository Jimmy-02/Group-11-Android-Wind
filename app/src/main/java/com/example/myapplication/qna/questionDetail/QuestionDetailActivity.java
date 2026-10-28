package com.example.myapplication.qna.questionDetail;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentReference;
import com.example.myapplication.R;
import com.example.myapplication.data.local.AnswerLocalDataSource;
import com.example.myapplication.data.local.QuestionLocalDataSource;
import com.example.myapplication.qna.Question;
import com.example.myapplication.qna.report.ReportAnswerActivity;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuestionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "question_id";

    private String questionId;
    private String questionStatus = "OPEN";
    private boolean offlineNoticeShown = false;

    private TextView tvDetailTitle;
    private TextView tvDetailStatus;
    private TextView tvDetailAuthorDate;
    private TextView tvDetailBody;
    private TextView tvDetailVoteCount;
    private TextView tvDetailAnswerCount;
    private TextView tvDetailAnswersEmpty;
    private ImageView ivDetailQuestionImage;
    private ChipGroup chipGroupDetailCategories;

    private RecyclerView rvDetailAnswers;
    private ProgressBar pbDetailAnswers;

    private TextInputLayout tilDetailAnswer;
    private TextInputEditText edtDetailAnswer;

    private Button btnSubmitAnswer;
    private TextView tvQuestionUpVote;
    private TextView tvQuestionDownVote;

    private AnswerAdapter answerAdapter;
    private ListenerRegistration questionListener;
    private ListenerRegistration answersListener;
    private final List<Answer> answerList = new ArrayList<>();

    private QuestionLocalDataSource questionLocalDataSource;
    private AnswerLocalDataSource answerLocalDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_question_detail);

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

        View qnaBackLayout = findViewById(R.id.QNAbackLayout);

        qnaBackLayout.setOnClickListener(v -> finish());

        questionId = getIntent().getStringExtra(EXTRA_QUESTION_ID);

        if (questionId == null || questionId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã câu hỏi", Toast.LENGTH_SHORT).show();

            finish();
            return;
        }

        questionLocalDataSource = new QuestionLocalDataSource(this);
        answerLocalDataSource = new AnswerLocalDataSource(this);

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
        ivDetailQuestionImage = findViewById(R.id.ivDetailQuestionImage);
        chipGroupDetailCategories = findViewById(R.id.chipGroupDetailCategories);
        rvDetailAnswers = findViewById(R.id.rvDetailAnswers);
        pbDetailAnswers = findViewById(R.id.pbDetailAnswers);
        tilDetailAnswer = findViewById(R.id.tilDetailAnswer);
        edtDetailAnswer = findViewById(R.id.edtDetailAnswer);
        btnSubmitAnswer = findViewById(R.id.btnSubmitAnswer);
        tvQuestionUpVote = findViewById(R.id.tvQuestionUpVote);
        tvQuestionDownVote = findViewById(R.id.tvQuestionDownVote);
    }

    private void setupAnswerRecyclerView() {
        rvDetailAnswers.setLayoutManager(new LinearLayoutManager(this));

        rvDetailAnswers.setNestedScrollingEnabled(false);

        answerAdapter = new AnswerAdapter(answerList, (answer, value) -> toggleAnswerVote(answer, value), answer -> openAnswerReport(answer));

        rvDetailAnswers.setAdapter(answerAdapter);
    }

    private void setupButtons() {
        btnSubmitAnswer.setOnClickListener(v -> submitAnswer());

        tvQuestionUpVote.setOnClickListener(v -> toggleQuestionVote(1));

        tvQuestionDownVote.setOnClickListener(v -> toggleQuestionVote(-1));

        loadQuestionVoteState();
    }

    private void openAnswerReport(Answer answer) {
        if (answer == null || answer.getId() == null || answer.getId().trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy câu trả lời cần báo cáo.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ReportAnswerActivity.class);

        intent.putExtra(ReportAnswerActivity.EXTRA_ANSWER_ID, answer.getId());

        intent.putExtra(ReportAnswerActivity.EXTRA_CONTENT, answer.getContent());

        startActivity(intent);
    }

    private void sortAnswersByVotes() {
        Collections.sort(answerList, (a, b) -> Long.compare(b.getVoteCount(), a.getVoteCount()));
    }

    private void showOfflineNotice() {
        if (offlineNoticeShown) {
            return;
        }

        offlineNoticeShown = true;
        Toast.makeText(this, "Đang hiển thị dữ liệu đã lưu offline.", Toast.LENGTH_SHORT).show();
    }

    private void toggleAnswerVote(Answer answer, int requestedValue) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình chọn.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (answer == null || answer.getId() == null || (requestedValue != 1 && requestedValue != -1)) {
            return;
        }

        String userId = user.getUid();
        String answerId = answer.getId();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        com.google.firebase.firestore.DocumentReference answerRef = db.collection("answers").document(answerId);

        com.google.firebase.firestore.DocumentReference voteRef = db.collection("answerVotes").document(answerId + "_" + userId);

        db.runTransaction(transaction -> {
            DocumentSnapshot answerSnapshot = transaction.get(answerRef);

            DocumentSnapshot voteSnapshot = transaction.get(voteRef);

            if (!answerSnapshot.exists()) {
                throw new IllegalStateException("Không tìm thấy câu trả lời.");
            }

            Long countValue = answerSnapshot.getLong("voteCount");

            long currentCount = countValue == null ? 0L : countValue;

            Long savedValue = voteSnapshot.getLong("value");

            long oldValue = voteSnapshot.exists() && savedValue != null ? savedValue : 0L;

            long updatedCount;

            if (voteSnapshot.exists() && oldValue == requestedValue) {

                // Đã chọn cùng chiều vote: giữ nguyên vote hiện tại.
                return currentCount;

            } else if (voteSnapshot.exists()) {

                // Đổi UP thành DOWN hoặc DOWN thành UP.
                transaction.update(voteRef, "value", requestedValue);

                updatedCount = currentCount + requestedValue - oldValue;

            } else {

                // Tạo lượt bình chọn mới.
                Map<String, Object> voteData = new HashMap<>();

                voteData.put("answerId", answerId);
                voteData.put("createdAt", FieldValue.serverTimestamp());
                voteData.put("userId", userId);
                voteData.put("value", requestedValue);

                transaction.set(voteRef, voteData);

                updatedCount = currentCount + requestedValue;
            }

            // Tổng điểm có thể âm nếu số DOWN lớn hơn UP.
            transaction.update(answerRef, "voteCount", updatedCount);

            return updatedCount;

        }).addOnSuccessListener(updatedCount -> {
            if (!isActivityActive()) {
                return;
            }

            answer.setVoteCount(updatedCount);

            sortAnswersByVotes();
            answerAdapter.setAnswers(answerList);

            Toast.makeText(this, "Đã cập nhật bình chọn.", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(error -> {
            Log.e("QNA_ANSWER_VOTE", "Answer vote failed", error);

            if (!isActivityActive()) {
                return;
            }

            Toast.makeText(this, "Không thể bình chọn: " + error.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void toggleQuestionVote(int requestedValue) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || questionId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình chọn.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        com.google.firebase.firestore.DocumentReference questionRef = db.collection("questions").document(questionId);

        com.google.firebase.firestore.DocumentReference voteRef = db.collection("questionVotes").document(questionId + "_" + userId);

        db.runTransaction(transaction -> {
            DocumentSnapshot questionSnapshot = transaction.get(questionRef);

            DocumentSnapshot voteSnapshot = transaction.get(voteRef);

            if (!questionSnapshot.exists()) {
                throw new IllegalStateException("Không tìm thấy câu hỏi.");
            }

            Long countValue = questionSnapshot.getLong("voteCount");
            long currentCount = countValue == null ? 0L : countValue;

            Long savedValue = voteSnapshot.getLong("value");

            long oldValue = voteSnapshot.exists() && savedValue != null ? savedValue : 0L;

            long updatedCount;

            if (voteSnapshot.exists() && oldValue == requestedValue) {

                // Đã chọn cùng chiều vote: giữ nguyên vote hiện tại.
                return currentCount;

            } else if (voteSnapshot.exists()) {

                // Đổi UP thành DOWN hoặc DOWN thành UP.
                transaction.update(voteRef, "value", requestedValue);

                updatedCount = currentCount + requestedValue - oldValue;

            } else {

                // Tạo vote mới.
                Map<String, Object> voteData = new HashMap<>();
                voteData.put("questionId", questionId);
                voteData.put("userId", userId);
                voteData.put("value", requestedValue);
                voteData.put("createdAt", FieldValue.serverTimestamp());

                transaction.set(voteRef, voteData);
                updatedCount = currentCount + requestedValue;
            }

            transaction.update(questionRef, "voteCount", updatedCount);

            return updatedCount;

        }).addOnSuccessListener(updatedCount -> {
            if (!isActivityActive()) return;

            tvDetailVoteCount.setText(updatedCount + " vote");

            loadQuestionVoteState();

            Intent result = new Intent();
            result.putExtra("questionId", questionId);
            setResult(RESULT_OK, result);

            Toast.makeText(this, "Đã cập nhật bình chọn.", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(error -> {
            Log.e("QNA_VOTE", "Question vote failed", error);

            if (!isActivityActive()) return;

            Toast.makeText(this, "Không thể bình chọn: " + error.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void loadQuestionVoteState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || questionId == null) {
            tvQuestionUpVote.setAlpha(1f);
            tvQuestionDownVote.setAlpha(1f);
            return;
        }

        String userId = user.getUid();

        FirebaseFirestore.getInstance().collection("questionVotes").document(questionId + "_" + userId).get().addOnSuccessListener(snapshot -> {
            if (!isActivityActive()) return;

            Long value = snapshot.getLong("value");

            long selectedValue = snapshot.exists() && value != null ? value : 0L;

            tvQuestionUpVote.setAlpha(selectedValue == 1L ? 1f : 0.6f);

            tvQuestionDownVote.setAlpha(selectedValue == -1L ? 1f : 0.6f);
        });
    }

    private void loadQuestion() {
        questionListener = FirebaseFirestore.getInstance().collection("questions").document(questionId).addSnapshotListener((document, error) -> {

            if (!isActivityActive()) {
                return;
            }

            if (error != null) {
                Log.e("QNA_DETAIL", "Không thể tải câu hỏi", error);
                showCachedQuestion();
                return;
            }

            if (document == null) {
                return;
            }

            if (!document.exists()) {
                // Offline: tài liệu không có trong cache không có nghĩa là câu hỏi đã bị xóa.
                if (document.getMetadata().isFromCache()) {
                    showCachedQuestion();
                    return;
                }

                Toast.makeText(this, "Câu hỏi không tồn tại", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String status = document.getString("status");

            if ("DELETED".equalsIgnoreCase(status) || "DELETE".equalsIgnoreCase(status) || document.get("deletedAt") != null) {

                Toast.makeText(this, "Câu hỏi đã bị xóa", Toast.LENGTH_SHORT).show();

                finish();
                return;
            }

            offlineNoticeShown = false;

            Question question = mapQuestion(document, status);

            bindQuestion(question);
            saveQuestionToCache(question);
        });
    }

    private Question mapQuestion(DocumentSnapshot document, String status) {
        Question question = new Question();

        question.setId(document.getId());
        question.setTitle(document.getString("title"));
        question.setContent(document.getString("content"));
        question.setUserId(document.getString("userId"));
        question.setAuthorName(document.getString("authorName"));
        question.setAuthorAvatar(document.getString("authorAvatar"));
        question.setImageUrl(document.getString("imageUrl"));
        question.setStatus(status == null || status.trim().isEmpty() ? "OPEN" : status.trim().toUpperCase(Locale.ROOT));
        question.setCategories(readCategories(document));
        question.setCreatedAt(document.getTimestamp("createdAt"));

        Long voteCount = document.getLong("voteCount");
        question.setVoteCount(voteCount == null ? 0L : voteCount);

        return question;
    }

    private List<String> readCategories(DocumentSnapshot document) {
        List<String> categories = new ArrayList<>();

        Object categoryValue = document.get("category");

        if (categoryValue == null) {
            categoryValue = document.get("categories");
        }

        if (categoryValue instanceof String) {
            categories.add((String) categoryValue);
        } else if (categoryValue instanceof List<?>) {
            for (Object item : (List<?>) categoryValue) {
                if (item instanceof String) {
                    categories.add((String) item);
                }
            }
        }

        return categories;
    }

    private void bindQuestion(Question question) {
        tvDetailTitle.setText(safeString(question.getTitle()));

        tvDetailBody.setText(safeString(question.getContent()));

        String imageUrl = question.getImageUrl();

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            ivDetailQuestionImage.setVisibility(View.VISIBLE);

            Picasso.get().load(imageUrl.trim()).fit().centerCrop().into(ivDetailQuestionImage);
        } else {
            Picasso.get().cancelRequest(ivDetailQuestionImage);
            ivDetailQuestionImage.setImageDrawable(null);
            ivDetailQuestionImage.setVisibility(View.GONE);
        }

        String status = question.getStatus();

        questionStatus = status == null || status.trim().isEmpty() ? "OPEN" : status.trim().toUpperCase(Locale.ROOT);

        tvDetailStatus.setText(questionStatus);

        boolean isClosed = "CLOSED".equals(questionStatus);

        tvDetailStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isClosed ? "#E5E7EB" : "#DCFCE7")));

        tvDetailStatus.setTextColor(Color.parseColor(isClosed ? "#475569" : "#166534"));

        String authorName = question.getAuthorName();

        if (authorName == null || authorName.trim().isEmpty()) {
            authorName = "Người dùng";
        }

        String dateText = formatDate(question.getCreatedAt());

        tvDetailAuthorDate.setText(dateText.isEmpty() ? authorName : authorName + " • " + dateText);

        tvDetailVoteCount.setText(question.getVoteCount() + " vote");

        chipGroupDetailCategories.removeAllViews();

        if (question.getCategories() != null) {
            for (String category : question.getCategories()) {
                addCategoryChip(category);
            }
        }

        setAnswerInputEnabled("OPEN".equals(questionStatus));
    }

    private void saveQuestionToCache(Question question) {
        try {
            // Giữ lại số câu trả lời đã tính trước đó, vì document câu hỏi có thể không có trường này.
            Question cachedQuestion = questionLocalDataSource.getQuestionById(questionId);

            if (cachedQuestion != null) {
                question.setAnswerCount(cachedQuestion.getAnswerCount());
            }

            questionLocalDataSource.saveQuestions(Collections.singletonList(question));
        } catch (Exception e) {
            Log.e("QNA_DETAIL", "Không thể lưu câu hỏi vào SQLite", e);
        }
    }

    private void showCachedQuestion() {
        Question cachedQuestion = null;

        try {
            cachedQuestion = questionLocalDataSource.getQuestionById(questionId);
        } catch (Exception e) {
            Log.e("QNA_DETAIL", "Không thể đọc câu hỏi từ SQLite", e);
        }

        if (cachedQuestion == null) {
            Toast.makeText(this, "Câu hỏi này chưa được lưu để xem offline.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindQuestion(cachedQuestion);
        showOfflineNotice();
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
    private void syncAnswerCountToFirestore(int actualCount) {
        FirebaseFirestore.getInstance()
                .collection("questions")
                .document(questionId)
                .update("answerCount", actualCount)
                .addOnSuccessListener(unused ->
                        Log.d("QNA_DETAIL", "Đã đồng bộ answerCount: " + actualCount))
                .addOnFailureListener(e ->
                        Log.e("QNA_DETAIL", "Không thể đồng bộ answerCount", e));
    }

    private void loadAnswers() {
        pbDetailAnswers.setVisibility(View.VISIBLE);
        tvDetailAnswersEmpty.setVisibility(View.GONE);

        if (answersListener != null) {
            answersListener.remove();
            answersListener = null;
        }

        answersListener = FirebaseFirestore.getInstance().collection("answers")
                .whereEqualTo("questionId", questionId)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isActivityActive()) {
                        return;
                    }

                    if (error != null) {
                        Log.e("QNA_DETAIL", "Không thể tải câu trả lời", error);
                        showCachedAnswers();
                        return;
                    }

                    if (snapshot == null) {
                        pbDetailAnswers.setVisibility(View.GONE);
                        return;
                    }

                    // Snapshot từ cache Firestore không được ghi đè dữ liệu SQLite.
                    if (snapshot.getMetadata().isFromCache()) {
                        List<Answer> cachedAnswers = answerLocalDataSource.getAnswersOfQuestion(questionId);

                        if (!cachedAnswers.isEmpty()) {
                            answerList.clear();
                            answerList.addAll(cachedAnswers);
                            renderAnswers();
                        } else if (!snapshot.isEmpty()) {
                            List<Answer> firestoreCachedAnswers = new ArrayList<>();

                            for (DocumentSnapshot document : snapshot.getDocuments()) {
                                String status = document.getString("status");

                                if ("DELETED".equalsIgnoreCase(status)
                                        || "DELETE".equalsIgnoreCase(status)
                                        || document.get("deletedAt") != null) {
                                    continue;
                                }

                                firestoreCachedAnswers.add(mapAnswer(document));
                            }

                            answerList.clear();
                            answerList.addAll(firestoreCachedAnswers);
                            renderAnswers();
                        } else {
                            showCachedAnswers();
                        }

                        pbDetailAnswers.setVisibility(View.GONE);
                        return;
                    }

                    // Snapshot từ server: lọc dữ liệu đã xóa.
                    List<Answer> loadedAnswers = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String status = document.getString("status");

                        if ("DELETED".equalsIgnoreCase(status)
                                || "DELETE".equalsIgnoreCase(status)
                                || document.get("deletedAt") != null) {
                            continue;
                        }

                        Answer answer = mapAnswer(document);

                        if (answer != null) {
                            loadedAnswers.add(answer);
                        }
                    }

                    answerList.clear();
                    answerList.addAll(loadedAnswers);

                    saveAnswersToCache(loadedAnswers);
                    renderAnswers();

                    // Đồng bộ số lượng thực tế lên Firestore.
                   // syncAnswerCountToFirestore(loadedAnswers.size());

                    pbDetailAnswers.setVisibility(View.GONE);
                });
    }

    private Answer mapAnswer(DocumentSnapshot document) {
        Answer answer = new Answer();

        answer.setId(document.getId());
        answer.setQuestionId(questionId);
        answer.setUserId(readFirstString(document, "userId", "authorId", "uid"));
        answer.setAuthorName(readFirstString(document, "authorName", "username", "userName"));
        answer.setAuthorAvatar(document.getString("authorAvatar"));
        answer.setContent(document.getString("content"));
        answer.setCreatedAt(document.getTimestamp("createdAt"));

        Long voteCount = document.getLong("voteCount");
        answer.setVoteCount(voteCount == null ? 0L : voteCount);

        return answer;
    }

    private void renderAnswers() {
        sortAnswersByVotes();
        answerAdapter.setAnswers(answerList);

        tvDetailAnswerCount.setText(answerList.size() + " câu trả lời");

        boolean isEmpty = answerList.isEmpty();

        tvDetailAnswersEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        rvDetailAnswers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        pbDetailAnswers.setVisibility(View.GONE);
    }

    private void saveAnswersToCache(List<Answer> answers) {
        try {
            answerLocalDataSource.replaceAnswersOfQuestion(questionId, answers);
            questionLocalDataSource.updateAnswerCount(questionId, answers.size());

            Log.d("OfflineCache", "Saved " + answerLocalDataSource.getAnswersOfQuestion(questionId).size() + " answers for question " + questionId);
        } catch (Exception e) {
            Log.e("QNA_DETAIL", "Không thể lưu câu trả lời vào SQLite", e);
        }
    }

    private void showCachedAnswers() {
        try {
            List<Answer> cachedAnswers = answerLocalDataSource.getAnswersOfQuestion(questionId);

            Log.d("OfflineCache", "Answers cached for question: " + cachedAnswers.size());

            answerList.clear();
            answerList.addAll(cachedAnswers);
        } catch (Exception e) {
            Log.e("QNA_DETAIL", "Không thể đọc câu trả lời từ SQLite", e);
        }

        renderAnswers();
        showOfflineNotice();
    }

    private void submitAnswer() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để trả lời", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = edtDetailAnswer.getText() == null ? "" : edtDetailAnswer.getText().toString().trim();

        if (content.isEmpty()) {
            tilDetailAnswer.setError("Vui lòng nhập câu trả lời");
            return;
        }

        tilDetailAnswer.setError(null);
        btnSubmitAnswer.setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Kiểm tra trạng thái Question trước khi gửi Answer.
        db.collection("questions").document(questionId).get().addOnSuccessListener(questionDocument -> {
            if (!isActivityActive()) {
                return;
            }

            String latestStatus = questionDocument.getString("status");

            if (!questionDocument.exists() || "CLOSED".equalsIgnoreCase(latestStatus) || "DELETED".equalsIgnoreCase(latestStatus)) {

                questionStatus = latestStatus == null ? "CLOSED" : latestStatus.toUpperCase(Locale.ROOT);

                setAnswerInputEnabled(false);

                Toast.makeText(this, "Câu hỏi đã đóng, không nhận câu trả lời mới", Toast.LENGTH_LONG).show();

                return;
            }

            // Lấy thông tin hồ sơ người trả lời giống Discussion.
            db.collection("users").document(user.getUid()).get().addOnSuccessListener(userDocument -> {
                if (!isActivityActive()) {
                    return;
                }

                saveAnswer(db, user, userDocument, content);
            }).addOnFailureListener(error -> {
                Log.e("QNA_ANSWER", "Không thể tải hồ sơ người dùng", error);

                if (!isActivityActive()) {
                    return;
                }

                // Vẫn cho phép trả lời nếu không tải được hồ sơ.
                saveAnswer(db, user, null, content);
            });
        }).addOnFailureListener(error -> {
            if (!isActivityActive()) {
                return;
            }

            btnSubmitAnswer.setEnabled(true);

            Toast.makeText(this, "Không thể kiểm tra trạng thái câu hỏi", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveAnswer(FirebaseFirestore db, FirebaseUser user, DocumentSnapshot userDocument, String content) {
        if (!isActivityActive()) {
            return;
        }

        Map<String, Object> answerData = new HashMap<>();
        answerData.put("questionId", questionId);
        answerData.put("userId", user.getUid());
        answerData.put("authorName", resolveAnswerAuthorName(user, userDocument));
        answerData.put("authorAvatar", resolveAnswerAuthorAvatar(user, userDocument));
        answerData.put("content", content);
        answerData.put("status", "ACTIVE");
        answerData.put("createdAt", FieldValue.serverTimestamp());
        answerData.put("voteCount", 0);

        DocumentReference questionRef = db.collection("questions").document(questionId);

        db.collection("answers").add(answerData)
                .addOnSuccessListener(reference -> {
                    if (!isActivityActive()) {
                        return;
                    }

                    db.collection("answers")
                            .whereEqualTo("questionId", questionId)
                            .whereEqualTo("status", "ACTIVE")
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (!isActivityActive()) {
                                    return;
                                }

                                int activeCount = 0;

                                for (DocumentSnapshot document : snapshot.getDocuments()) {
                                    if (document.get("deletedAt") == null) {
                                        activeCount++;
                                    }
                                }

                                questionRef.update("answerCount", activeCount)
                                        .addOnSuccessListener(unused -> {
                                            if (!isActivityActive()) {
                                                return;
                                            }

                                            edtDetailAnswer.setText("");
                                            Toast.makeText(this, "Đã gửi câu trả lời", Toast.LENGTH_SHORT).show();

                                            loadAnswers();

                                            Intent result = new Intent();
                                            result.putExtra("questionId", questionId);
                                            setResult(RESULT_OK, result);
                                        })
                                        .addOnFailureListener(error -> {
                                            Log.e("QNA_ANSWER", "Không thể cập nhật answerCount", error);

                                            if (isActivityActive()) {
                                                Toast.makeText(this, "Câu trả lời đã gửi nhưng chưa cập nhật được số lượng.", Toast.LENGTH_LONG).show();
                                                loadAnswers();
                                            }
                                        });
                            })
                            .addOnFailureListener(error -> {
                                Log.e("QNA_ANSWER", "Không thể đếm câu trả lời", error);

                                if (isActivityActive()) {
                                    Toast.makeText(this, "Câu trả lời đã gửi nhưng chưa thể đồng bộ số lượng.", Toast.LENGTH_LONG).show();
                                    loadAnswers();
                                }
                            });
                })
                .addOnFailureListener(error -> {
                    Log.e("QNA_ANSWER", "Send answer failed", error);

                    if (isActivityActive()) {
                        Toast.makeText(this, "Không thể gửi câu trả lời: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnCompleteListener(task -> {
                    if (isActivityActive()) {
                        btnSubmitAnswer.setEnabled("OPEN".equals(questionStatus));
                    }
                });
    }

    private String resolveAnswerAuthorName(FirebaseUser user, DocumentSnapshot userDocument) {
        if (userDocument != null && userDocument.exists()) {
            String fullName = userDocument.getString("fullName");

            if (fullName != null && !fullName.trim().isEmpty()) {
                return fullName.trim();
            }

            String username = userDocument.getString("username");

            if (username != null && !username.trim().isEmpty()) {
                return username.trim();
            }
        }

        String displayName = user.getDisplayName();

        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.trim();
        }

        return "Người dùng";
    }

    private String resolveAnswerAuthorAvatar(FirebaseUser user, DocumentSnapshot userDocument) {
        if (userDocument != null && userDocument.exists()) {
            String avatar = userDocument.getString("avatar");

            if (avatar != null && !avatar.trim().isEmpty()) {
                return avatar.trim();
            }
        }

        if (user.getPhotoUrl() != null) {
            return user.getPhotoUrl().toString();
        }

        return "";
    }

    private void setAnswerInputEnabled(boolean enabled) {
        tilDetailAnswer.setEnabled(enabled);
        edtDetailAnswer.setEnabled(enabled);
        btnSubmitAnswer.setEnabled(enabled);

        if (!enabled) {
            edtDetailAnswer.setHint("Câu hỏi đã đóng, không nhận câu trả lời mới");
        } else {
            edtDetailAnswer.setHint("Nhập câu trả lời...");
        }
    }

    private String readFirstString(DocumentSnapshot document, String... fields) {
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

        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
    }

    @Override
    protected void onDestroy() {
        if (questionListener != null) {
            questionListener.remove();
            questionListener = null;
        }

        if (answersListener != null) {
            answersListener.remove();
            answersListener = null;
        }

        if (questionLocalDataSource != null) {
            questionLocalDataSource.close();
        }

        if (answerLocalDataSource != null) {
            answerLocalDataSource.close();
        }

        super.onDestroy();
    }
}