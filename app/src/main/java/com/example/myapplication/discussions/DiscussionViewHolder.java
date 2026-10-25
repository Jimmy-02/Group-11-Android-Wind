package com.example.myapplication.discussions;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.articles.articlesDetail.ArticleDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class DiscussionViewHolder extends RecyclerView.ViewHolder {

    private final TextView tvCategory;
    private final TextView tvTitle;
    private final TextView tvContent;
    private final TextView tvRelatedArticle;
    private final TextView tvReport;
    private final TextView tvLikes;
    private final TextView tvComments;
    private final ImageView ivDiscussionImage;

    public DiscussionViewHolder(@NonNull View itemView) {
        super(itemView);

        tvCategory = itemView.findViewById(R.id.tv_discussion_category);
        tvTitle = itemView.findViewById(R.id.tv_discussion_item_title);
        tvContent = itemView.findViewById(R.id.tv_discussion_item_content);
        tvRelatedArticle = itemView.findViewById(R.id.tv_discussion_related_article);
        tvReport = itemView.findViewById(R.id.tv_discussion_report);
        tvLikes = itemView.findViewById(R.id.tv_discussion_likes);
        tvComments = itemView.findViewById(R.id.tv_discussion_comments);
        ivDiscussionImage = itemView.findViewById(R.id.iv_discussion_image);
    }

    public void bind(Discussion discussion) {
        tvCategory.setText(discussion.getCategory() != null ? discussion.getCategory() : "General");
        tvTitle.setText(discussion.getTitle() != null ? discussion.getTitle() : "");
        tvContent.setText(discussion.getContent() != null ? discussion.getContent() : "");
        tvLikes.setText("♥ " + discussion.getLikeCount() + " Likes");
        tvComments.setText("\uD83D\uDCAC" + discussion.getCommentCount() + " Comments");

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        tvReport.setVisibility(currentUserId.equals(discussion.getUserId()) ? View.GONE : View.VISIBLE);

        String imageUrl = discussion.getImageUrl();

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            ivDiscussionImage.setVisibility(View.VISIBLE);
            Picasso.get().load(imageUrl).fit().centerCrop().into(ivDiscussionImage);
        } else {
            ivDiscussionImage.setVisibility(View.GONE);
            Picasso.get().cancelRequest(ivDiscussionImage);
            ivDiscussionImage.setImageDrawable(null);
        }

        loadRelatedArticle(discussion.getArticleId());
    }

    private void loadRelatedArticle(String articleId) {
        if (articleId == null || articleId.trim().isEmpty()) {
            tvRelatedArticle.setVisibility(View.GONE);
            tvRelatedArticle.setOnClickListener(null);
            return;
        }

        tvRelatedArticle.setVisibility(View.VISIBLE);
        tvRelatedArticle.setText("Loading related article...");
        tvRelatedArticle.setOnClickListener(null);

        FirebaseFirestore.getInstance()
                .collection("articles")
                .document(articleId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        tvRelatedArticle.setVisibility(View.GONE);
                        tvRelatedArticle.setOnClickListener(null);
                        return;
                    }

                    String articleTitle = document.getString("title");

                    tvRelatedArticle.setText(articleTitle != null && !articleTitle.trim().isEmpty() ? articleTitle : "Related article");

                    tvRelatedArticle.setOnClickListener(v -> openArticleDetail(document));
                })
                .addOnFailureListener(exception -> {
                    tvRelatedArticle.setText("Unable to load related article");
                    tvRelatedArticle.setOnClickListener(null);
                });
    }

    private void openArticleDetail(DocumentSnapshot document) {
        Intent intent = new Intent(itemView.getContext(), ArticleDetailActivity.class);

        intent.putExtra("title", document.getString("title"));
        intent.putExtra("authorName", document.getString("authorName"));
        intent.putExtra("categoryId", document.getString("categoryId"));
        intent.putExtra("description", document.getString("description"));
        intent.putExtra("institution", document.getString("institution"));
        intent.putExtra("digitalObjectIdentifier", document.getString("digitalObjectIdentifier"));
        intent.putExtra("contentUrl", document.getString("contentUrl"));

        Object createdAtValue = document.get("createdAt");

        if (createdAtValue instanceof com.google.firebase.Timestamp) {
            intent.putExtra("createdAt", ((com.google.firebase.Timestamp) createdAtValue).toDate().getTime());
        } else if (createdAtValue instanceof Number) {
            intent.putExtra("createdAt", ((Number) createdAtValue).longValue());
        }

        itemView.getContext().startActivity(intent);
    }

    public TextView getReportView() {
        return tvReport;
    }

    public TextView getLikesView() {
        return tvLikes;
    }

    public TextView getCommentsView() {
        return tvComments;
    }
}