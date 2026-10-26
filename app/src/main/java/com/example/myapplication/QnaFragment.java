
package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.qna.report.ReportQuestionActivity;
import com.example.myapplication.qna.CreateQuestionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import com.example.myapplication.qna.QuestionAdapter;
import com.example.myapplication.qna.Question;
import com.example.myapplication.qna.questionDetail.QuestionDetailActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.List;
import java.util.Locale;


public class QnaFragment extends Fragment {

    private RecyclerView rvQuestions;
    private ChipGroup chipGroupQuestionFilter;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAskQuestion;
    private Chip chipAllQuestions;
    private Chip chipMyQuestions;
    private Chip chipUnanswered;
    private Chip chipAnswered;
    private Spinner spinnerQuestionSort;
    private ProgressBar pbQuestions;
    private TextView tvEmptyQuestions;

    private QuestionAdapter adapter;

    private final List<Question> allQuestions = new ArrayList<>();

    private String currentFilter = "ALL";
    private int currentSort = 0;
    private TextInputEditText etSearchQuestions;
    private String searchQuery = "";
    private ActivityResultLauncher<Intent> createQuestionLauncher;
    private ActivityResultLauncher<Intent> questionDetailLauncher;

    public QnaFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_qna,
                container,
                false
        );

        createQuestionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                                loadQuestions();
                            }
                        }
                );

        questionDetailLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode()
                                    == android.app.Activity.RESULT_OK) {
                                loadQuestions();
                            }
                        }
                );

        initViews(view);
        setupRecyclerView();
        setupFilters();
        setupSort();
        setupSearch();
        setupCreateQuestion();

        getParentFragmentManager().setFragmentResultListener(
                "questionVoteUpdated",
                getViewLifecycleOwner(),
                (requestKey, result) -> loadQuestions()
        );

        loadQuestions();

        return view;
    }

    private void initViews(View view) {
        rvQuestions = view.findViewById(R.id.rvQuestions);
        etSearchQuestions = view.findViewById(R.id.etSearchQuestions);
        fabAskQuestion = view.findViewById(R.id.fabAskQuestion);

        chipGroupQuestionFilter =
                view.findViewById(R.id.chipGroupQuestionFilter);

        chipAllQuestions =
                view.findViewById(R.id.chipAllQuestions);

        chipMyQuestions =
                view.findViewById(R.id.chipMyQuestions);

        chipUnanswered =
                view.findViewById(R.id.chipUnanswered);

        chipAnswered =
                view.findViewById(R.id.chipAnswered);

        spinnerQuestionSort =
                view.findViewById(R.id.spinnerQuestionSort);

        pbQuestions = view.findViewById(R.id.pbQuestions);

        tvEmptyQuestions =
                view.findViewById(R.id.tvEmptyQuestions);
    }
    private void setupSearch() {
        etSearchQuestions.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count
            ) {
                searchQuery = s == null
                        ? ""
                        : s.toString().trim().toLowerCase(Locale.ROOT);

                applyFilterAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    private void setupCreateQuestion() {
        fabAskQuestion.setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    CreateQuestionActivity.class
            );

            createQuestionLauncher.launch(intent);
        });
    }

    private void setupRecyclerView() {
        rvQuestions.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        String currentUserId =
                user != null ? user.getUid() : null;

        adapter = new QuestionAdapter(
                new ArrayList<>(),
                currentUserId,
                new QuestionAdapter.OnQuestionActionListener() {

                    @Override
                    public void onQuestionClick(Question question) {
                        Intent intent = new Intent(
                                requireContext(),
                                QuestionDetailActivity.class
                        );
                        intent.putExtra(
                                QuestionDetailActivity.EXTRA_QUESTION_ID,
                                question.getId()
                        );
                        questionDetailLauncher.launch(intent);
                    }

                    @Override
                    public void onReportClick(Question question) {
                        Intent intent = new Intent(
                                requireContext(),
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
                    public void onVoteClick(Question question, int value) {
                        toggleQuestionVote(question, value);
                    }
                }
        );

        rvQuestions.setAdapter(adapter);
    }

    private void showQuestionReportDialog(Question question) {
        if (question == null || question.getId() == null) {
            return;
        }

        String[] reasons = {
                "Spam hoặc quảng cáo",
                "Thông tin sai lệch",
                "Nội dung không phù hợp",
                "Quấy rối hoặc xúc phạm",
                "Không liên quan đến chủ đề",
                "Vi phạm quyền riêng tư",
                "Vi phạm bản quyền",
                "Lý do khác"
        };

        int[] selectedReason = {-1};

        new AlertDialog.Builder(requireContext())
                .setTitle("Báo cáo câu hỏi")
                .setSingleChoiceItems(
                        reasons,
                        -1,
                        (dialog, which) -> selectedReason[0] = which
                )
                .setPositiveButton("Gửi báo cáo", (dialog, which) -> {
                    if (selectedReason[0] < 0) {
                        Toast.makeText(
                                requireContext(),
                                "Vui lòng chọn lý do báo cáo.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    submitQuestionReport(
                            question,
                            reasons[selectedReason[0]]
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitQuestionReport(
            Question question,
            String reason
    ) {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    requireContext(),
                    "Vui lòng đăng nhập để báo cáo.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String questionId = question.getId();
        String userId = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Mỗi tài khoản chỉ có một báo cáo cho mỗi câu hỏi.
        String reportId = questionId + "_" + userId;

        com.google.firebase.firestore.DocumentReference reportRef =
                db.collection("questionReports").document(reportId);

        com.google.firebase.firestore.DocumentReference questionRef =
                db.collection("questions").document(questionId);

        Map<String, Object> report = new HashMap<>();

        report.put("questionId", questionId);
        report.put("userId", userId);
        report.put("reason", reason);
        report.put("detailReason", "");
        report.put("status", "PENDING");
        report.put("createdAt", FieldValue.serverTimestamp());
        report.put("reviewedAt", null);
        report.put("reviewedBy", "");
        report.put("reviewReason", "");
        report.put(
                "questionTitle",
                question.getTitle() == null ? "" : question.getTitle()
        );

        db.runTransaction(transaction -> {
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
            if (!isAdded()) {
                return;
            }

            if ("CREATED".equals(result)) {
                Toast.makeText(
                        requireContext(),
                        "Đã gửi báo cáo câu hỏi thành công.",
                        Toast.LENGTH_SHORT
                ).show();

            } else {
                Toast.makeText(
                        requireContext(),
                        "Bạn đã báo cáo câu hỏi này trước đó.",
                        Toast.LENGTH_LONG
                ).show();
            }

        }).addOnFailureListener(error -> {
            if (!isAdded()) {
                return;
            }

            android.util.Log.e(
                    "QNA_REPORT",
                    "Không thể gửi báo cáo Question",
                    error
            );

            Toast.makeText(
                    requireContext(),
                    "Gửi báo cáo thất bại. Vui lòng thử lại.",
                    Toast.LENGTH_LONG
            ).show();
        });
    }
    private void toggleQuestionVote(
            Question question,
            int requestedValue
    ) {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    requireContext(),
                    "Vui lòng đăng nhập để bình chọn.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (question == null
                || question.getId() == null
                || (requestedValue != 1 && requestedValue != -1)) {
            return;
        }

        String questionId = question.getId();
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

            Long countValue =
                    questionSnapshot.getLong("voteCount");

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

                // Nhấn lại lựa chọn đang chọn: bỏ vote.
                transaction.delete(voteRef);
                updatedCount = currentCount - oldValue;

            } else if (voteSnapshot.exists()) {

                // Đổi UP sang DOWN hoặc DOWN sang UP.
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
            if (!isAdded()) {
                return;
            }

            question.setVoteCount(updatedCount);
            adapter.notifyDataSetChanged();

            Toast.makeText(
                    requireContext(),
                    "Đã cập nhật bình chọn.",
                    Toast.LENGTH_SHORT
            ).show();

        }).addOnFailureListener(error -> {
            android.util.Log.e(
                    "QNA_QUESTION_VOTE",
                    "Question vote failed",
                    error
            );

            if (!isAdded()) {
                return;
            }

            Toast.makeText(
                    requireContext(),
                    "Không thể bình chọn: " + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private void setupFilters() {
        View.OnClickListener filterListener = view -> {
            int id = view.getId();

            if (id == R.id.chipAllQuestions) {
                currentFilter = "ALL";
            } else if (id == R.id.chipMyQuestions) {
                currentFilter = "MINE";
            } else if (id == R.id.chipUnanswered) {
                currentFilter = "UNANSWERED";
            } else if (id == R.id.chipAnswered) {
                currentFilter = "ANSWERED";
            }

            applyFilterAndSort();
        };

        chipAllQuestions.setOnClickListener(filterListener);
        chipMyQuestions.setOnClickListener(filterListener);
        chipUnanswered.setOnClickListener(filterListener);
        chipAnswered.setOnClickListener(filterListener);
    }

    private void setupSort() {
        List<String> sortOptions = Arrays.asList(
                "Ngày mới nhất",
                "Ngày cũ nhất",
                "Trạng thái: OPEN trước"
        );

        ArrayAdapter<String> sortAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        sortOptions
                );

        sortAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerQuestionSort.setAdapter(sortAdapter);

        spinnerQuestionSort.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        currentSort = position;
                        applyFilterAndSort();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    private void loadQuestions() {
        pbQuestions.setVisibility(View.VISIBLE);
        rvQuestions.setVisibility(View.GONE);
        tvEmptyQuestions.setVisibility(View.GONE);

        FirebaseFirestore db =
                FirebaseFirestore.getInstance();

        db.collection("questions")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    allQuestions.clear();

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        Question question = new Question();

                        question.setId(doc.getId());

                        question.setTitle(
                                doc.getString("title")
                        );

                        question.setContent(
                                doc.getString("content")
                        );

                        question.setImageUrl(
                                doc.getString("imageUrl")
                        );

                        question.setUserId(
                                readFirstString(
                                        doc,
                                        "userId",
                                        "authorId",
                                        "uid"
                                )
                        );

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

                        String status = doc.getString("status");

                        if (status == null || status.isEmpty()) {
                            status = "OPEN";
                        }

                        question.setStatus(
                                status.toUpperCase(Locale.ROOT)
                        );

                        // Không hiển thị câu hỏi đã bị xóa.
                        if ("DELETED".equalsIgnoreCase(status)) {
                            continue;
                        }

                        question.setCreatedAt(
                                doc.getTimestamp("createdAt")
                        );

                        Object voteValue = doc.get("voteCount");

                        if (voteValue instanceof Number) {
                            question.setVoteCount(
                                    ((Number) voteValue).longValue()
                            );
                        }

                        // Category có thể là String hoặc List.
                        List<String> categories = new ArrayList<>();

                        Object categoryValue = doc.get("category");

                        if (categoryValue instanceof String) {
                            String category =
                                    ((String) categoryValue).trim();

                            if (!category.isEmpty()) {
                                categories.add(category);
                            }

                        } else if (categoryValue instanceof List<?>) {

                            for (Object item :
                                    (List<?>) categoryValue) {

                                if (item instanceof String) {
                                    categories.add((String) item);
                                }
                            }
                        }

                        question.setCategories(categories);

                        allQuestions.add(question);
                    }

                    // Tải số câu trả lời cho từng câu hỏi.
                    loadAnswerCounts(db);

                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) return;

                    pbQuestions.setVisibility(View.GONE);

                    Toast.makeText(
                            requireContext(),
                            "Không thể tải câu hỏi: "
                                    + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadAnswerCounts(FirebaseFirestore db) {
        db.collection("answers")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    Map<String, Integer> answerCounts =
                            new HashMap<>();

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        String questionId =
                                readFirstString(
                                        doc,
                                        "questionId",
                                        "questionID"
                                );

                        if (questionId == null) continue;

                        int count = answerCounts.containsKey(questionId)
                                ? answerCounts.get(questionId) + 1
                                : 1;

                        answerCounts.put(questionId, count);
                    }

                    for (Question question : allQuestions) {
                        int count = answerCounts.containsKey(
                                question.getId()
                        )
                                ? answerCounts.get(question.getId())
                                : 0;

                        question.setAnswerCount(count);
                    }

                    pbQuestions.setVisibility(View.GONE);
                    applyFilterAndSort();
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) return;

                    // Vẫn hiển thị câu hỏi nếu không tải được answers.
                    for (Question question : allQuestions) {
                        question.setAnswerCount(0);
                    }

                    pbQuestions.setVisibility(View.GONE);
                    applyFilterAndSort();

                    Toast.makeText(
                            requireContext(),
                            "Không thể tải số câu trả lời",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void applyFilterAndSort() {
        if (adapter == null || !isAdded()) return;

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        String currentUserId =
                user != null ? user.getUid() : null;

        List<Question> filtered = new ArrayList<>();

        for (Question question : allQuestions) {
            boolean matches = false;

            switch (currentFilter) {
                case "MINE":
                    matches = currentUserId != null
                            && currentUserId.equals(
                            question.getUserId()
                    );
                    break;

                case "UNANSWERED":
                    matches = question.getAnswerCount() == 0;
                    break;

                case "ANSWERED":
                    matches = question.getAnswerCount() > 0;
                    break;

                case "ALL":
                default:
                    matches = true;
                    break;
            }


            String title = question.getTitle() == null
                    ? ""
                    : question.getTitle().toLowerCase(Locale.ROOT);

            String content = question.getContent() == null
                    ? ""
                    : question.getContent().toLowerCase(Locale.ROOT);

            String author = question.getAuthorName() == null
                    ? ""
                    : question.getAuthorName().toLowerCase(Locale.ROOT);

            boolean matchesSearch = searchQuery.isEmpty()
                    || title.contains(searchQuery)
                    || content.contains(searchQuery)
                    || author.contains(searchQuery);

            if (matches && matchesSearch) {
                filtered.add(question);
            }

        }

        if (currentSort == 0) {
            // Ngày mới nhất trước.
            filtered.sort(
                    (a, b) -> Long.compare(
                            getCreatedTime(b),
                            getCreatedTime(a)
                    )
            );

        } else if (currentSort == 1) {
            // Ngày cũ nhất trước.
            filtered.sort(
                    Comparator.comparingLong(
                            this::getCreatedTime
                    )
            );

        } else {
            // OPEN trước, CLOSED sau.
            filtered.sort(
                    Comparator.comparingInt(question ->
                            "OPEN".equalsIgnoreCase(
                                    question.getStatus()
                            ) ? 0 : 1
                    )
            );
        }

        adapter.setQuestions(filtered);

        boolean isEmpty = filtered.isEmpty();

        rvQuestions.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );

        tvEmptyQuestions.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        tvEmptyQuestions.setText(
                allQuestions.isEmpty()
                        ? "Chưa có câu hỏi nào"
                        : "Không có câu hỏi phù hợp"
        );
    }

    private long getCreatedTime(Question question) {
        return question.getCreatedAt() != null
                ? question.getCreatedAt()
                .toDate()
                .getTime()
                : 0L;
    }

    private String readFirstString(
            DocumentSnapshot doc,
            String... fields
    ) {
        for (String field : fields) {
            Object value = doc.get(field);

            if (value instanceof String) {
                String text = ((String) value).trim();

                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return null;
    }

    private void confirmCloseQuestion(Question question) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đóng câu hỏi")
                .setMessage(
                        "Bạn có chắc muốn đóng câu hỏi này không? " +
                                "Người dùng sẽ không thể gửi thêm câu trả lời."
                )
                .setPositiveButton("Có", (dialog, which) -> {
                    FirebaseFirestore.getInstance()
                            .collection("questions")
                            .document(question.getId())
                            .update("status", "CLOSED")
                            .addOnSuccessListener(unused -> {
                                if (!isAdded()) return;

                                question.setStatus("CLOSED");
                                applyFilterAndSort();

                                Toast.makeText(
                                        requireContext(),
                                        "Đã đóng câu hỏi",
                                        Toast.LENGTH_SHORT
                                ).show();
                            })
                            .addOnFailureListener(error -> {
                                if (!isAdded()) return;

                                Toast.makeText(
                                        requireContext(),
                                        "Không thể đóng câu hỏi",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .setNegativeButton("Không", null)
                .show();
    }
}
