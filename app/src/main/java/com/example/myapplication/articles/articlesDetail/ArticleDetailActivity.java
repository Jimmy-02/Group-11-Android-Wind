package com.example.myapplication.articles.articlesDetail;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.myapplication.R;
import com.example.myapplication.articles.categories.ArticleCategory;
import com.example.myapplication.articles.categories.ArticleCategoryRepository;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArticleDetailActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvCategory;
    private TextView tvDescription;
    private TextView tvInstitution;
    private TextView tvDoi;
    private TextView tvDate;
    private MaterialButton btnReadArticle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_article_detail);

        // Status bar nền tím nên icon màu trắng (giống màn Search Results)
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.root);
        View statusBarSpacer = findViewById(R.id.status_bar_spacer);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.LayoutParams params = statusBarSpacer.getLayoutParams();
            params.height = bars.top;
            statusBarSpacer.setLayoutParams(params);

            v.setPadding(bars.left, 0, bars.right, bars.bottom);

            return WindowInsetsCompat.CONSUMED;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvAuthor = findViewById(R.id.tv_detail_author);
        tvCategory = findViewById(R.id.tv_detail_category);
        tvDescription = findViewById(R.id.tv_detail_description);
        tvInstitution = findViewById(R.id.tv_detail_institution);
        tvDoi = findViewById(R.id.tv_detail_doi);
        tvDate = findViewById(R.id.tv_detail_date);
        btnReadArticle = findViewById(R.id.btn_read_article);

        btnBack.setOnClickListener(v -> finish());

        Intent intent = getIntent();

        String title = intent.getStringExtra("title");
        String authorName = intent.getStringExtra("authorName");
        String categoryId = intent.getStringExtra("categoryId");
        String description = intent.getStringExtra("description");
        String institution = intent.getStringExtra("institution");
        String doi = intent.getStringExtra("digitalObjectIdentifier");
        String contentUrl = intent.getStringExtra("contentUrl");

        long createdAt = intent.getLongExtra("createdAt", -1L);

        tvTitle.setText(valueOrDefault(title, "Untitled Article"));
        tvAuthor.setText("By " + valueOrDefault(authorName, "Unknown Author"));
        tvDescription.setText(valueOrDefault(description, "No description available."));
        tvInstitution.setText(valueOrDefault(institution, "Not provided"));
        tvDoi.setText(valueOrDefault(doi, "Not provided"));

        loadCategoryName(categoryId);

        if (createdAt > 0) {
            tvDate.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(createdAt)));
        } else {
            tvDate.setText("Not available");
        }

        btnReadArticle.setOnClickListener(v -> {
            if (contentUrl == null || contentUrl.trim().isEmpty()) {
                android.widget.Toast.makeText(this, "PDF URL is unavailable.", android.widget.Toast.LENGTH_LONG).show();
                return;
            }

            Intent pdfIntent = new Intent(this, PdfReaderActivity.class);
            pdfIntent.putExtra("contentUrl", contentUrl);
            pdfIntent.putExtra("articleTitle", title);
            startActivity(pdfIntent);
        });
    }

    private void loadCategoryName(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            tvCategory.setText("Uncategorized");
            return;
        }

        ArticleCategoryRepository categoryRepository =
                new ArticleCategoryRepository();

        categoryRepository.getActiveCategories(
                new ArticleCategoryRepository.OnCategoriesLoadedListener() {
                    @Override
                    public void onSuccess(List<ArticleCategory> categories) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        for (ArticleCategory category : categories) {
                            if (categoryId.equals(category.getId())
                                    || categoryId.equals(category.getSlug())) {

                                tvCategory.setText(
                                        valueOrDefault(
                                                category.getName(),
                                                "Uncategorized"
                                        )
                                );
                                return;
                            }
                        }

                        // Không tìm thấy thì hiển thị ID ban đầu.
                        tvCategory.setText(categoryId);
                    }

                    @Override
                    public void onError(Exception exception) {
                        android.util.Log.e(
                                "ArticleDetailActivity",
                                "Failed to load category",
                                exception
                        );

                        if (!isFinishing() && !isDestroyed()) {
                            tvCategory.setText(categoryId);
                        }
                    }
                }
        );
    }
    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}