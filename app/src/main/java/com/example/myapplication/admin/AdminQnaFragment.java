package com.example.myapplication.admin;

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
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminQnaFragment extends Fragment {

    private TabLayout tabLayout;
    private EditText etSearch;
    private View btnClearSearch;
    private TextView chipAll, chipPending, chipResolved, chipDismissed;
    private RecyclerView rvReports;
    private ProgressBar pbLoading;
    private View layoutEmptyState;

    private AdminReportAdapter adapter;
    private List<AdminReportItem> allQuestionItems = new ArrayList<>();
    private List<AdminReportItem> allAnswerItems = new ArrayList<>();

    // 0 = Questions, 1 = Answers
    private int currentTab = 0;
    // "ALL", "PENDING", "REPORTED_CONTENT_DELETED", "DISMISSED"
    private String currentFilter = "ALL";
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
            Toast.makeText(requireContext(), "Đã chọn: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            // TODO: Ở Bước 1.2 sẽ mở AdminReportDetailActivity
        });
        rvReports.setAdapter(adapter);
    }

    private void loadFirestoreReports() {
        pbLoading.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Tải báo cáo Câu hỏi (questionReports)
        db.collection("questionReports").get().addOnCompleteListener(task -> {
            if (isAdded()) {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    groupQuestionReports(task.getResult().getDocuments());
                } else {
                    allQuestionItems.clear();
                }

                // 2. Tải báo cáo Câu trả lời (answerReports)
                loadAnswerReports(db);
            }
        });
    }

    private void loadAnswerReports(FirebaseFirestore db) {
        db.collection("answerReports").get().addOnCompleteListener(task -> {
            if (isAdded()) {
                pbLoading.setVisibility(View.GONE);
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    groupAnswerReports(task.getResult().getDocuments());
                } else {
                    allAnswerItems.clear();
                }
                applyFilterAndSearch();
            }
        });
    }

    private void groupQuestionReports(List<DocumentSnapshot> docs) {
        allQuestionItems.clear();
        Map<String, List<DocumentSnapshot>> map = new HashMap<>();
        for (DocumentSnapshot doc : docs) {
            String qId = doc.getString("questionId");
            if (qId == null || qId.isEmpty()) continue;
            if (!map.containsKey(qId)) map.put(qId, new ArrayList<>());
            map.get(qId).add(doc);
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (Map.Entry<String, List<DocumentSnapshot>> entry : map.entrySet()) {
            String questionId = entry.getKey();
            List<DocumentSnapshot> reports = entry.getValue();
            String latestStatus = reports.get(0).getString("status");
            if (latestStatus == null || latestStatus.isEmpty()) latestStatus = "PENDING";

            final String statusToUse = latestStatus;
            db.collection("questions").document(questionId).get().addOnSuccessListener(qDoc -> {
                if (!isAdded()) return;
                String title = qDoc.exists() ? qDoc.getString("title") : "Câu hỏi vi phạm";
                String category = qDoc.exists() ? qDoc.getString("category") : "Chung";

                AdminReportItem item = new AdminReportItem(
                        reports.get(0).getId(),
                        questionId,
                        "QUESTION",
                        title,
                        qDoc.exists() ? qDoc.getString("content") : "",
                        null,
                        statusToUse,
                        reports.size(),
                        "Vừa xong",
                        "Người dùng",
                        null,
                        Arrays.asList(category != null ? category : "Q&A")
                );
                allQuestionItems.add(item);
                if (currentTab == 0) applyFilterAndSearch();
            });
        }
    }

    private void groupAnswerReports(List<DocumentSnapshot> docs) {
        allAnswerItems.clear();
        Map<String, List<DocumentSnapshot>> map = new HashMap<>();
        for (DocumentSnapshot doc : docs) {
            String aId = doc.getString("answerId");
            if (aId == null || aId.isEmpty()) continue;
            if (!map.containsKey(aId)) map.put(aId, new ArrayList<>());
            map.get(aId).add(doc);
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (Map.Entry<String, List<DocumentSnapshot>> entry : map.entrySet()) {
            String answerId = entry.getKey();
            List<DocumentSnapshot> reports = entry.getValue();
            String latestStatus = reports.get(0).getString("status");
            if (latestStatus == null || latestStatus.isEmpty()) latestStatus = "PENDING";

            final String statusToUse = latestStatus;
            db.collection("answers").document(answerId).get().addOnSuccessListener(aDoc -> {
                if (!isAdded()) return;
                String content = aDoc.exists() ? aDoc.getString("content") : "Câu trả lời vi phạm";
                String questionId = aDoc.exists() ? aDoc.getString("questionId") : "";

                AdminReportItem item = new AdminReportItem(
                        reports.get(0).getId(),
                        answerId,
                        "ANSWER",
                        content,
                        content,
                        "Câu hỏi liên quan",
                        statusToUse,
                        reports.size(),
                        "Vừa xong",
                        "Người dùng",
                        null,
                        new ArrayList<>()
                );

                if (questionId != null && !questionId.isEmpty()) {
                    db.collection("questions").document(questionId).get().addOnSuccessListener(qDoc -> {
                        if (isAdded() && qDoc.exists()) {
                            item.setParentQuestionTitle(qDoc.getString("title"));
                            if (currentTab == 1) applyFilterAndSearch();
                        }
                    });
                }

                allAnswerItems.add(item);
                if (currentTab == 1) applyFilterAndSearch();
            });
        }
    }

    // Tạm thời bỏ các dữ liệu ví dụ mẫu (mock data), chỉ hiển thị dữ liệu thực từ Firestore
    private void createMockQuestions() {
        allQuestionItems.clear();
    }

    private void createMockAnswers() {
        allAnswerItems.clear();
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

        adapter.setItems(filteredList);

        if (filteredList.isEmpty()) {
            rvReports.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvReports.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}
