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
import com.example.myapplication.articles.articlesDetail.ArticleDetailActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyArticlesAdapter
        extends RecyclerView.Adapter<MyArticleViewHolder> {

    private final List<DocumentSnapshot> articleList;

    public MyArticlesAdapter(List<DocumentSnapshot> articleList) {
        this.articleList = new ArrayList<>(articleList);
    }

    public void setArticles(List<DocumentSnapshot> articles) {
        articleList.clear();

        if (articles != null) {
            articleList.addAll(articles);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyArticleViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_article, parent, false);

        return new MyArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MyArticleViewHolder holder,
            int position) {

        DocumentSnapshot article = articleList.get(position);

        String title = article.getString("title");

        holder.tvArticleTitle.setText(
                title == null || title.trim().isEmpty()
                        ? "Chưa có tiêu đề"
                        : title
        );

        String description = article.getString("description");

        holder.tvArticleDescription.setText(
                description == null || description.trim().isEmpty()
                        ? "Không có mô tả."
                        : description
        );

        String status = article.getString("status");

        if (status == null) {
            status = "PENDING";
        }

        switch (status.toUpperCase(Locale.ROOT)) {
            case "PENDING":
                holder.tvArticleStatus.setText("Đang chờ duyệt");
                holder.tvArticleStatus.setTextColor(
                        Color.parseColor("#946200")
                );
                break;

            case "APPROVED":
                holder.tvArticleStatus.setText("Đã duyệt");
                holder.tvArticleStatus.setTextColor(
                        Color.parseColor("#16834A")
                );
                break;

            case "REJECTED":
                holder.tvArticleStatus.setText("Bị từ chối");
                holder.tvArticleStatus.setTextColor(
                        Color.parseColor("#DC2626")
                );
                break;

            case "DELETED":
                holder.tvArticleStatus.setText("Đã xóa");
                holder.tvArticleStatus.setTextColor(
                        Color.parseColor("#667085")
                );
                break;

            default:
                holder.tvArticleStatus.setText("Chưa xác định");
                holder.tvArticleStatus.setTextColor(
                        Color.parseColor("#667085")
                );
                break;
        }

        Timestamp createdAt = article.getTimestamp("createdAt");

        if (createdAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
            );

            holder.tvArticleDate.setText(
                    "Ngày đăng: " + sdf.format(createdAt.toDate())
            );
        } else {
            holder.tvArticleDate.setText("Chưa có ngày đăng");
        }

        String imageUrl = article.getString("imageUrl");

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            holder.ivArticleImage.setVisibility(View.VISIBLE);

            Glide.with(holder.itemView.getContext())
                    .load(imageUrl.trim())
                    .centerCrop()
                    .into(holder.ivArticleImage);
        } else {
            Glide.with(holder.itemView.getContext())
                    .clear(holder.ivArticleImage);

            holder.ivArticleImage.setImageDrawable(null);
            holder.ivArticleImage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    v.getContext(),
                    ArticleDetailActivity.class
            );

            intent.putExtra(
                    "title",
                    article.getString("title")
            );

            intent.putExtra(
                    "authorName",
                    article.getString("authorName")
            );

            String categoryId = article.getString("categoryId");

            if (categoryId == null || categoryId.trim().isEmpty()) {
                categoryId = article.getString("category");
            }

            intent.putExtra("categoryId", categoryId);

            intent.putExtra(
                    "description",
                    article.getString("description")
            );

            intent.putExtra(
                    "institution",
                    article.getString("institution")
            );

            intent.putExtra(
                    "digitalObjectIdentifier",
                    article.getString("digitalObjectIdentifier")
            );

            intent.putExtra(
                    "contentUrl",
                    article.getString("contentUrl")
            );

            Timestamp articleCreatedAt =
                    article.getTimestamp("createdAt");

            intent.putExtra(
                    "createdAt",
                    articleCreatedAt != null
                            ? articleCreatedAt.toDate().getTime()
                            : -1L
            );

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return articleList.size();
    }
}
