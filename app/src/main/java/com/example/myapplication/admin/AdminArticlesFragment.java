package com.example.myapplication.admin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.admin.adapters.AdminArticleAdapter;
import com.example.myapplication.admin.adapters.AdminReportAdapter;
import com.example.myapplication.admin.dialogs.AdminActionBottomSheetDialog;
import com.example.myapplication.admin.dialogs.AdminSuccessDialog;
import com.example.myapplication.admin.models.AdminArticleItem;
import com.example.myapplication.admin.models.AdminReportItem;
import com.example.myapplication.articles.articlesDetail.ArticleDetailActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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

public class AdminArticlesFragment extends Fragment
        implements AdminArticleAdapter.OnArticleActionListener, AdminReportAdapter.OnReportClickListener {

    private TabLayout tabLayout;
    private EditText etSearch;
    private ImageView btnClearSearch;

    private TextView chip1;
    private TextView chip2;
    private TextView chip3;
    private TextView chip4;
    private TextView chip5;

    private ProgressBar progressBar;
    private RecyclerView rvArticles;
    private View layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    private int currentTab = 0; // 0 = Kiểm duyệt bài, 1 = Báo cáo vi phạm
    private String currentFilter = "ALL";
    private String currentSearchKeyword = "";

    private AdminArticleAdapter articleAdapter;
    private AdminReportAdapter reportAdapter;

    private final List<AdminArticleItem> allArticles = new ArrayList<>();
    private final List<AdminReportItem> allReportItems = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration articlesListener;
    private ListenerRegistration reportsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_articles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupTabs();
        setupFilterChips();
        setupSearch();
        setupAdapters();

        loadArticlesData();
        loadReportsData();
    }

    private void initViews(View root) {
        tabLayout = root.findViewById(R.id.tab_admin_articles);
        etSearch = root.findViewById(R.id.et_admin_article_search);
        btnClearSearch = root.findViewById(R.id.btn_clear_article_search);

        chip1 = root.findViewById(R.id.chip_article_filter_1);
        chip2 = root.findViewById(R.id.chip_article_filter_2);
        chip3 = root.findViewById(R.id.chip_article_filter_3);
        chip4 = root.findViewById(R.id.chip_article_filter_4);
        chip5 = root.findViewById(R.id.chip_article_filter_5);

        progressBar = root.findViewById(R.id.pb_admin_articles);
        rvArticles = root.findViewById(R.id.rv_admin_articles);
        layoutEmpty = root.findViewById(R.id.layout_empty_articles);
        tvEmptyTitle = root.findViewById(R.id.tv_empty_articles_title);
        tvEmptyDesc = root.findViewById(R.id.tv_empty_articles_desc);
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Kiểm duyệt bài"));
        tabLayout.addTab(tabLayout.newTab().setText("Báo cáo vi phạm"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                currentFilter = "ALL";
                updateFilterChipLabels();
                updateChipStyles();

                if (currentTab == 0) {
                    rvArticles.setAdapter(articleAdapter);
                } else {
                    rvArticles.setAdapter(reportAdapter);
                }

                applyFilterAndSearch();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAdapters() {
        articleAdapter = new AdminArticleAdapter(requireContext(), this);
        reportAdapter = new AdminReportAdapter(this);

        rvArticles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvArticles.setAdapter(articleAdapter);
    }

    private void setupFilterChips() {
        View.OnClickListener chipListener = v -> {
            int id = v.getId();
            if (id == R.id.chip_article_filter_1) {
                currentFilter = "ALL";
            } else if (id == R.id.chip_article_filter_2) {
                currentFilter = "PENDING";
            } else if (id == R.id.chip_article_filter_3) {
                currentFilter = (currentTab == 0) ? "APPROVED" : "REPORTED_ARTICLE_DELETED";
            } else if (id == R.id.chip_article_filter_4) {
                currentFilter = (currentTab == 0) ? "REJECTED" : "DISMISSED";
            } else if (id == R.id.chip_article_filter_5) {
                currentFilter = "DELETED";
            }
            updateChipStyles();
            applyFilterAndSearch();
        };

        chip1.setOnClickListener(chipListener);
        chip2.setOnClickListener(chipListener);
        chip3.setOnClickListener(chipListener);
        chip4.setOnClickListener(chipListener);
        chip5.setOnClickListener(chipListener);

        updateFilterChipLabels();
        updateChipStyles();
    }

    private void updateFilterChipLabels() {
        if (currentTab == 0) {
            chip1.setText("Tất cả");
            chip2.setText("Chờ duyệt");
            chip3.setText("Đã duyệt");
            chip4.setText("Từ chối");
            chip5.setText("Đã xóa");
            chip5.setVisibility(View.VISIBLE);
        } else {
            chip1.setText("Tất cả");
            chip2.setText("Chờ xử lý");
            chip3.setText("Đã xử lý");
            chip4.setText("Đã bác bỏ");
            chip5.setVisibility(View.GONE);
        }
    }

    private void updateChipStyles() {
        if (currentTab == 0) {
            resetChip(chip1, "ALL".equals(currentFilter));
            resetChip(chip2, "PENDING".equals(currentFilter));
            resetChip(chip3, "APPROVED".equals(currentFilter));
            resetChip(chip4, "REJECTED".equals(currentFilter));
            resetChip(chip5, "DELETED".equals(currentFilter));
        } else {
            resetChip(chip1, "ALL".equals(currentFilter));
            resetChip(chip2, "PENDING".equals(currentFilter));
            resetChip(chip3, "REPORTED_ARTICLE_DELETED".equals(currentFilter));
            resetChip(chip4, "DISMISSED".equals(currentFilter));
        }
    }

    private void resetChip(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(null, Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_filter_chip_unselected);
            chip.setTextColor(Color.parseColor("#475569"));
            chip.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchKeyword = (s != null) ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                btnClearSearch.setVisibility(currentSearchKeyword.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    private void loadArticlesData() {
        if (articlesListener != null) articlesListener.remove();

        progressBar.setVisibility(View.VISIBLE);
        articlesListener = db.collection("articles")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);

                    if (error != null || snapshots == null || snapshots.isEmpty()) {
                        if (allArticles.isEmpty()) {
                            createMockArticles();
                        }
                        return;
                    }

                    allArticles.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        AdminArticleItem item = new AdminArticleItem();
                        item.setId(doc.getId());
                        item.setTitle(doc.getString("title"));
                        item.setAuthorName(doc.getString("authorName"));
                        item.setInstitution(doc.getString("institution"));
                        item.setCategory(doc.getString("category"));
                        item.setCategoryId(doc.getString("categoryId"));
                        item.setDescription(doc.getString("description"));
                        item.setContentUrl(doc.getString("contentUrl"));
                        item.setSourceUrl(doc.getString("sourceUrl"));
                        item.setDoi(doc.getString("digitalObjectIdentifier"));

                        String st = doc.getString("status");
                        item.setStatus(st != null && !st.trim().isEmpty() ? st : "PENDING");

                        Long repCount = doc.getLong("reportCount");
                        item.setReportCount(repCount != null ? repCount.intValue() : 0);
                        item.setUserId(doc.getString("userId"));
                        item.setCreatedAt(doc.getTimestamp("createdAt"));
                        item.setUpdatedAt(doc.getTimestamp("updatedAt"));

                        allArticles.add(item);
                    }

                    if (currentTab == 0) applyFilterAndSearch();
                });
    }

    private void createMockArticles() {
        allArticles.clear();

        allArticles.add(new AdminArticleItem(
                "mock_art_1",
                "Phát triển mô hình học sâu (Deep Learning) phát hiện sớm tổn thương phổi trên ảnh X-Quang lồng ngực",
                "PGS.TS. Trần Đình Khoa",
                "Viện Y học Ứng dụng & Đại học Quốc gia",
                "Y học & Dược phẩm",
                "Nghiên cứu áp dụng kiến trúc mạng tích chập sâu cải tiến (DenseNet-121) nhằm tối ưu độ nhạy trong phát hiện nốt mờ phổi giai đoạn sớm.",
                "PENDING"
        ));

        allArticles.add(new AdminArticleItem(
                "mock_art_2",
                "Tối ưu hóa thuật toán định tuyến cho mạng lưới Internet vạn vật (IoT) sử dụng trí tuệ nhân tạo biên",
                "TS. Nguyễn Anh Tài",
                "Trường Đại học Bách Khoa",
                "Công nghệ thông tin",
                "Bài báo trình bày giải pháp giảm thiểu độ trễ và tiêu thụ năng lượng trong các cụm cảm biến IoT thông qua thuật toán học tăng cường.",
                "PENDING"
        ));

        allArticles.add(new AdminArticleItem(
                "mock_art_3",
                "Khảo sát tính khả thi của vật liệu graphene trong pin lithium-ion thế hệ mới",
                "ThS. Dương Minh Triết",
                "Viện Khoa học Vật liệu Tiên tiến",
                "Vật liệu & Năng lượng",
                "Nghiên cứu thực nghiệm về độ bền chu kỳ và dung lượng riêng của điện cực nano composite trên nền tảng tấm graphene đa lớp.",
                "APPROVED"
        ));

        allArticles.add(new AdminArticleItem(
                "mock_art_4",
                "Ứng dụng công nghệ xử lý ảnh vệ tinh cảnh báo sớm sạt lở đất tại miền núi phía Bắc",
                "Nhóm Nghiên cứu Địa chất Môi trường",
                "Đại học Mỏ - Địa chất",
                "Khoa học Môi trường",
                "Bài viết có nhiều số liệu thiếu trích dẫn nguồn đo đạc thực địa và sử dụng ảnh tư liệu không có quyền sở hữu hợp lệ.",
                "REJECTED"
        ));

        if (currentTab == 0) applyFilterAndSearch();
    }

    private void loadReportsData() {
        if (reportsListener != null) reportsListener.remove();

        reportsListener = db.collection("articleReports")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshots == null || snapshots.isEmpty()) {
                        if (allReportItems.isEmpty()) {
                            createMockArticleReports();
                        }
                        return;
                    }

                    groupArticleReports(snapshots.getDocuments());
                });
    }

    private void groupArticleReports(List<DocumentSnapshot> docs) {
        allReportItems.clear();
        Map<String, List<DocumentSnapshot>> map = new HashMap<>();
        for (DocumentSnapshot doc : docs) {
            String artId = doc.getString("articleId");
            if (artId == null || artId.isEmpty()) continue;
            if (!map.containsKey(artId)) map.put(artId, new ArrayList<>());
            map.get(artId).add(doc);
        }

        for (Map.Entry<String, List<DocumentSnapshot>> entry : map.entrySet()) {
            String articleId = entry.getKey();
            List<DocumentSnapshot> reports = entry.getValue();
            String latestStatus = reports.get(0).getString("status");
            if (latestStatus == null || latestStatus.isEmpty()) latestStatus = "PENDING";

            final String statusToUse = latestStatus;
            db.collection("articles").document(articleId).get().addOnSuccessListener(aDoc -> {
                if (!isAdded()) return;
                String title = aDoc.exists() ? aDoc.getString("title") : "Bài báo khoa học";
                String author = aDoc.exists() ? aDoc.getString("authorName") : "Tác giả";
                String category = aDoc.exists() ? aDoc.getString("category") : "Khoa học";
                String imgUrl = aDoc.exists() ? aDoc.getString("imageUrl") : null;

                AdminReportItem item = new AdminReportItem(
                        reports.get(0).getId(),
                        articleId,
                        "ARTICLE",
                        title,
                        title,
                        null,
                        statusToUse,
                        reports.size(),
                        "Vừa xong",
                        author,
                        imgUrl,
                        Arrays.asList(category != null ? category : "Khoa học", "Bài báo")
                );
                item.setImageUrl(imgUrl);

                allReportItems.add(item);
                if (currentTab == 1) applyFilterAndSearch();
            });
        }
    }

    private void createMockArticleReports() {
        allReportItems.clear();

        allReportItems.add(new AdminReportItem(
                "art_rep_1",
                "mock_art_report_1",
                "ARTICLE",
                "Nghiên cứu về liệu pháp tế bào gốc trong điều trị lão hóa: Kết quả thử nghiệm giai đoạn 1",
                "Bài báo chứa nhiều đồ thị sai lệch tỷ lệ thống kê và quảng cáo trực tiếp cho một phòng khám tư nhân.",
                null,
                "PENDING",
                4,
                "3 giờ trước",
                "Dr. John Doe",
                null,
                Arrays.asList("Y học", "Tế bào gốc")
        ));

        allReportItems.add(new AdminReportItem(
                "art_rep_2",
                "mock_art_report_2",
                "ARTICLE",
                "Đánh giá trữ lượng khoáng sản hiếm tại khu vực Tây Bắc bằng phương pháp phân tích quang phổ",
                "Sao chép nguyên văn 80% luận văn tiến sĩ từ một trường đại học nước ngoài mà không dẫn xuất nguồn.",
                null,
                "PENDING",
                6,
                "1 ngày trước",
                "Lê Văn Nam",
                null,
                Arrays.asList("Địa chất", "Khoáng sản")
        ));

        if (currentTab == 1) applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        if (currentTab == 0) {
            List<AdminArticleItem> filtered = new ArrayList<>();
            for (AdminArticleItem item : allArticles) {
                boolean matchStatus = "ALL".equals(currentFilter) || currentFilter.equalsIgnoreCase(item.getStatus());
                if (!matchStatus) continue;

                if (!currentSearchKeyword.isEmpty()) {
                    String title = item.getTitle().toLowerCase(Locale.ROOT);
                    String author = item.getAuthorName().toLowerCase(Locale.ROOT);
                    String institution = item.getInstitution().toLowerCase(Locale.ROOT);
                    String category = item.getCategory().toLowerCase(Locale.ROOT);

                    if (!title.contains(currentSearchKeyword) && !author.contains(currentSearchKeyword)
                            && !institution.contains(currentSearchKeyword) && !category.contains(currentSearchKeyword)) {
                        continue;
                    }
                }
                filtered.add(item);
            }

            articleAdapter.setData(filtered);

            if (filtered.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvArticles.setVisibility(View.GONE);
                tvEmptyTitle.setText("Không có bài báo nào");
                tvEmptyDesc.setText("Không tìm thấy bài báo khoa học nào khớp với bộ lọc.");
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rvArticles.setVisibility(View.VISIBLE);
            }
        } else {
            List<AdminReportItem> filtered = new ArrayList<>();
            for (AdminReportItem item : allReportItems) {
                boolean matchStatus = "ALL".equals(currentFilter) || currentFilter.equalsIgnoreCase(item.getStatus());
                if (!matchStatus) continue;

                if (!currentSearchKeyword.isEmpty()) {
                    String title = item.getTitle() != null ? item.getTitle().toLowerCase(Locale.ROOT) : "";
                    String author = item.getAuthorName() != null ? item.getAuthorName().toLowerCase(Locale.ROOT) : "";

                    if (!title.contains(currentSearchKeyword) && !author.contains(currentSearchKeyword)) {
                        continue;
                    }
                }
                filtered.add(item);
            }

            Collections.sort(filtered, (a, b) -> {
                boolean aPending = "PENDING".equalsIgnoreCase(a.getStatus());
                boolean bPending = "PENDING".equalsIgnoreCase(b.getStatus());
                if (aPending != bPending) {
                    return aPending ? -1 : 1;
                }
                return Long.compare(b.getTimestamp(), a.getTimestamp());
            });

            reportAdapter.setItems(filtered);

            if (filtered.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvArticles.setVisibility(View.GONE);
                tvEmptyTitle.setText("Không có báo cáo bài báo");
                tvEmptyDesc.setText("Hiện tại không có bài báo nào bị phản ánh vi phạm.");
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rvArticles.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onApprove(AdminArticleItem article) {
        if (!isAdded()) return;

        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String adminId = user != null ? user.getUid() : "ADMIN";
        Timestamp now = Timestamp.now();

        if (article.getId() == null || article.getId().startsWith("mock_")) {
            progressBar.setVisibility(View.GONE);
            article.setStatus("APPROVED");
            applyFilterAndSearch();
            showSuccess("Đã duyệt bài báo", "Bài báo \"" + article.getTitle() + "\" đã được phê duyệt và hiển thị công khai.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "APPROVED");
        updates.put("updatedAt", now);

        db.collection("articles").document(article.getId()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> log = new HashMap<>();
                    log.put("action", "APPROVED");
                    log.put("articleId", article.getId());
                    log.put("adminId", adminId);
                    log.put("createdAt", now);
                    db.collection("articleApprovals").add(log);

                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    article.setStatus("APPROVED");
                    applyFilterAndSearch();
                    showSuccess("Đã duyệt bài báo", "Bài báo \"" + article.getTitle() + "\" đã được phê duyệt thành công.");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    article.setStatus("APPROVED");
                    applyFilterAndSearch();
                    showSuccess("Đã duyệt bài báo (Demo)", "Bài báo đã được chuyển sang trạng thái đã duyệt.");
                });
    }

    @Override
    public void onReject(AdminArticleItem article) {
        if (!isAdded()) return;

        AdminActionBottomSheetDialog dialog = AdminActionBottomSheetDialog.newInstance(
                AdminActionBottomSheetDialog.ACTION_DELETE,
                "ARTICLE",
                article.getTitle()
        );

        dialog.setOnActionConfirmListener(reason -> {
            progressBar.setVisibility(View.VISIBLE);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String adminId = user != null ? user.getUid() : "ADMIN";
            Timestamp now = Timestamp.now();

            if (article.getId() == null || article.getId().startsWith("mock_")) {
                dialog.dismiss();
                progressBar.setVisibility(View.GONE);
                article.setStatus("REJECTED");
                applyFilterAndSearch();
                showSuccess("Đã từ chối bài báo", "Bài báo đã bị từ chối phê duyệt với lý do đã nhập.");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "REJECTED");
            updates.put("updatedAt", now);

            db.collection("articles").document(article.getId()).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Map<String, Object> log = new HashMap<>();
                        log.put("action", "REJECTED");
                        log.put("articleId", article.getId());
                        log.put("adminId", adminId);
                        log.put("reason", reason);
                        log.put("createdAt", now);
                        db.collection("articleApprovals").add(log);

                        if (!isAdded()) return;
                        dialog.dismiss();
                        progressBar.setVisibility(View.GONE);
                        article.setStatus("REJECTED");
                        applyFilterAndSearch();
                        showSuccess("Đã từ chối bài báo", "Bài báo đã bị từ chối phê duyệt.");
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        dialog.dismiss();
                        progressBar.setVisibility(View.GONE);
                        article.setStatus("REJECTED");
                        applyFilterAndSearch();
                        showSuccess("Đã từ chối bài báo (Demo)", "Bài báo đã được chuyển sang trạng thái từ chối.");
                    });
        });

        dialog.show(getParentFragmentManager(), "RejectArticleDialog");
    }

    @Override
    public void onDelete(AdminArticleItem article) {
        if (!isAdded()) return;

        AdminActionBottomSheetDialog dialog = AdminActionBottomSheetDialog.newInstance(
                AdminActionBottomSheetDialog.ACTION_DELETE,
                "ARTICLE",
                article.getTitle()
        );

        dialog.setOnActionConfirmListener(reason -> {
            progressBar.setVisibility(View.VISIBLE);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String adminId = user != null ? user.getUid() : "ADMIN";
            Timestamp now = Timestamp.now();

            if (article.getId() == null || article.getId().startsWith("mock_")) {
                dialog.dismiss();
                progressBar.setVisibility(View.GONE);
                article.setStatus("DELETED");
                applyFilterAndSearch();
                showSuccess("Đã xóa bài báo", "Bài báo đã được xóa mềm thành công.");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "DELETED");
            updates.put("deletedAt", now);
            updates.put("updatedAt", now);

            db.collection("articles").document(article.getId()).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Map<String, Object> log = new HashMap<>();
                        log.put("action", "DELETED");
                        log.put("articleId", article.getId());
                        log.put("adminId", adminId);
                        log.put("reason", reason);
                        log.put("createdAt", now);
                        db.collection("articleApprovals").add(log);

                        if (!isAdded()) return;
                        dialog.dismiss();
                        progressBar.setVisibility(View.GONE);
                        article.setStatus("DELETED");
                        applyFilterAndSearch();
                        showSuccess("Đã xóa bài báo", "Bài báo đã được chuyển sang trạng thái đã xóa.");
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        dialog.dismiss();
                        progressBar.setVisibility(View.GONE);
                        article.setStatus("DELETED");
                        applyFilterAndSearch();
                        showSuccess("Đã xóa bài báo (Demo)", "Bài báo đã được chuyển sang trạng thái đã xóa.");
                    });
        });

        dialog.show(getParentFragmentManager(), "DeleteArticleDialog");
    }

    @Override
    public void onView(AdminArticleItem article) {
        if (getContext() == null) return;

        Intent intent = new Intent(getContext(), ArticleDetailActivity.class);
        intent.putExtra("title", article.getTitle());
        intent.putExtra("authorName", article.getAuthorName());
        intent.putExtra("categoryId", article.getCategoryId() != null ? article.getCategoryId() : article.getCategory());
        intent.putExtra("description", article.getDescription());
        intent.putExtra("institution", article.getInstitution());
        intent.putExtra("digitalObjectIdentifier", article.getDoi());
        intent.putExtra("contentUrl", article.getContentUrl());
        if (article.getCreatedAt() != null) {
            intent.putExtra("createdAt", article.getCreatedAt().toDate().getTime());
        }
        startActivity(intent);
    }

    @Override
    public void onReportClick(AdminReportItem item) {
        if (getContext() == null) return;

        Intent intent = new Intent(getContext(), AdminReportDetailActivity.class);
        intent.putExtra(AdminReportDetailActivity.EXTRA_TARGET_ID, item.getContentId());
        intent.putExtra(AdminReportDetailActivity.EXTRA_TARGET_TYPE, "ARTICLE");
        intent.putExtra(AdminReportDetailActivity.EXTRA_STATUS, item.getStatus());
        intent.putExtra(AdminReportDetailActivity.EXTRA_TITLE, item.getTitle());
        intent.putExtra(AdminReportDetailActivity.EXTRA_CONTENT, item.getContent());
        intent.putExtra(AdminReportDetailActivity.EXTRA_REPORT_COUNT, item.getReportCount());
        intent.putExtra(AdminReportDetailActivity.EXTRA_AUTHOR_NAME, item.getAuthorName());
        intent.putExtra(AdminReportDetailActivity.EXTRA_AUTHOR_AVATAR, item.getAuthorAvatar());
        intent.putExtra(AdminReportDetailActivity.EXTRA_IMAGE_URL, item.getImageUrl());
        intent.putExtra(AdminReportDetailActivity.EXTRA_POST_TIME, item.getTimeAgo());
        if (item.getTags() != null && !item.getTags().isEmpty()) {
            intent.putExtra(AdminReportDetailActivity.EXTRA_TAG, item.getTags().get(0));
        }
        startActivity(intent);
    }

    private void showSuccess(String title, String message) {
        if (!isAdded()) return;
        AdminSuccessDialog dialog = AdminSuccessDialog.newInstance(title, message);
        dialog.show(getParentFragmentManager(), "AdminArticleSuccess");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            applyFilterAndSearch();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (articlesListener != null) {
            articlesListener.remove();
            articlesListener = null;
        }
        if (reportsListener != null) {
            reportsListener.remove();
            reportsListener = null;
        }
    }
}