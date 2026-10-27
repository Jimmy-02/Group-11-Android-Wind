package com.example.myapplication.account.myarticles;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.articles.Article;
import com.example.myapplication.data.local.ArticleLocalDataSource;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyArticlesActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvSummary;
    private TextView tvEmpty;
    private RecyclerView rvMyArticles;

    private RadioGroup rgArticleFilter;
    private RadioButton rbFilterAll;
    private RadioButton rbFilterPending;
    private RadioButton rbFilterApproved;
    private RadioButton rbFilterRejected;

    private MyArticlesAdapter adapter;
    private ArticleLocalDataSource localDataSource;

    private final List<Article> allArticles = new ArrayList<>();
    private final List<Article> filteredArticles = new ArrayList<>();

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_my_articles);

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

        View myArticlesBackLayout = findViewById(R.id.myArticlesBackLayout);
        myArticlesBackLayout.setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.pbMyArticles);
        tvSummary = findViewById(R.id.tvMyArticlesSummary);
        tvEmpty = findViewById(R.id.tvMyArticlesEmpty);
        rvMyArticles = findViewById(R.id.rvMyArticles);

        rgArticleFilter = findViewById(R.id.rgArticleFilter);
        rbFilterAll = findViewById(R.id.rbFilterAll);
        rbFilterPending = findViewById(R.id.rbFilterPending);
        rbFilterApproved = findViewById(R.id.rbFilterApproved);
        rbFilterRejected = findViewById(R.id.rbFilterRejected);

        rvMyArticles.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyArticlesAdapter(new ArrayList<>());
        rvMyArticles.setAdapter(adapter);

        localDataSource = new ArticleLocalDataSource(this);

        rgArticleFilter.setOnCheckedChangeListener((group, checkedId) -> applyFilter());

        loadMyArticles();
    }

    private void loadMyArticles() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem bài báo của bạn.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = user.getUid();

        progressBar.setVisibility(View.VISIBLE);
        rvMyArticles.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("articles")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    List<Article> fetchedArticles = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String status = document.getString("status");

                        if ("DELETED".equalsIgnoreCase(status)) {
                            continue;
                        }

                        Article article = document.toObject(Article.class);

                        if (article == null) {
                            continue;
                        }

                        article.setId(document.getId());
                        article.setStatus(status == null ? "PENDING" : status);

                        Timestamp createdAt = document.getTimestamp("createdAt");
                        article.setCreatedAt(createdAt == null ? 0L : createdAt.toDate().getTime());

                        fetchedArticles.add(article);
                    }

                    Collections.sort(fetchedArticles, (first, second) -> Long.compare(second.getCreatedAt(), first.getCreatedAt()));

                    allArticles.clear();
                    allArticles.addAll(fetchedArticles);

                    try {
                        localDataSource.saveArticles(currentUserId, fetchedArticles);
                    } catch (Exception e) {
                        Log.e("MyArticlesActivity", "Không thể lưu cache SQLite", e);
                    }

                    progressBar.setVisibility(View.GONE);
                    applyFilter();
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Log.e("MyArticlesActivity", "Không thể tải bài báo từ Firestore", error);

                    loadCachedArticles();
                });
    }

    private void loadCachedArticles() {
        try {
            List<Article> cachedArticles = localDataSource.getArticles(currentUserId);

            allArticles.clear();
            allArticles.addAll(cachedArticles);

            progressBar.setVisibility(View.GONE);

            if (cachedArticles.isEmpty()) {
                tvEmpty.setText("Không có dữ liệu offline. Hãy kết nối mạng và mở lại trang này.");
                tvEmpty.setVisibility(View.VISIBLE);
                rvMyArticles.setVisibility(View.GONE);
                tvSummary.setText("Bạn có 0 bài báo");

                Toast.makeText(this, "Không có dữ liệu bài báo đã lưu offline.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Đang hiển thị dữ liệu đã lưu offline.", Toast.LENGTH_SHORT).show();
            applyFilter();
        } catch (Exception e) {
            Log.e("MyArticlesActivity", "Không thể đọc cache SQLite", e);

            progressBar.setVisibility(View.GONE);
            tvEmpty.setText("Không thể tải danh sách bài báo.");
            tvEmpty.setVisibility(View.VISIBLE);
            rvMyArticles.setVisibility(View.GONE);
        }
    }

    private void applyFilter() {
        filteredArticles.clear();

        int checkedId = rgArticleFilter.getCheckedRadioButtonId();
        String selectedStatus = null;

        if (checkedId == R.id.rbFilterPending) {
            selectedStatus = "PENDING";
        } else if (checkedId == R.id.rbFilterApproved) {
            selectedStatus = "APPROVED";
        } else if (checkedId == R.id.rbFilterRejected) {
            selectedStatus = "REJECTED";
        }

        for (Article article : allArticles) {
            if (selectedStatus == null) {
                filteredArticles.add(article);
                continue;
            }

            String status = article.getStatus();

            if (selectedStatus.equalsIgnoreCase(status == null ? "" : status)) {
                filteredArticles.add(article);
            }
        }

        adapter.setArticles(filteredArticles);

        tvSummary.setText("Bạn có " + filteredArticles.size() + " bài báo");

        if (filteredArticles.isEmpty()) {
            if (selectedStatus == null) {
                tvEmpty.setText("Bạn chưa có bài báo nào.");
            } else {
                tvEmpty.setText("Không có bài báo nào thuộc trạng thái này.");
            }

            tvEmpty.setVisibility(View.VISIBLE);
            rvMyArticles.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvMyArticles.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        if (localDataSource != null) {
            localDataSource.close();
        }

        super.onDestroy();
    }
}