package com.example.myapplication.account.myarticles;
import android.view.View;
import android.os.Bundle;
import android.util.Log;

import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
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

    private final List<DocumentSnapshot> allArticles =
            new ArrayList<>();
    private final List<DocumentSnapshot> filteredArticles =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_articles);

        View myArticlesBackLayout =
                findViewById(R.id.myArticlesBackLayout);

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

        rvMyArticles.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new MyArticlesAdapter(new ArrayList<>());
        rvMyArticles.setAdapter(adapter);

        rgArticleFilter.setOnCheckedChangeListener(
                (group, checkedId) -> applyFilter()
        );

        loadMyArticles();
    }

    private void loadMyArticles() {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập để xem bài báo của bạn.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvMyArticles.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("articles")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    progressBar.setVisibility(View.GONE);

                    allArticles.clear();

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {

                        Timestamp deletedAt =
                                document.getTimestamp("deletedAt");

                        String status =
                                document.getString("status");

                        if (deletedAt != null) {
                            continue;
                        }

                        if ("DELETED".equalsIgnoreCase(status)) {
                            continue;
                        }

                        allArticles.add(document);
                    }

                    Collections.sort(
                            allArticles,
                            new Comparator<DocumentSnapshot>() {
                                @Override
                                public int compare(
                                        DocumentSnapshot first,
                                        DocumentSnapshot second) {

                                    return Long.compare(
                                            getCreatedAtMillis(second),
                                            getCreatedAtMillis(first)
                                    );
                                }
                            }
                    );

                    applyFilter();
                })
                .addOnFailureListener(error -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    progressBar.setVisibility(View.GONE);

                    Log.e(
                            "MyArticlesActivity",
                            "Không thể tải bài báo",
                            error
                    );

                    tvEmpty.setText(
                            "Không thể tải danh sách bài báo. "
                                    + "Vui lòng thử lại."
                    );

                    tvEmpty.setVisibility(View.VISIBLE);
                    rvMyArticles.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Không thể tải bài báo của bạn.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
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

        for (DocumentSnapshot article : allArticles) {

            if (selectedStatus == null) {
                filteredArticles.add(article);
                continue;
            }

            String status = article.getString("status");

            if (selectedStatus.equalsIgnoreCase(
                    status == null ? "" : status
            )) {
                filteredArticles.add(article);
            }
        }

        adapter.setArticles(filteredArticles);

        tvSummary.setText(
                "Bạn có " + filteredArticles.size() + " bài báo"
        );

        if (filteredArticles.isEmpty()) {

            if (selectedStatus == null) {
                tvEmpty.setText("Bạn chưa có bài báo nào.");
            } else {
                tvEmpty.setText(
                        "Không có bài báo nào thuộc trạng thái này."
                );
            }

            tvEmpty.setVisibility(View.VISIBLE);
            rvMyArticles.setVisibility(View.GONE);

        } else {
            tvEmpty.setVisibility(View.GONE);
            rvMyArticles.setVisibility(View.VISIBLE);
        }
    }

    private static long getCreatedAtMillis(
            @NonNull DocumentSnapshot document
    ) {
        Timestamp timestamp =
                document.getTimestamp("createdAt");

        return timestamp == null
                ? 0L
                : timestamp.toDate().getTime();
    }
}
