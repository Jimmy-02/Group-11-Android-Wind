package com.example.myapplication.articles.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.articles.Article;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ArticleViewHolder> {

    private final List<Article> articles = new ArrayList<>();

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);

        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        Article article = articles.get(position);

        holder.titleView.setText(article.getTitle());
        holder.authorView.setText(article.getAuthorName());
        holder.categoryView.setText(article.getCategoryId());

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

        holder.metadataView.setText(metadata.toString());

        String imageUrl = article.getImageUrl();

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .fit()
                    .centerCrop()
                    .into(holder.coverView);
        } else {
            Picasso.get().cancelRequest(holder.coverView);
            holder.coverView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.reportButton.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public void setArticles(List<Article> newArticles) {
        articles.clear();
        articles.addAll(newArticles);
        notifyDataSetChanged();
    }

    static class ArticleViewHolder extends RecyclerView.ViewHolder {

        private final ImageView coverView;
        private final TextView titleView;
        private final TextView authorView;
        private final TextView metadataView;
        private final TextView categoryView;
        private final ImageButton reportButton;

        ArticleViewHolder(@NonNull View itemView) {
            super(itemView);

            coverView = itemView.findViewById(R.id.iv_article_cover);
            titleView = itemView.findViewById(R.id.tv_article_title);
            authorView = itemView.findViewById(R.id.tv_article_author);
            metadataView = itemView.findViewById(R.id.tv_article_metadata);
            categoryView = itemView.findViewById(R.id.tv_article_category);
            reportButton = itemView.findViewById(R.id.btn_report_article);
        }
    }
}