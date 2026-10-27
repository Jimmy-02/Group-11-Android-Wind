package com.example.myapplication.account.myarticles;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.articles.Article;
import com.example.myapplication.articles.articlesDetail.ArticleDetailActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyArticlesAdapter extends RecyclerView.Adapter<MyArticleViewHolder> {

    private final List<Article> articleList;

    public MyArticlesAdapter(List<Article> articleList) {
        this.articleList = new ArrayList<>(articleList);
    }

    public void setArticles(List<Article> articles) {
        articleList.clear();

        if (articles != null) {
            articleList.addAll(articles);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_article, parent, false);
        return new MyArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyArticleViewHolder holder, int position) {
        Article article = articleList.get(position);

        String title = article.getTitle();
        holder.tvArticleTitle.setText(title == null || title.trim().isEmpty() ? "Chưa có tiêu đề" : title);

        String description = article.getDescription();
        holder.tvArticleDescription.setText(description == null || description.trim().isEmpty() ? "Không có mô tả." : description);

        String status = article.getStatus();

        if (status == null) {
            status = "PENDING";
        }

        switch (status.toUpperCase(Locale.ROOT)) {
            case "PENDING":
                holder.tvArticleStatus.setText("Đang chờ duyệt");
                holder.tvArticleStatus.setTextColor(Color.parseColor("#946200"));
                break;
            case "APPROVED":
                holder.tvArticleStatus.setText("Đã duyệt");
                holder.tvArticleStatus.setTextColor(Color.parseColor("#16834A"));
                break;
            case "REJECTED":
                holder.tvArticleStatus.setText("Bị từ chối");
                holder.tvArticleStatus.setTextColor(Color.parseColor("#DC2626"));
                break;
            case "DELETED":
                holder.tvArticleStatus.setText("Đã xóa");
                holder.tvArticleStatus.setTextColor(Color.parseColor("#667085"));
                break;
            default:
                holder.tvArticleStatus.setText("Chưa xác định");
                holder.tvArticleStatus.setTextColor(Color.parseColor("#667085"));
                break;
        }

        if (article.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvArticleDate.setText("Ngày đăng: " + sdf.format(new Date(article.getCreatedAt())));
        } else {
            holder.tvArticleDate.setText("Chưa có ngày đăng");
        }

        String imageUrl = article.getImageUrl();

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            holder.ivArticleImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(imageUrl.trim()).centerCrop().into(holder.ivArticleImage);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.ivArticleImage);
            holder.ivArticleImage.setImageDrawable(null);
            holder.ivArticleImage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ArticleDetailActivity.class);
            intent.putExtra("title", article.getTitle());
            intent.putExtra("authorName", article.getAuthorName());
            intent.putExtra("categoryId", article.getCategoryId());
            intent.putExtra("description", article.getDescription());
            intent.putExtra("institution", article.getInstitution());
            intent.putExtra("digitalObjectIdentifier", article.getDigitalObjectIdentifier());
            intent.putExtra("sourceUrl", article.getSourceUrl());
            intent.putExtra("contentUrl", article.getContentUrl());
            intent.putExtra("createdAt", article.getCreatedAt() > 0 ? article.getCreatedAt() : -1L);
            intent.putExtra("articleId", article.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return articleList.size();
    }
}