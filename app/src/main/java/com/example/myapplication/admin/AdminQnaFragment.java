package com.example.myapplication.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.admin.adapters.AdminReportAdapter;
import com.example.myapplication.admin.models.AdminReportItem;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class AdminQnaFragment extends Fragment {

    private TabLayout tabLayout;
    private EditText etSearch;
    private View btnClearSearch;
    private TextView chipAll, chipPending, chipResolved, chipDismissed;
    private RecyclerView rvReports;
    private ProgressBar pbLoading;
    private View layoutEmptyState;

    private AdminReportAdapter adapter;
    private final List<AdminReportItem> allQuestionItems = new ArrayList<>();
    private final List<AdminReportItem> allAnswerItems = new ArrayList<>();
    private ListenerRegistration questionReportsListener;
    private ListenerRegistration answerReportsListener;
    private ListenerRegistration deletedQuestionsListener;
    private ListenerRegistration deletedAnswersListener;

    private List<DocumentSnapshot> latestQuestionReportsDocs = null;
    private List<DocumentSnapshot> latestDeletedQuestionsDocs = null;
    private List<DocumentSnapshot> latestAnswerReportsDocs = null;
    private List<DocumentSnapshot> latestDeletedAnswersDocs = null;

    private int currentTab = 0; // 0: Câu hỏi, 1: Câu trả lời
    private String currentFilter = "ALL"; // ALL, PENDING, REPORTED_CONTENT_DELETED, DISMISSED
    private String currentSearchKeyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_qna, container, false);

        initViews(view);
        setupTabs();
        setupFilterChips();
        setupSearch();
        setupRecyclerView();
        loadFirestoreReports();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFirestoreReports();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_admin_qna);
        etSearch = view.findViewById(R.id.et_admin_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        chipAll = view.findViewById(R.id.chip_filter_all);
        chipPending = view.findViewById(R.id.chip_filter_pending);
        chipResolved = view.findViewById(R.id.chip_filter_resolved);
        chipDismissed = view.findViewById(R.id.chip_filter_dismissed);
        rvReports = view.findViewById(R.id.rv_admin_reports);
        pbLoading = view.findViewById(R.id.pb_admin_loading);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Câu hỏi"));
        tabLayout.addTab(tabLayout.newTab().setText("Câu trả lời"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                applyFilterAndSearch();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFilterChips() {
        View.OnClickListener chipListener = v -> {
            int id = v.getId();
            if (id == R.id.chip_filter_all) {
                currentFilter = "ALL";
            } else if (id == R.id.chip_filter_pending) {
                currentFilter = "PENDING";
            } else if (id == R.id.chip_filter_resolved) {
                currentFilter = "REPORTED_CONTENT_DELETED";
            } else if (id == R.id.chip_filter_dismissed) {
                currentFilter = "DISMISSED";
            }
            updateChipStyles();
            applyFilterAndSearch();
        };

        chipAll.setOnClickListener(chipListener);
        chipPending.setOnClickListener(chipListener);
        chipResolved.setOnClickListener(chipListener);
        chipDismissed.setOnClickListener(chipListener);

        updateChipStyles();
    }

    private void updateChipStyles() {
        resetChip(chipAll, "ALL".equals(currentFilter));
        resetChip(chipPending, "PENDING".equals(currentFilter));
        resetChip(chipResolved, "REPORTED_CONTENT_DELETED".equals(currentFilter));
        resetChip(chipDismissed, "DISMISSED".equals(currentFilter));
    }

    private void resetChip(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_filter_chip_unselected);
            chip.setTextColor(Color.parseColor("#475569"));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchKeyword = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                btnClearSearch.setVisibility(currentSearchKeyword.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    private void setupRecyclerView() {
        rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminReportAdapter(item -> {
            Intent intent = new Intent(requireContext(), AdminReportDetailActivity.class);
            intent.putExtra(AdminReportDetailActivity.EXTRA_REPORT_ID, item.getId());
            intent.putExtra(AdminReportDetailActivity.EXTRA_TARGET_ID, item.getContentId());
            intent.putExtra(AdminReportDetailActivity.EXTRA_TARGET_TYPE, item.getContentType());
            intent.putExtra(AdminReportDetailActivity.EXTRA_TITLE, item.getTitle());
            intent.putExtra(AdminReportDetailActivity.EXTRA_CONTENT, item.getContent());
            intent.putExtra(AdminReportDetailActivity.EXTRA_PARENT_QUESTION, item.getParentQuestionTitle());
            intent.putExtra(AdminReportDetailActivity.EXTRA_STATUS, item.getStatus());
            intent.putExtra(AdminReportDetailActivity.EXTRA_REPORT_COUNT, item.getReportCount());
            intent.putExtra(AdminReportDetailActivity.EXTRA_AUTHOR_NAME, item.getAuthorName());
            intent.putExtra(AdminReportDetailActivity.EXTRA_AUTHOR_AVATAR, item.getAuthorAvatar());
            intent.putExtra(AdminReportDetailActivity.EXTRA_POST_TIME, item.getTimeAgo());
            String tag = (item.getTags() != null && !item.getTags().isEmpty()) ? android.text.TextUtils.join(" • ", item.getTags()) : "";
            intent.putExtra(AdminReportDetailActivity.EXTRA_TAG, tag);
            intent.putExtra(AdminReportDetailActivity.EXTRA_IMAGE_URL, item.getImageUrl());
            startActivity(intent);
        });
        rvReports.setAdapter(adapter);
    }

    private void loadFirestoreReports() {
        if (questionReportsListener != null) {
            questionReportsListener.remove();
            questionReportsListener = null;
        }
        if (answerReportsListener != null) {
            answerReportsListener.remove();
            answerReportsListener = null;
        }
        if (deletedQuestionsListener != null) {
            deletedQuestionsListener.remove();
            deletedQuestionsListener = null;
        }
        if (deletedAnswersListener != null) {
            deletedAnswersListener.remove();
            deletedAnswersListener = null;
        }
        if (deletedQuestionsListener != null) {
            deletedQuestionsListener.remove();
            deletedQuestionsListener = null;
        }
        if (deletedAnswersListener != null) {
            deletedAnswersListener.remove();
            deletedAnswersListener = null;
        }

        pbLoading.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Lắng nghe báo cáo Câu hỏi realtime
        questionReportsListener = db.collection("questionReports")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    latestQuestionReportsDocs = (snapshots != null) ? snapshots.getDocuments() : null;
                    rebuildQuestionItems();
                });

        // 2. Lắng nghe các câu hỏi đã bị xóa realtime (để giữ trong admin và gán nhãn "Đã xử lý")
        deletedQuestionsListener = db.collection("questions")
                .whereEqualTo("status", "DELETED")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    latestDeletedQuestionsDocs = (snapshots != null) ? snapshots.getDocuments() : null;
                    rebuildQuestionItems();
                });

        // 3. Lắng nghe báo cáo Câu trả lời realtime
        answerReportsListener = db.collection("answerReports")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    latestAnswerReportsDocs = (snapshots != null) ? snapshots.getDocuments() : null;
                    rebuildAnswerItems();
                });

        // 4. Lắng nghe các câu trả lời đã bị xóa realtime
        deletedAnswersListener = db.collection("answers")
                .whereEqualTo("status", "DELETED")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    latestDeletedAnswersDocs = (snapshots != null) ? snapshots.getDocuments() : null;
                    rebuildAnswerItems();
                });
    }

    private String formatTimestamp(long timeMillis) {
        if (timeMillis <= 0) return "Vừa xong";
        long diff = System.currentTimeMillis() - timeMillis;
        if (diff < 0) diff = 0;
        long minutes = diff / (1000 * 60);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + " ngày trước";
        if (hours > 0) return hours + " giờ trước";
        if (minutes > 0) return minutes + " phút trước";
        return "Vừa xong";
    }

    private void rebuildQuestionItems() {
        allQuestionItems.clear();
        Map<String, List<DocumentSnapshot>> reportMap = new HashMap<>();
        if (latestQuestionReportsDocs != null) {
            for (DocumentSnapshot doc : latestQuestionReportsDocs) {
                String qId = doc.getString("questionId");
                if (qId == null || qId.isEmpty()) continue;
                if (!reportMap.containsKey(qId)) reportMap.put(qId, new ArrayList<>());
                reportMap.get(qId).add(doc);
            }
        }

        Set<String> processedQuestionIds = new HashSet<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Xử lý các câu hỏi có báo cáo
        for (Map.Entry<String, List<DocumentSnapshot>> entry : reportMap.entrySet()) {
            String questionId = entry.getKey();
            processedQuestionIds.add(questionId);
            List<DocumentSnapshot> reports = entry.getValue();

            db.collection("questions").document(questionId).get().addOnSuccessListener(qDoc -> {
                if (!isAdded()) return;
                String title = qDoc.exists() ? qDoc.getString("title") : "Câu hỏi vi phạm";
                String category = qDoc.exists() ? qDoc.getString("category") : "Chung";
                String authorName = qDoc.exists() ? qDoc.getString("authorName") : null;
                String authorAvatar = qDoc.exists() ? qDoc.getString("authorAvatar") : null;
                String imageUrl = qDoc.exists() ? qDoc.getString("imageUrl") : null;
                String userId = qDoc.exists() ? qDoc.getString("userId") : null;
                String qStatus = qDoc.exists() ? qDoc.getString("status") : null;

                String statusToUse;
                if ("DELETED".equalsIgnoreCase(qStatus) || (qDoc.exists() && qDoc.get("deletedAt") != null)) {
                    statusToUse = "REPORTED_CONTENT_DELETED"; // Luôn gán nhãn "Đã xử lý"
                } else {
                    boolean hasPending = false;
                    boolean hasDeleted = false;
                    boolean allDismissed = true;
                    for (DocumentSnapshot rDoc : reports) {
                        String s = rDoc.getString("status");
                        if ("PENDING".equalsIgnoreCase(s) || s == null || s.isEmpty()) {
                            hasPending = true;
                            allDismissed = false;
                        } else if ("REPORTED_CONTENT_DELETED".equalsIgnoreCase(s)) {
                            hasDeleted = true;
                            allDismissed = false;
                        } else if (!"DISMISSED".equalsIgnoreCase(s)) {
                            allDismissed = false;
                        }
                    }
                    if (hasPending) {
                        statusToUse = "PENDING";
                    } else if (hasDeleted) {
                        statusToUse = "REPORTED_CONTENT_DELETED";
                    } else if (allDismissed && !reports.isEmpty()) {
                        statusToUse = "DISMISSED";
                    } else {
                        statusToUse = "PENDING";
                    }
                }

                long maxTime = 0;
                for (DocumentSnapshot rDoc : reports) {
                    Timestamp ts = rDoc.getTimestamp("createdAt");
                    if (ts != null && ts.toDate().getTime() > maxTime) {
                        maxTime = ts.toDate().getTime();
                    }
                }
                if (maxTime == 0 && qDoc.exists()) {
                    Timestamp qTs = qDoc.getTimestamp("createdAt");
                    if (qTs != null) maxTime = qTs.toDate().getTime();
                }
                if (maxTime == 0) maxTime = System.currentTimeMillis();

                AdminReportItem item = new AdminReportItem(
                        reports.get(0).getId(),
                        questionId,
                        "QUESTION",
                        title,
                        qDoc.exists() ? qDoc.getString("content") : "",
                        null,
                        statusToUse,
                        reports.size(),
                        formatTimestamp(maxTime),
                        (authorName != null && !authorName.trim().isEmpty()) ? authorName.trim() : "Người dùng",
                        authorAvatar,
                        Arrays.asList(category != null ? category : "Q&A")
                );
                item.setTimestamp(maxTime);
                item.setImageUrl(imageUrl);
                allQuestionItems.add(item);
                pbLoading.setVisibility(View.GONE);
                if (currentTab == 0) applyFilterAndSearch();

                if ((authorName == null || authorName.trim().isEmpty() || authorAvatar == null || authorAvatar.trim().isEmpty())
                        && userId != null && !userId.trim().isEmpty()) {
                    db.collection("users").document(userId).get().addOnSuccessListener(uDoc -> {
                        if (!isAdded() || !uDoc.exists()) return;
                        String uName = uDoc.getString("fullName");
                        if (uName == null || uName.trim().isEmpty()) uName = uDoc.getString("username");
                        if (uName == null || uName.trim().isEmpty()) uName = uDoc.getString("email");
                        String uAvatar = uDoc.getString("avatar");
                        boolean changed = false;
                        if (uName != null && !uName.trim().isEmpty() && "Người dùng".equals(item.getAuthorName())) {
                            item.setAuthorName(uName.trim());
                            changed = true;
                        }
                        if (uAvatar != null && !uAvatar.trim().isEmpty() && item.getAuthorAvatar() == null) {
                            item.setAuthorAvatar(uAvatar.trim());
                            changed = true;
                        }
                        if (changed && currentTab == 0) applyFilterAndSearch();
                    });
                }
            });
        }

        // 2. Thêm các câu hỏi đã xóa (kể cả chưa có báo cáo) để admin vẫn giữ và hiển thị "Đã xử lý"
        if (latestDeletedQuestionsDocs != null) {
            for (DocumentSnapshot qDoc : latestDeletedQuestionsDocs) {
                String questionId = qDoc.getId();
                if (processedQuestionIds.contains(questionId)) continue;
                processedQuestionIds.add(questionId);

                String title = qDoc.getString("title");
                String content = qDoc.getString("content");
                String authorName = qDoc.getString("authorName");
                String authorAvatar = qDoc.getString("authorAvatar");
                String imageUrl = qDoc.getString("imageUrl");
                Object catObj = qDoc.get("category");
                List<String> tags = new ArrayList<>();
                if (catObj instanceof String) tags.add((String) catObj);
                else if (catObj instanceof List<?>) {
                    for (Object o : (List<?>) catObj) if (o instanceof String) tags.add((String) o);
                }
                if (tags.isEmpty()) tags.add("Q&A");

                Timestamp delTs = qDoc.getTimestamp("deletedAt");
                if (delTs == null) delTs = qDoc.getTimestamp("createdAt");
                long delTime = (delTs != null) ? delTs.toDate().getTime() : System.currentTimeMillis();

                AdminReportItem item = new AdminReportItem(
                        "del_" + questionId,
                        questionId,
                        "QUESTION",
                        (title != null && !title.isEmpty()) ? title : "Câu hỏi đã xóa",
                        (content != null) ? content : "",
                        null,
                        "REPORTED_CONTENT_DELETED", // Gán nhãn "Đã xử lý"
                        0,
                        formatTimestamp(delTime),
                        (authorName != null && !authorName.trim().isEmpty()) ? authorName.trim() : "Người dùng",
                        authorAvatar,
                        tags
                );
                item.setTimestamp(delTime);
                item.setImageUrl(imageUrl);
                allQuestionItems.add(item);
                pbLoading.setVisibility(View.GONE);
                if (currentTab == 0) applyFilterAndSearch();
            }
        }

        if (processedQuestionIds.isEmpty()) {
            if ((latestQuestionReportsDocs == null || latestQuestionReportsDocs.isEmpty())
                    && (latestDeletedQuestionsDocs == null || latestDeletedQuestionsDocs.isEmpty())) {
                createMockQuestions();
            }
            if (currentTab == 0) {
                pbLoading.setVisibility(View.GONE);
                applyFilterAndSearch();
            }
        }
    }

    private void rebuildAnswerItems() {
        allAnswerItems.clear();
        Map<String, List<DocumentSnapshot>> reportMap = new HashMap<>();
        if (latestAnswerReportsDocs != null) {
            for (DocumentSnapshot doc : latestAnswerReportsDocs) {
                String aId = doc.getString("answerId");
                if (aId == null || aId.isEmpty()) continue;
                if (!reportMap.containsKey(aId)) reportMap.put(aId, new ArrayList<>());
                reportMap.get(aId).add(doc);
            }
        }

        Set<String> processedAnswerIds = new HashSet<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Thêm các câu trả lời có báo cáo
        for (Map.Entry<String, List<DocumentSnapshot>> entry : reportMap.entrySet()) {
            String answerId = entry.getKey();
            processedAnswerIds.add(answerId);
            List<DocumentSnapshot> reports = entry.getValue();

            db.collection("answers").document(answerId).get().addOnSuccessListener(aDoc -> {
                if (!isAdded()) return;
                String content = aDoc.exists() ? aDoc.getString("content") : "Nội dung câu trả lời";
                String questionId = aDoc.exists() ? aDoc.getString("questionId") : "";
                String authorName = aDoc.exists() ? aDoc.getString("authorName") : null;
                String authorAvatar = aDoc.exists() ? aDoc.getString("authorAvatar") : null;
                String userId = aDoc.exists() ? aDoc.getString("userId") : null;
                String aStatus = aDoc.exists() ? aDoc.getString("status") : null;

                String statusToUse;
                if ("DELETED".equalsIgnoreCase(aStatus) || (aDoc.exists() && aDoc.get("deletedAt") != null)) {
                    statusToUse = "REPORTED_CONTENT_DELETED"; // Luôn gán nhãn "Đã xử lý"
                } else {
                    boolean hasPending = false;
                    boolean hasDeleted = false;
                    boolean allDismissed = true;
                    for (DocumentSnapshot rDoc : reports) {
                        String s = rDoc.getString("status");
                        if ("PENDING".equalsIgnoreCase(s) || s == null || s.isEmpty()) {
                            hasPending = true;
                            allDismissed = false;
                        } else if ("REPORTED_CONTENT_DELETED".equalsIgnoreCase(s)) {
                            hasDeleted = true;
                            allDismissed = false;
                        } else if (!"DISMISSED".equalsIgnoreCase(s)) {
                            allDismissed = false;
                        }
                    }
                    if (hasPending) {
                        statusToUse = "PENDING";
                    } else if (hasDeleted) {
                        statusToUse = "REPORTED_CONTENT_DELETED";
                    } else if (allDismissed && !reports.isEmpty()) {
                        statusToUse = "DISMISSED";
                    } else {
                        statusToUse = "PENDING";
                    }
                }

                long maxTime = 0;
                for (DocumentSnapshot rDoc : reports) {
                    Timestamp ts = rDoc.getTimestamp("createdAt");
                    if (ts != null && ts.toDate().getTime() > maxTime) {
                        maxTime = ts.toDate().getTime();
                    }
                }
                if (maxTime == 0 && aDoc.exists()) {
                    Timestamp aTs = aDoc.getTimestamp("createdAt");
                    if (aTs != null) maxTime = aTs.toDate().getTime();
                }
                if (maxTime == 0) maxTime = System.currentTimeMillis();

                AdminReportItem item = new AdminReportItem(
                        reports.get(0).getId(),
                        answerId,
                        "ANSWER",
                        content,
                        content,
                        "Câu hỏi liên quan",
                        statusToUse,
                        reports.size(),
                        formatTimestamp(maxTime),
                        (authorName != null && !authorName.trim().isEmpty()) ? authorName.trim() : "Người dùng",
                        authorAvatar,
                        Arrays.asList("Câu trả lời")
                );
                item.setTimestamp(maxTime);
                allAnswerItems.add(item);
                pbLoading.setVisibility(View.GONE);
                if (currentTab == 1) applyFilterAndSearch();

                if (questionId != null && !questionId.isEmpty()) {
                    db.collection("questions").document(questionId).get().addOnSuccessListener(qDoc -> {
                        if (!isAdded() || !qDoc.exists()) return;
                        String qTitle = qDoc.getString("title");
                        if (qTitle != null && !qTitle.trim().isEmpty()) {
                            item.setParentQuestionTitle(qTitle.trim());
                            if (currentTab == 1) applyFilterAndSearch();
                        }
                    });
                }
            });
        }

        // 2. Thêm các câu trả lời đã bị xóa
        if (latestDeletedAnswersDocs != null) {
            for (DocumentSnapshot aDoc : latestDeletedAnswersDocs) {
                String answerId = aDoc.getId();
                if (processedAnswerIds.contains(answerId)) continue;
                processedAnswerIds.add(answerId);

                String content = aDoc.getString("content");
                String authorName = aDoc.getString("authorName");
                String authorAvatar = aDoc.getString("authorAvatar");
                String questionId = aDoc.getString("questionId");

                Timestamp delTs = aDoc.getTimestamp("deletedAt");
                if (delTs == null) delTs = aDoc.getTimestamp("createdAt");
                long delTime = (delTs != null) ? delTs.toDate().getTime() : System.currentTimeMillis();

                AdminReportItem item = new AdminReportItem(
                        "del_" + answerId,
                        answerId,
                        "ANSWER",
                        (content != null && !content.isEmpty()) ? content : "Câu trả lời đã xóa",
                        (content != null) ? content : "",
                        "Câu hỏi liên quan",
                        "REPORTED_CONTENT_DELETED", // Gán nhãn "Đã xử lý"
                        0,
                        formatTimestamp(delTime),
                        (authorName != null && !authorName.trim().isEmpty()) ? authorName.trim() : "Người dùng",
                        authorAvatar,
                        Arrays.asList("Câu trả lời")
                );
                item.setTimestamp(delTime);
                allAnswerItems.add(item);
                pbLoading.setVisibility(View.GONE);
                if (currentTab == 1) applyFilterAndSearch();

                if (questionId != null && !questionId.isEmpty()) {
                    db.collection("questions").document(questionId).get().addOnSuccessListener(qDoc -> {
                        if (!isAdded() || !qDoc.exists()) return;
                        String qTitle = qDoc.getString("title");
                        if (qTitle != null && !qTitle.trim().isEmpty()) {
                            item.setParentQuestionTitle(qTitle.trim());
                            if (currentTab == 1) applyFilterAndSearch();
                        }
                    });
                }
            }
        }

        if (processedAnswerIds.isEmpty()) {
            if ((latestAnswerReportsDocs == null || latestAnswerReportsDocs.isEmpty())
                    && (latestDeletedAnswersDocs == null || latestDeletedAnswersDocs.isEmpty())) {
                createMockAnswers();
            }
            if (currentTab == 1) {
                pbLoading.setVisibility(View.GONE);
                applyFilterAndSearch();
            }
        }
    }

        // Dữ liệu ví dụ mẫu phong phú phục vụ kiểm thử giao diện theo thiết kế image1.png
    private void createMockQuestions() {
        allQuestionItems.clear();
        allQuestionItems.add(new AdminReportItem(
                "q_rep_1",
                "q_1",
                "QUESTION",
                "Làm thế nào để ứng dụng Deep Learning trong phân tích tế bào ung thư?",
                "Tôi đang nghiên cứu đề tài về nhận diện tế bào ác tính qua ảnh mô bệnh học nhuộm H&E. Hiện tại tôi phân vân giữa mô hình ResNet50 và EfficientNetV2 để trích xuất đặc trưng hình thái tế bào, không biết mô hình nào cho độ nhạy (sensitivity) cao hơn trên tập dữ liệu kích thước nhỏ? Mọi người có kinh nghiệm tiền xử lý ảnh Whole Slide Imaging (WSI) xin cho mình lời khuyên.",
                null,
                "PENDING",
                3,
                "2 giờ trước",
                "Nguyễn Văn A",
                null,
                Arrays.asList("AI", "Y sinh học")
        ));

        allQuestionItems.add(new AdminReportItem(
                "q_rep_2",
                "q_2",
                "QUESTION",
                "Có nên sử dụng ChatGPT để viết phần Tổng quan tài liệu (Literature Review)?",
                "Việc sử dụng các công cụ LLM để tóm tắt các công trình nghiên cứu trước đây có bị các tạp chí khoa học xem là vi phạm liêm chính học thuật không? Có cách nào trích dẫn công cụ AI đúng quy chuẩn IEEE không?",
                null,
                "PENDING",
                2,
                "5 giờ trước",
                "Trần Thị B",
                null,
                Arrays.asList("Liêm chính học thuật", "AI")
        ));

        allQuestionItems.add(new AdminReportItem(
                "q_rep_3",
                "q_3",
                "QUESTION",
                "Chia sẻ tài khoản truy cập cơ sở dữ liệu Scopus và Web of Science miễn phí",
                "Mình có một số tài khoản và proxy dùng chung để tải bài báo có trả phí từ ScienceDirect, ai cần inbox mình gửi thông tin đăng nhập...",
                null,
                "REPORTED_CONTENT_DELETED",
                5,
                "1 ngày trước",
                "Lê Hoàng C",
                null,
                Arrays.asList("Tài liệu", "Chia sẻ")
        ));

        allQuestionItems.add(new AdminReportItem(
                "q_rep_4",
                "q_4",
                "QUESTION",
                "Khác biệt cốt lõi giữa học có giám sát và học tăng cường là gì?",
                "Mình là người mới bắt đầu học Machine Learning và thấy khó phân biệt khi nào nên áp dụng Reinforcement Learning thay vì Supervised Learning trong bài toán điều khiển tự động.",
                null,
                "DISMISSED",
                1,
                "3 ngày trước",
                "Vũ Minh D",
                null,
                Arrays.asList("Machine Learning", "Nhập môn")
        ));
    }

    private void createMockAnswers() {
        allAnswerItems.clear();
        allAnswerItems.add(new AdminReportItem(
                "a_rep_1",
                "a_1",
                "ANSWER",
                "Cứ copy nguyên bài nghiên cứu của nhóm khác rồi dịch sang tiếng Việt nộp là được, không ai kiểm tra đâu...",
                "Cứ copy nguyên bài nghiên cứu của nhóm khác rồi dịch sang tiếng Việt nộp là được, giáo viên cũng bận không ai kiểm tra đạo văn kỹ đâu bạn ơi, đỡ mất công làm.",
                "Làm sao để hoàn thành khóa luận tốt nghiệp đúng hạn?",
                "PENDING",
                4,
                "3 giờ trước",
                "Phạm Hữu E",
                null,
                new ArrayList<>()
        ));

        allAnswerItems.add(new AdminReportItem(
                "a_rep_2",
                "a_2",
                "ANSWER",
                "Bạn truy cập trang web này [link rút gọn cá độ/quảng cáo] để nhận tài liệu tham khảo miễn phí...",
                "Bạn truy cập trang web cá cược abc.xyz này để nhận tài liệu tham khảo luận văn miễn phí cực kỳ uy tín nhé.",
                "Tìm kiếm bộ dữ liệu MNIST ở đâu?",
                "PENDING",
                2,
                "6 giờ trước",
                "Hoàng Nam F",
                null,
                new ArrayList<>()
        ));

        allAnswerItems.add(new AdminReportItem(
                "a_rep_3",
                "a_3",
                "ANSWER",
                "Dữ liệu nghiên cứu này hoàn toàn là bịa đặt, người viết câu hỏi này không có kiến thức...",
                "Dữ liệu nghiên cứu này hoàn toàn là bịa đặt, người viết câu hỏi này không có kiến thức gì về khoa học mà cũng đòi lên đây hỏi...",
                "Phương pháp chuẩn hóa Z-score trong dữ liệu y tế",
                "REPORTED_CONTENT_DELETED",
                3,
                "2 ngày trước",
                "Đặng Tiến G",
                null,
                new ArrayList<>()
        ));

        allAnswerItems.add(new AdminReportItem(
                "a_rep_4",
                "a_4",
                "ANSWER",
                "Theo tài liệu chính thống của tác giả bài báo gốc năm 2017 (Attention Is All You Need)...",
                "Theo tài liệu chính thống của tác giả bài báo gốc năm 2017 (Attention Is All You Need), ma trận Query, Key, Value được nhân tích vô hướng tỷ lệ để tính toán trọng số tương đồng ngữ cảnh.",
                "Cơ chế Self-Attention trong Transformer hoạt động ra sao?",
                "DISMISSED",
                1,
                "4 ngày trước",
                "Ngô Quốc H",
                null,
                new ArrayList<>()
        ));
    }

    private void applyFilterAndSearch() {
        List<AdminReportItem> sourceList = (currentTab == 0) ? allQuestionItems : allAnswerItems;
        List<AdminReportItem> filteredList = new ArrayList<>();

        for (AdminReportItem item : sourceList) {
            // 1. Kiểm tra Filter Chip
            boolean matchesFilter = true;
            if (!"ALL".equals(currentFilter)) {
                matchesFilter = currentFilter.equalsIgnoreCase(item.getStatus());
            }

            // 2. Kiểm tra Search Keyword
            boolean matchesSearch = true;
            if (!currentSearchKeyword.isEmpty()) {
                String title = item.getTitle() != null ? item.getTitle().toLowerCase(Locale.ROOT) : "";
                String content = item.getContent() != null ? item.getContent().toLowerCase(Locale.ROOT) : "";
                String author = item.getAuthorName() != null ? item.getAuthorName().toLowerCase(Locale.ROOT) : "";
                matchesSearch = title.contains(currentSearchKeyword)
                        || content.contains(currentSearchKeyword)
                        || author.contains(currentSearchKeyword);
            }

            if (matchesFilter && matchesSearch) {
                filteredList.add(item);
            }
        }

        // Sắp xếp: Báo cáo "Chờ xử lý" (PENDING) luôn nằm ở đầu,
        // và nội dung mới bị report (timestamp mới nhất) luôn nằm trên cùng
        Collections.sort(filteredList, (a, b) -> {
            boolean aPending = "PENDING".equalsIgnoreCase(a.getStatus());
            boolean bPending = "PENDING".equalsIgnoreCase(b.getStatus());
            if (aPending != bPending) {
                return aPending ? -1 : 1;
            }
            return Long.compare(b.getTimestamp(), a.getTimestamp());
        });

        adapter.setItems(filteredList);

        if (filteredList.isEmpty()) {
            rvReports.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvReports.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (questionReportsListener != null) {
            questionReportsListener.remove();
            questionReportsListener = null;
        }
        if (answerReportsListener != null) {
            answerReportsListener.remove();
            answerReportsListener = null;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            applyFilterAndSearch();
        }
    }
}
