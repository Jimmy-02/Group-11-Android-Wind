
package com.example.myapplication.account.myqna;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.qna.Question;
import com.example.myapplication.qna.QuestionAdapter;
import com.example.myapplication.qna.questionDetail.Answer;
import com.example.myapplication.qna.questionDetail.AnswerViewHolder;
import com.example.myapplication.qna.questionDetail.QuestionDetailActivity;
import com.example.myapplication.qna.report.ReportQuestionActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyQnaActivity extends AppCompatActivity {

    private static final String TAG = "MyQnaActivity";

    private ProgressBar progressBar;
    private TextView tvSummary;
    private TextView tvEmpty;
    private RecyclerView rvMyQna;
    private TabLayout tabMyQna;

    private QuestionAdapter questionAdapter;
    private MyAnswersAdapter answerAdapter;

    private final List<Question> myQuestions = new ArrayList<>();
    private final List<Answer> myAnswers = new ArrayList<>();

    private final Map<String, String> questionTitles = new HashMap<>();

    private String currentUserId;
    private int selectedTab = 0;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_qna);

        progressBar = findViewById(R.id.pbMyQna);
        tvSummary = findViewById(R.id.tvMyQnaSummary);
        tvEmpty = findViewById(R.id.tvMyQnaEmpty);
        rvMyQna = findViewById(R.id.rvMyQna);
        tabMyQna = findViewById(R.id.tabMyQna);

        findViewById(R.id.myQnaBackLayout)
                .setOnClickListener(v -> finish());

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập để xem Q&A của bạn.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        currentUserId = user.getUid();

        rvMyQna.setLayoutManager(
                new LinearLayoutManager(this)
        );

        setupQuestionAdapter();
        answerAdapter = new MyAnswersAdapter();

        rvMyQna.setAdapter(questionAdapter);

        tabMyQna.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(
                            TabLayout.Tab tab) {
                        selectedTab = tab.getPosition();
                        showSelectedTab();
                    }

                    @Override
                    public void onTabUnselected(
                            TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(
                            TabLayout.Tab tab) {
                    }
                }
        );

        loadMyQna();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentUserId != null
                && !isLoading
                && rvMyQna != null) {
            loadMyQna();
        }
    }

    private void setupQuestionAdapter() {
        questionAdapter = new QuestionAdapter(
                new ArrayList<>(),
                currentUserId,
                new QuestionAdapter.OnQuestionActionListener() {

                    @Override
                    public void onQuestionClick(Question question) {
                        openQuestion(question.getId());
                    }

                    @Override
                    public void onReportClick(Question question) {
                        Intent intent = new Intent(
                                MyQnaActivity.this,
                                ReportQuestionActivity.class
                        );

                        intent.putExtra(
                                ReportQuestionActivity.EXTRA_QUESTION_ID,
                                question.getId()
                        );

                        intent.putExtra(
                                ReportQuestionActivity.EXTRA_TITLE,
                                question.getTitle()
                        );

                        intent.putExtra(
                                ReportQuestionActivity.EXTRA_CONTENT,
                                question.getContent()
                        );

                        startActivity(intent);
                    }

                    @Override
                    public void onCloseClick(Question question) {
                        confirmCloseQuestion(question);
                    }

                    @Override
                    public void onVoteClick(
                            Question question,
                            int value) {
                        toggleQuestionVote(question, value);
                    }
                }
        );
    }

    private void loadMyQna() {
        if (isLoading || currentUserId == null) {
            return;
        }

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        rvMyQna.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Đọc câu hỏi và lọc theo chủ sở hữu.
        db.collection("questions")
                .get()
                .addOnSuccessListener(questionSnapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        isLoading = false;
                        return;
                    }

                    myQuestions.clear();
                    questionTitles.clear();

                    for (DocumentSnapshot doc :
                            questionSnapshot.getDocuments()) {

                        String title = doc.getString("title");

                        if (title != null) {
                            questionTitles.put(doc.getId(), title);
                        }

                        String ownerId = readFirstString(
                                doc,
                                "userId",
                                "authorId",
                                "uid"
                        );

                        if (!currentUserId.equals(ownerId)) {
                            continue;
                        }

                        String status = doc.getString("status");

                        if ("DELETED".equalsIgnoreCase(status)) {
                            continue;
                        }

                        Question question = new Question();

                        question.setId(doc.getId());
                        question.setTitle(title);
                        question.setContent(doc.getString("content"));
                        question.setImageUrl(doc.getString("imageUrl"));
                        question.setUserId(ownerId);

                        question.setAuthorName(
                                readFirstString(
                                        doc,
                                        "authorName",
                                        "userName",
                                        "username"
                                )
                        );

                        question.setAuthorAvatar(
                                doc.getString("authorAvatar")
                        );

                        question.setStatus(
                                status == null || status.trim().isEmpty()
                                        ? "OPEN"
                                        : status.toUpperCase(Locale.ROOT)
                        );

                        question.setCreatedAt(
                                doc.getTimestamp("createdAt")
                        );

                        Object voteValue = doc.get("voteCount");

                        question.setVoteCount(
                                voteValue instanceof Number
                                        ? ((Number) voteValue).longValue()
                                        : 0L
                        );

                        // Số câu trả lời sẽ được tính từ collection answers.
                        question.setAnswerCount(0);

                        Object categoryValue = doc.get("category");

                        List<String> categories = new ArrayList<>();

                        if (categoryValue instanceof String) {
                            categories.add((String) categoryValue);

                        } else if (categoryValue instanceof List<?>) {
                            for (Object item :
                                    (List<?>) categoryValue) {

                                if (item instanceof String) {
                                    categories.add((String) item);
                                }
                            }
                        }

                        question.setCategories(categories);
                        myQuestions.add(question);
                    }

                    sortQuestions();

                    // Tải câu trả lời để vừa lấy câu trả lời của tôi,
                    // vừa tính số câu trả lời của mỗi câu hỏi.
                    loadMyAnswersAndAnswerCounts(db);
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        isLoading = false;
                        return;
                    }

                    handleLoadError(error);
                });
    }

    private void loadMyAnswersAndAnswerCounts(
            FirebaseFirestore db) {

        db.collection("answers")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        isLoading = false;
                        return;
                    }

                    myAnswers.clear();

                    Map<String, Integer> answerCounts =
                            new HashMap<>();

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        String questionId =
                                doc.getString("questionId");

                        if (questionId != null) {
                            Integer oldCount =
                                    answerCounts.get(questionId);

                            answerCounts.put(
                                    questionId,
                                    oldCount == null ? 1 : oldCount + 1
                            );
                        }

                        String ownerId = readFirstString(
                                doc,
                                "userId",
                                "authorId",
                                "uid"
                        );

                        if (!currentUserId.equals(ownerId)) {
                            continue;
                        }

                        Answer answer = new Answer();

                        answer.setId(doc.getId());
                        answer.setQuestionId(questionId);
                        answer.setUserId(ownerId);

                        answer.setAuthorName(
                                readFirstString(
                                        doc,
                                        "authorName",
                                        "username",
                                        "userName"
                                )
                        );

                        answer.setAuthorAvatar(
                                doc.getString("authorAvatar")
                        );

                        answer.setContent(doc.getString("content"));

                        answer.setCreatedAt(
                                doc.getTimestamp("createdAt")
                        );

                        Object voteValue = doc.get("voteCount");

                        answer.setVoteCount(
                                voteValue instanceof Number
                                        ? ((Number) voteValue).longValue()
                                        : 0L
                        );

                        myAnswers.add(answer);
                    }

                    // Cập nhật số câu trả lời trên từng câu hỏi.
                    for (Question question : myQuestions) {
                        Integer count =
                                answerCounts.get(question.getId());

                        question.setAnswerCount(
                                count == null ? 0 : count
                        );
                    }

                    sortAnswers();

                    progressBar.setVisibility(View.GONE);
                    isLoading = false;

                    questionAdapter.setQuestions(myQuestions);
                    answerAdapter.setAnswers(myAnswers);

                    tvSummary.setText(
                            myQuestions.size() + " câu hỏi · "
                                    + myAnswers.size() + " câu trả lời"
                    );

                    showSelectedTab();
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        isLoading = false;
                        return;
                    }

                    handleLoadError(error);
                });
    }

    private void showSelectedTab() {
        if (selectedTab == 1) {
            rvMyQna.setAdapter(answerAdapter);

            if (myAnswers.isEmpty()) {
                tvEmpty.setText("Bạn chưa có câu trả lời nào.");
                tvEmpty.setVisibility(View.VISIBLE);
                rvMyQna.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvMyQna.setVisibility(View.VISIBLE);
            }

        } else {
            rvMyQna.setAdapter(questionAdapter);

            if (myQuestions.isEmpty()) {
                tvEmpty.setText("Bạn chưa có câu hỏi nào.");
                tvEmpty.setVisibility(View.VISIBLE);
                rvMyQna.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvMyQna.setVisibility(View.VISIBLE);
            }
        }
    }

    private void openQuestion(String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Không tìm thấy mã câu hỏi.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent = new Intent(
                this,
                QuestionDetailActivity.class
        );

        intent.putExtra(
                QuestionDetailActivity.EXTRA_QUESTION_ID,
                questionId
        );

        startActivity(intent);
    }

    private void confirmCloseQuestion(Question question) {
        if (question == null || question.getId() == null) return;
        String currentStatus = question.getStatus() != null ? question.getStatus().toUpperCase(Locale.ROOT) : "OPEN";
        boolean isOpen = "OPEN".equals(currentStatus);

        String[] options;
        if (isOpen) {
            options = new String[]{"Xóa câu hỏi", "Đóng câu hỏi (Ngừng nhận trả lời)"};
        } else {
            options = new String[]{"Xóa câu hỏi", "Mở lại câu hỏi"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Tùy chọn câu hỏi")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        promptDeleteQuestion(question);
                    } else if (which == 1) {
                        toggleQuestionCloseStatus(question, isOpen ? "CLOSED" : "OPEN");
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void promptDeleteQuestion(Question question) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa câu hỏi")
                .setMessage("Bạn có chắc chắn muốn xóa câu hỏi này không? Câu hỏi sẽ bị xóa vĩnh viễn khỏi danh sách.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "DELETED");
                    updates.put("deletedAt", FieldValue.serverTimestamp());

                    FirebaseFirestore.getInstance()
                            .collection("questions")
                            .document(question.getId())
                            .update(updates)
                            .addOnSuccessListener(unused -> {
                                question.setStatus("DELETED");
                                myQuestions.remove(question);
                                questionAdapter.setQuestions(myQuestions);
                                tvSummary.setText(myQuestions.size() + " câu hỏi · " + myAnswers.size() + " câu trả lời");
                                showSelectedTab();
                                Toast.makeText(this, "Đã xóa câu hỏi", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(error -> {
                                Toast.makeText(this, "Không thể xóa câu hỏi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void toggleQuestionCloseStatus(Question question, String targetStatus) {
        FirebaseFirestore.getInstance()
                .collection("questions")
                .document(question.getId())
                .update("status", targetStatus)
                .addOnSuccessListener(unused -> {
                    question.setStatus(targetStatus);
                    questionAdapter.notifyDataSetChanged();
                    String msg = "CLOSED".equals(targetStatus) ? "Đã đóng câu hỏi" : "Đã mở lại câu hỏi";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(this, "Thao tác thất bại: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleQuestionVote(
            Question question,
            int requestedValue) {

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

        String questionId = question.getId();
        String userId = user.getUid();

        if (questionId == null
                || (requestedValue != 1 && requestedValue != -1)) {
            return;
        }

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

                transaction.delete(voteRef);
                updatedCount = currentCount - oldValue;

            } else if (voteSnapshot.exists()) {

                transaction.update(
                        voteRef,
                        "value",
                        requestedValue
                );

                updatedCount =
                        currentCount + requestedValue - oldValue;

            } else {
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
            if (isFinishing() || isDestroyed()) {
                return;
            }

            question.setVoteCount(updatedCount);
            questionAdapter.notifyDataSetChanged();

        }).addOnFailureListener(error -> {
            Log.e(TAG, "Không thể bình chọn câu hỏi", error);

            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(
                        this,
                        "Không thể bình chọn: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void sortQuestions() {
        Collections.sort(
                myQuestions,
                (first, second) -> Long.compare(
                        getTimestampMillis(second.getCreatedAt()),
                        getTimestampMillis(first.getCreatedAt())
                )
        );
    }

    private void sortAnswers() {
        Collections.sort(
                myAnswers,
                (first, second) -> Long.compare(
                        getTimestampMillis(second.getCreatedAt()),
                        getTimestampMillis(first.getCreatedAt())
                )
        );
    }

    private long getTimestampMillis(Timestamp timestamp) {
        return timestamp == null
                ? 0L
                : timestamp.toDate().getTime();
    }

    private String readFirstString(
            DocumentSnapshot document,
            String... fields) {

        for (String field : fields) {
            String value = document.getString(field);

            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }

        return null;
    }

    private void handleLoadError(Exception error) {
        isLoading = false;
        progressBar.setVisibility(View.GONE);
        rvMyQna.setVisibility(View.GONE);
        tvEmpty.setText("Không thể tải dữ liệu Q&A. Vui lòng thử lại.");
        tvEmpty.setVisibility(View.VISIBLE);

        Log.e(TAG, "Lỗi tải dữ liệu Q&A", error);

        Toast.makeText(
                this,
                "Tải dữ liệu Q&A thất bại.",
                Toast.LENGTH_SHORT
        ).show();
    }

    // Adapter riêng để hiển thị các câu trả lời của người dùng.
    // Nhấn câu trả lời sẽ mở câu hỏi liên quan.
    private class MyAnswersAdapter
            extends RecyclerView.Adapter<AnswerViewHolder> {

        private final List<Answer> answerList = new ArrayList<>();

        void setAnswers(List<Answer> answers) {
            answerList.clear();

            if (answers != null) {
                answerList.addAll(answers);
            }

            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AnswerViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_answer, parent, false);

            return new AnswerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull AnswerViewHolder holder,
                int position) {

            Answer answer = answerList.get(position);

            // Hiển thị tiêu đề câu hỏi liên quan tại vị trí tên tác giả.
            String questionTitle =
                    questionTitles.get(answer.getQuestionId());

            if (questionTitle != null
                    && !questionTitle.trim().isEmpty()) {
                holder.tvAnswerAuthor.setText(
                        "Câu hỏi: " + questionTitle
                );
            } else {
                holder.tvAnswerAuthor.setText("Câu trả lời của bạn");
            }

            holder.tvAnswerContent.setText(
                    answer.getContent() == null
                            ? ""
                            : answer.getContent()
            );

            Timestamp createdAt = answer.getCreatedAt();

            if (createdAt != null) {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                        );

                holder.tvAnswerDate.setText(
                        sdf.format(createdAt.toDate())
                );
            } else {
                holder.tvAnswerDate.setText("");
            }

            holder.tvAnswerVoteCount.setText(
                    answer.getVoteCount() + " vote"
            );

            String avatarUrl = answer.getAuthorAvatar();

            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                Picasso.get()
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_account)
                        .error(R.drawable.ic_account)
                        .fit()
                        .centerCrop()
                        .into(holder.ivAuthorAvatar);
            } else {
                Picasso.get().cancelRequest(holder.ivAuthorAvatar);
                holder.ivAuthorAvatar.setImageResource(
                        R.drawable.ic_account
                );
            }


            holder.btnReportAnswer.setVisibility(View.GONE);

            View parent = (View) holder.tvAnswerUpVote.getParent();
            parent.setVisibility(View.GONE);


            holder.itemView.setOnClickListener(v ->
                    openQuestion(answer.getQuestionId())
            );
        }

        @Override
        public int getItemCount() {
            return answerList.size();
        }
    }
}
