package com.example.myapplication.articles.search;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
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
import com.example.myapplication.articles.ArticleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchResultsActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY = "extra_query";

    private TextView searchQuery;
    private TextView resultCount;
    private RecyclerView resultList;

    private SearchResultsAdapter searchAdapter;
    private ArticleRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_search_results);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.root);
        View statusBarSpacer = findViewById(R.id.status_bar_spacer);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.LayoutParams params = statusBarSpacer.getLayoutParams();
            params.height = bars.top;
            statusBarSpacer.setLayoutParams(params);

            v.setPadding(bars.left, 0, bars.right, bars.bottom);

            return WindowInsetsCompat.CONSUMED;
        });

        ImageButton backButton = findViewById(R.id.btn_back);
        searchQuery = findViewById(R.id.tv_search_query);
        resultCount = findViewById(R.id.tv_search_result_count);
        resultList = findViewById(R.id.rv_search_results);

        String query = getIntent().getStringExtra(EXTRA_QUERY);

        if (query == null) {
            query = "";
        }

        query = query.trim();
        searchQuery.setText("Results for \"" + query + "\"");

        resultList.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchResultsAdapter();
        resultList.setAdapter(searchAdapter);

        repository = new ArticleRepository();

        backButton.setOnClickListener(v -> finish());

        if (query.isEmpty()) {
            resultCount.setText("Please enter a search keyword.");
            return;
        }

        searchArticles(query);
    }

    private void searchArticles(String query) {
        resultCount.setText("Searching...");
        resultList.setVisibility(View.GONE);

        repository.getLatestArticles(new ArticleRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<Article> articles) {
                List<Article> matchedArticles = new ArrayList<>();

                String keyword = query.toLowerCase(Locale.ROOT);

                for (Article article : articles) {
                    if (matches(article.getTitle(), keyword)
                            || matches(article.getAuthorName(), keyword)
                            || matches(article.getInstitution(), keyword)
                            || matches(article.getDigitalObjectIdentifier(), keyword)) {
                        matchedArticles.add(article);
                    }
                }

                searchAdapter.setArticles(matchedArticles);
                resultCount.setText(matchedArticles.size() + " results");
                resultList.setVisibility(
                        matchedArticles.isEmpty() ? View.GONE : View.VISIBLE
                );

                if (matchedArticles.isEmpty()) {
                    resultCount.setText("No articles found. Try another keyword.");
                }
            }

            @Override
            public void onError(Exception exception) {
                android.util.Log.e(
                        "SearchResultsActivity",
                        "Failed to search articles",
                        exception
                );

                resultCount.setText("Unable to search articles. Please try again.");
                resultList.setVisibility(View.GONE);
            }
        });
    }

    private boolean matches(String value, String keyword) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}