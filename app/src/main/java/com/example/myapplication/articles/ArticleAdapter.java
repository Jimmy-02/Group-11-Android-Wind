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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import com.example.myapplication.articles.search.SearchResultsActivity;

public class ArticleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ARTICLE = 1;

    private final List<Article> articleList = new ArrayList<>();

    public interface OnArticleClickListener {
        void onArticleClick(Article article);
    }

    public interface OnReportClickListener {
        void onReportClick(Article article);
    }

    private final OnArticleClickListener articleClickListener;
    private final OnReportClickListener reportClickListener;

    public ArticleAdapter(OnArticleClickListener articleClickListener, OnReportClickListener reportClickListener) {
        this.articleClickListener = articleClickListener;
        this.reportClickListener = reportClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_ARTICLE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_header, parent, false);
            return new HeaderViewHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(articleList.size());
            return;
        }

        ArticleViewHolder articleHolder = (ArticleViewHolder) holder;
        Article article = articleList.get(position - 1);

        articleHolder.titleView.setText(article.getTitle());
        articleHolder.authorView.setText(article.getAuthorName());
        articleHolder.categoryView.setText(article.getCategory());

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
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .fit()
                    .centerCrop()
                    .into(articleHolder.coverView);
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
        return articleList.size() + 1;
    }

    public void setArticles(List<Article> articles) {
        articleList.clear();
        articleList.addAll(articles);
        notifyDataSetChanged();
    }

    public void addArticles(List<Article> articles) {
        int startPosition = articleList.size() + 1;
        articleList.addAll(articles);
        notifyItemRangeInserted(startPosition, articles.size());
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView articleCount;
        private final EditText searchInput;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            articleCount = itemView.findViewById(R.id.tv_article_count);
            searchInput = itemView.findViewById(R.id.et_search_articles);

            searchInput.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchInput.getText().toString().trim();

                    if (query.isEmpty()) {
                        searchInput.setError("Please enter a search keyword");
                        return true;
                    }

                    Context context = itemView.getContext();

                    InputMethodManager keyboard =
                            (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

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