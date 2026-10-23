package com.example.myapplication;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import com.example.myapplication.articles.articlesDetail.ArticleDetailActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.articles.Article;
import com.example.myapplication.articles.ArticleAdapter;
import com.example.myapplication.articles.ArticleRepository;
import com.example.myapplication.articles.categories.ArticleCategory;
import com.example.myapplication.articles.categories.ArticleCategoryRepository;

import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView articleList;
    private ArticleAdapter articleAdapter;
    private View emptyState;
    private LinearLayout categoryChipContainer;

    private ArticleRepository repository;
    private ArticleCategoryRepository categoryRepository;


    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        articleList = view.findViewById(R.id.rv_article_list);
        emptyState = view.findViewById(R.id.layout_empty_articles);

        articleAdapter = new ArticleAdapter(
                article -> {
                    Intent intent = new Intent(requireContext(), ArticleDetailActivity.class);
                    intent.putExtra("articleId", article.getId());
                    intent.putExtra("title", article.getTitle());
                    intent.putExtra("authorName", article.getAuthorName());
                    intent.putExtra("categoryId", article.getCategoryId());
                    intent.putExtra("institution", article.getInstitution());
                    intent.putExtra("digitalObjectIdentifier", article.getDigitalObjectIdentifier());
                    intent.putExtra("description", article.getDescription());
                    intent.putExtra("imageUrl", article.getImageUrl());
                    intent.putExtra("sourceUrl", article.getSourceUrl());
                    intent.putExtra("contentUrl", article.getContentUrl());
                    intent.putExtra("createdAt", article.getCreatedAt());
                    startActivity(intent);
                },
                article -> Toast.makeText(requireContext(), "Report feature will be added later.", Toast.LENGTH_SHORT).show(),
                categoryId -> loadArticles(categoryId)
        );

        articleList.setLayoutManager(new LinearLayoutManager(requireContext()));
        articleList.setAdapter(articleAdapter);

        repository = new ArticleRepository();
        categoryRepository = new ArticleCategoryRepository();

        loadArticles(null); //null = all
    }

    private void addCategoryChip(String name, String categoryId, boolean selected) {
        TextView chip = new TextView(requireContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(40)
        );
        params.setMarginEnd(dpToPx(8));

        chip.setLayoutParams(params);
        chip.setText(name);
        chip.setTextSize(13);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        chip.setMaxLines(1);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setBackgroundResource(R.drawable.bg_category_chip);

        updateChipStyle(chip, selected);

        chip.setOnClickListener(v -> {
            if (!isAdded()) {
                return;
            }

            for (int i = 0; i < categoryChipContainer.getChildCount(); i++) {
                View child = categoryChipContainer.getChildAt(i);
                if (child instanceof TextView) {
                    updateChipStyle(child, child == chip);
                }
            }

            if (categoryId == null) {
                loadArticles(null); //null = all
            } else {
                Toast.makeText(
                        requireContext(),
                        "Selected category: " + name,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        categoryChipContainer.addView(chip);
    }

    private void updateChipStyle(View view, boolean selected) {
        if (!(view instanceof TextView)) {
            return;
        }

        TextView chip = (TextView) view;

        if (selected) {
            chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2457C5")));
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
            chip.setTextColor(Color.parseColor("#475569"));
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void loadArticles(String categoryId) {
        articleList.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        ArticleRepository.OnArticlesLoadedListener listener = new ArticleRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<Article> articles) {
                if (!isAdded()) {
                    return;
                }

                articleAdapter.setArticles(articles);

                emptyState.setVisibility(articles.isEmpty() ? View.VISIBLE : View.GONE);
                articleList.setVisibility(articles.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) {
                    return;
                }

                android.util.Log.e("HomeFragment", "Failed to load articles", exception);
                Toast.makeText(requireContext(), "Unable to load articles.", Toast.LENGTH_SHORT).show();

                articleList.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            }
        };

        if (categoryId == null) {
            repository.getLatestArticles(listener);
        } else {
            repository.getArticlesByCategory(categoryId, listener);
        }
    }
}