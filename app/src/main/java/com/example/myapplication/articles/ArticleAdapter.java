package com.example.myapplication.articles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.Intent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Color;
import android.widget.LinearLayout;
import com.example.myapplication.articles.categories.ArticleCategory;
import com.example.myapplication.articles.categories.ArticleCategoryRepository;
import com.example.myapplication.articles.creation.CreateArticleActivity;
import com.example.myapplication.R;
import com.example.myapplication.data.local.CategoryLocalDataSource;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import com.example.myapplication.articles.search.SearchResultsActivity;

public class ArticleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ARTICLE = 1;

    private final List<Article> articleList = new ArrayList<>();
    private final Map<String, String> categoryNames = new HashMap<>();
    private final ArticleCategoryRepository categoryRepository = new ArticleCategoryRepository();
    private CategoryLocalDataSource categoryLocalDataSource;

    private final OnArticleClickListener articleClickListener;
    private final OnReportClickListener reportClickListener;
    private final OnCategorySelectedListener categorySelectedListener;
    private final OnCreateArticleClickListener createArticleClickListener;
    private boolean categoriesLoaded = false;
    private final boolean showHeader;
    public interface OnArticleClickListener {
        void onArticleClick(Article article);
    }

    public interface OnReportClickListener {
        void onReportClick(Article article);
    }

    public interface OnCategorySelectedListener {
        void onCategorySelected(String categoryId);
    }
    public interface OnCreateArticleClickListener {
        void onCreateArticleClick();
    }


    public ArticleAdapter(OnArticleClickListener articleClickListener, OnReportClickListener reportClickListener, OnCategorySelectedListener categorySelectedListener, OnCreateArticleClickListener createArticleClickListener) {
        this(articleClickListener, reportClickListener, categorySelectedListener, createArticleClickListener, true);
    }

    public ArticleAdapter(OnArticleClickListener articleClickListener, OnReportClickListener reportClickListener, OnCategorySelectedListener categorySelectedListener, OnCreateArticleClickListener createArticleClickListener, boolean showHeader) {
        this.articleClickListener = articleClickListener;
        this.reportClickListener = reportClickListener;
        this.categorySelectedListener = categorySelectedListener;
        this.createArticleClickListener = createArticleClickListener;
        this.showHeader = showHeader;
    }

    @Override
    public int getItemViewType(int position) {
        return showHeader && position == 0 ? TYPE_HEADER : TYPE_ARTICLE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_header, parent, false);
            return new HeaderViewHolder(view, this);
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(articleList.size());
            return;
        }

        int articlePosition = showHeader ? position - 1 : position;
        ArticleViewHolder articleHolder = (ArticleViewHolder) holder;
        Article article = articleList.get(articlePosition);

        articleHolder.titleView.setText(article.getTitle());
        articleHolder.authorView.setText(article.getAuthorName());

        String categoryId = article.getCategoryId();
        String categoryName = categoryNames.get(categoryId);
        articleHolder.categoryView.setText(categoryName != null ? categoryName : "Other");

        String institution = article.getInstitution();
        String doi = article.getDigitalObjectIdentifier();
        StringBuilder metadata = new StringBuilder();

        if (institution != null && !institution.trim().isEmpty()) {
            metadata.append(institution.trim());
        }

        if (doi != null && !doi.trim().isEmpty()) {
            if (metadata.length() > 0) {
                metadata.append(" · ");
            }
            metadata.append("DOI: ").append(doi.trim());
        }

        if (metadata.length() == 0) {
            metadata.append("Scientific article");
        }

        articleHolder.metadataView.setText(metadata.toString());

        String imageUrl = article.getImageUrl();

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Picasso.get().load(imageUrl).placeholder(android.R.drawable.ic_menu_gallery).error(android.R.drawable.ic_menu_gallery).fit().centerCrop().into(articleHolder.coverView);
        } else {
            Picasso.get().cancelRequest(articleHolder.coverView);
            articleHolder.coverView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        articleHolder.itemView.setOnClickListener(v -> {
            if (articleClickListener != null) {
                articleClickListener.onArticleClick(article);
            }
        });

        articleHolder.reportButton.setOnClickListener(v -> {
            if (reportClickListener != null) {
                reportClickListener.onReportClick(article);
            }
        });
    }

    @Override
    public int getItemCount() {
        return articleList.size() + (showHeader ? 1 : 0);
    }

    public void setArticles(List<Article> articles) {
        articleList.clear();
        articleList.addAll(articles);
        notifyDataSetChanged();
    }

    public void setCategoryNames(Map<String, String> names) {
        categoryNames.clear();

        if (names != null) {
            categoryNames.putAll(names);
        }

        notifyItemRangeChanged(showHeader ? 1 : 0, articleList.size());
    }

    private void loadCategories(LinearLayout categoryChipContainer, Context context) {
        if (categoriesLoaded) {
            return;
        }

        categoriesLoaded = true;
        categoryChipContainer.removeAllViews();
        addCategoryChip(categoryChipContainer, context, "All", null, true);

        if (categoryLocalDataSource == null) {
            categoryLocalDataSource = new CategoryLocalDataSource(context.getApplicationContext());
        }

        categoryRepository.getActiveCategories(new ArticleCategoryRepository.OnCategoriesLoadedListener() {
            @Override
            public void onSuccess(List<ArticleCategory> categories) {
                if (categories == null || categories.isEmpty()) {
                    // Offline có thể trả danh sách rỗng: dùng cache SQLite và không ghi đè.
                    loadCachedCategories(categoryChipContainer, context);
                    return;
                }

                showCategories(categoryChipContainer, context, categories);

                try {
                    categoryLocalDataSource.saveCategories(categories);
                } catch (Exception e) {
                    android.util.Log.e("ArticleAdapter", "Failed to save categories to SQLite", e);
                }
            }

            @Override
            public void onError(Exception exception) {
                android.util.Log.e("ArticleAdapter", "Failed to load categories", exception);
                categoriesLoaded = false;
                loadCachedCategories(categoryChipContainer, context);
            }
        });
    }

    private void loadCachedCategories(LinearLayout container, Context context) {
        try {
            List<ArticleCategory> cachedCategories = categoryLocalDataSource.getCategories();
            android.util.Log.d("OfflineCache", "Cached categories count: " + cachedCategories.size());

            if (!cachedCategories.isEmpty()) {
                showCategories(container, context, cachedCategories);
            }
        } catch (Exception e) {
            android.util.Log.e("ArticleAdapter", "Failed to read categories from SQLite", e);
        }
    }

    private void showCategories(LinearLayout container, Context context, List<ArticleCategory> categories) {
        container.removeAllViews();
        addCategoryChip(container, context, "All", null, true);

        Map<String, String> names = new HashMap<>();

        for (ArticleCategory category : categories) {
            if (category.getId() != null && category.getName() != null) {
                names.put(category.getId(), category.getName());
                addCategoryChip(container, context, category.getName(), category.getId(), false);
            }
        }

        setCategoryNames(names);
    }

    private void addCategoryChip(LinearLayout container, Context context, String name, String categoryId, boolean selected) {
        TextView chip = (TextView) LayoutInflater.from(context).inflate(R.layout.item_category_chip, container, false);
        chip.setText(name);
        updateCategoryChipStyle(chip, selected);

        chip.setOnClickListener(v -> {
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof TextView) {
                    updateCategoryChipStyle((TextView) child, child == chip);
                }
            }

            if (categorySelectedListener != null) {
                categorySelectedListener.onCategorySelected(categoryId);
            }
        });

        container.addView(chip);
    }

    private void updateCategoryChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(selected ? "#2457C5" : "#F1F5F9")));
        chip.setTextColor(Color.parseColor(selected ? "#FFFFFF" : "#475569"));
    }
    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView articleCount;
        private final EditText searchInput;
        private final LinearLayout categoryChipContainer;
        private final View btnCreateArticle;

        public HeaderViewHolder(@NonNull View itemView, ArticleAdapter adapter) {
            super(itemView);
            articleCount = itemView.findViewById(R.id.tv_article_count);
            searchInput = itemView.findViewById(R.id.et_search_articles);
            categoryChipContainer = itemView.findViewById(R.id.category_chip_container);
            btnCreateArticle = itemView.findViewById(R.id.btn_create_article);
            btnCreateArticle.setOnClickListener(v -> {
                if (adapter.createArticleClickListener != null) {
                    adapter.createArticleClickListener.onCreateArticleClick();
                }
            });
            adapter.loadCategories(categoryChipContainer, itemView.getContext());

            searchInput.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchInput.getText().toString().trim();

                    if (query.isEmpty()) {
                        searchInput.setError("Please enter a search keyword");
                        return true;
                    }

                    Context context = itemView.getContext();

                    InputMethodManager keyboard = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

                    if (keyboard != null) {
                        keyboard.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                    }

                    Intent intent = new Intent(context, SearchResultsActivity.class);
                    intent.putExtra(SearchResultsActivity.EXTRA_QUERY, query);
                    context.startActivity(intent);

                    return true;
                }

                return false;
            });
        }

        public void bind(int count) {
            articleCount.setText(count + " articles");
        }
    }
}