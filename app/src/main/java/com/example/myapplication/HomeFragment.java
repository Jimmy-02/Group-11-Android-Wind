package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;
import android.graphics.Color;

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
import com.example.myapplication.articles.search.SearchResultsActivity;
import com.example.myapplication.articles.articlesDetail.ArticleDetailActivity;
import com.example.myapplication.articles.creation.CreateArticleActivity;
import com.example.myapplication.articles.reports.ReportArticleActivity;
import com.example.myapplication.data.local.ArticleLocalDataSource;
import com.example.myapplication.data.local.CategoryLocalDataSource;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView articleList;
    private ArticleAdapter articleAdapter;
    private ArticleRepository repository;
    private ArticleCategoryRepository categoryRepository;
    private ListenerRegistration articleListener;
    private ArticleLocalDataSource localDataSource;
    private CategoryLocalDataSource categoryLocalDataSource;

    private String currentCategoryId;
    private boolean isLandscapeLayout;

    private View homeHeader;
    private EditText searchInput;
    private LinearLayout categoryChipContainer;
    private TextView articleCount;
    private ImageButton createArticleButton;

    private final Map<String, String> categoryNames = new HashMap<>();

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        articleList = view.findViewById(R.id.rv_article_list);
        homeHeader = view.findViewById(R.id.home_header);
        isLandscapeLayout = homeHeader != null;

        repository = new ArticleRepository();
        categoryRepository = new ArticleCategoryRepository();
        localDataSource = new ArticleLocalDataSource(requireContext());
        categoryLocalDataSource = new CategoryLocalDataSource(requireContext());

        if (isLandscapeLayout) {
            setupLandscapeHeader();
        }

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
                article -> {
                    Intent intent = new Intent(requireContext(), ReportArticleActivity.class);
                    intent.putExtra("articleId", article.getId());
                    intent.putExtra("title", article.getTitle());
                    intent.putExtra("authorName", article.getAuthorName());
                    startActivity(intent);
                },
                categoryId -> loadArticles(categoryId),
                () -> {
                    Intent intent = new Intent(requireContext(), CreateArticleActivity.class);
                    startActivity(intent);
                },
                !isLandscapeLayout
        );

        articleList.setLayoutManager(new LinearLayoutManager(requireContext()));
        articleList.setAdapter(articleAdapter);

        loadArticles(null);

        if (isLandscapeLayout) {
            loadCategories();
        }
    }

    private void setupLandscapeHeader() {
        searchInput = homeHeader.findViewById(R.id.et_search_articles);
        categoryChipContainer = homeHeader.findViewById(R.id.category_chip_container);
        articleCount = homeHeader.findViewById(R.id.tv_article_count);
        createArticleButton = homeHeader.findViewById(R.id.btn_create_article);

        if (articleCount != null) {
            articleCount.setText("0 articles");
        }

        if (createArticleButton != null) {
            createArticleButton.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CreateArticleActivity.class);
                startActivity(intent);
            });
        }

        if (searchInput != null) {
            searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            searchInput.setSingleLine(true);

            searchInput.setOnEditorActionListener((input, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchInput.getText().toString().trim();

                    if (query.isEmpty()) {
                        searchInput.setError("Please enter a search keyword");
                        return true;
                    }

                    InputMethodManager keyboard = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (keyboard != null) {
                        keyboard.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                    }

                    Intent intent = new Intent(requireContext(), SearchResultsActivity.class);
                    intent.putExtra(SearchResultsActivity.EXTRA_QUERY, query);
                    startActivity(intent);
                    return true;
                }

                return false;
            });
        }
    }

    private void loadCategories() {
        if (!isAdded() || categoryChipContainer == null || categoryLocalDataSource == null) {
            return;
        }

        // Hiển thị dữ liệu SQLite trước để giao diện không bị trống khi offline.
        List<ArticleCategory> cachedCategories = categoryLocalDataSource.getCategories();
        if (!cachedCategories.isEmpty()) {
            showCategories(cachedCategories);
        } else {
            categoryChipContainer.removeAllViews();
            addCategoryChip("All", null);
        }

        categoryRepository.getActiveCategories(new ArticleCategoryRepository.OnCategoriesLoadedListener() {
            @Override
            public void onSuccess(List<ArticleCategory> categories) {
                if (!isAdded() || categoryChipContainer == null) {
                    return;
                }

                if (categories != null && !categories.isEmpty()) {
                    try {
                        categoryLocalDataSource.saveCategories(categories);
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Unable to cache categories", e);
                    }

                    showCategories(categories);
                } else if (categoryChipContainer.getChildCount() == 0) {
                    addCategoryChip("All", null);
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.e("HomeFragment", "Failed to load categories from Firestore", exception);

                if (!isAdded() || categoryChipContainer == null) {
                    return;
                }

                List<ArticleCategory> fallbackCategories = categoryLocalDataSource.getCategories();
                showCategories(fallbackCategories);

                if (fallbackCategories.isEmpty()) {
                    Toast.makeText(requireContext(), "Unable to load categories.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showCategories(List<ArticleCategory> categories) {
        if (categoryChipContainer == null) {
            return;
        }

        categoryChipContainer.removeAllViews();
        categoryNames.clear();

        addCategoryChip("All", null);

        if (categories == null) {
            return;
        }

        for (ArticleCategory category : categories) {
            if (category == null || category.getId() == null || category.getName() == null) {
                continue;
            }

            categoryNames.put(category.getId(), category.getName());
            addCategoryChip(category.getName(), category.getId());
        }

        if (articleAdapter != null) {
            articleAdapter.setCategoryNames(categoryNames);
        }
    }

    private void addCategoryChip(String name, String categoryId) {
        if (categoryChipContainer == null || !isAdded()) {
            return;
        }

        TextView chip = (TextView) LayoutInflater.from(requireContext()).inflate(
                R.layout.item_category_chip,
                categoryChipContainer,
                false
        );

        chip.setText(name);
        updateCategoryChipStyle(chip, isSameCategory(categoryId, currentCategoryId));

        chip.setOnClickListener(v -> {
            for (int i = 0; i < categoryChipContainer.getChildCount(); i++) {
                View child = categoryChipContainer.getChildAt(i);
                if (child instanceof TextView) {
                    updateCategoryChipStyle((TextView) child, child == chip);
                }
            }

            loadArticles(categoryId);
        });

        categoryChipContainer.addView(chip);
    }

    private boolean isSameCategory(String first, String second) {
        if (first == null || first.trim().isEmpty()) {
            return second == null || second.trim().isEmpty();
        }
        return first.equals(second);
    }

    private void updateCategoryChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(selected ? "#2457C5" : "#F1F5F9")
        ));
        chip.setTextColor(Color.parseColor(selected ? "#FFFFFF" : "#475569"));
    }

    private List<Article> getCachedArticles(String categoryId) {
        List<Article> cachedArticles = new ArrayList<>(localDataSource.getHomeArticles());

        if (categoryId != null && !categoryId.trim().isEmpty()) {
            cachedArticles.removeIf(article -> !categoryId.equals(article.getCategoryId()));
        }

        return cachedArticles;
    }

    private void loadArticles(String categoryId) {
        if (!isAdded() || repository == null || articleAdapter == null || localDataSource == null) {
            return;
        }

        currentCategoryId = categoryId;

        if (articleListener != null) {
            articleListener.remove();
            articleListener = null;
        }

        List<Article> cachedArticles = getCachedArticles(categoryId);
        Log.d("OfflineCache", "Cached articles count: " + cachedArticles.size());

        if (!cachedArticles.isEmpty()) {
            articleAdapter.setArticles(cachedArticles);
            updateArticleCount(cachedArticles.size());
        } else {
            articleAdapter.setArticles(new ArrayList<>());
            updateArticleCount(0);
        }

        articleListener = repository.listenApprovedArticles(categoryId, new ArticleRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<Article> articles) {
                if (!isAdded() || articleAdapter == null || localDataSource == null) {
                    return;
                }

                Log.d("OfflineCache", "Firestore loaded: " + articles.size() + " articles");

                if (categoryId == null || categoryId.trim().isEmpty()) {
                    localDataSource.saveHomeArticles(articles);
                }

                articleAdapter.setArticles(articles);
                updateArticleCount(articles.size());
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded() || articleAdapter == null || localDataSource == null) {
                    return;
                }

                Log.e("OfflineCache", "Firestore failed or offline; checking SQLite cache", exception);

                List<Article> fallbackArticles = getCachedArticles(categoryId);
                articleAdapter.setArticles(fallbackArticles);
                updateArticleCount(fallbackArticles.size());

                if (!fallbackArticles.isEmpty()) {
                    Toast.makeText(requireContext(), "Showing saved articles offline.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Unable to load articles.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateArticleCount(int count) {
        if (articleCount != null) {
            articleCount.setText(count + (count == 1 ? " article" : " articles"));
        }
    }

    @Override
    public void onDestroyView() {
        removeArticleListener();

        articleList = null;
        articleAdapter = null;
        homeHeader = null;
        searchInput = null;
        categoryChipContainer = null;
        articleCount = null;
        createArticleButton = null;

        super.onDestroyView();
    }

    public void removeArticleListener() {
        if (articleListener != null) {
            articleListener.remove();
            articleListener = null;
        }
    }
}