package com.example.myapplication.discussions.comments;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.squareup.picasso.Picasso;

public class DiscussionCommentViewHolder extends RecyclerView.ViewHolder {

    private final ImageView imgAvatar;
    private final TextView tvAuthor;
    private final TextView tvContent;
    private final TextView tvLikes;
    private final TextView tvReport;

    public DiscussionCommentViewHolder(@NonNull View itemView) {
        super(itemView);
        imgAvatar = itemView.findViewById(R.id.img_comment_avatar);
        tvAuthor = itemView.findViewById(R.id.tv_comment_author);
        tvContent = itemView.findViewById(R.id.tv_comment_content);
        tvLikes = itemView.findViewById(R.id.tv_comment_likes);
        tvReport = itemView.findViewById(R.id.tv_comment_report);
    }

    public void bind(DiscussionComment comment, String currentUserId) {
        String authorName = comment.getAuthorName();
        tvAuthor.setText(authorName != null && !authorName.trim().isEmpty() ? authorName : "User");
        tvContent.setText(comment.getContent() != null ? comment.getContent() : "");

        tvLikes.setText((comment.isLiked() ? "♥ " : "♡ ") + comment.getLikeCount());
        tvLikes.setTextColor(Color.parseColor(comment.isLiked() ? "#D92D20" : "#667085"));

        boolean isOwnComment = currentUserId != null && currentUserId.equals(comment.getUserId());
        tvReport.setVisibility(isOwnComment ? View.GONE : View.VISIBLE);

        String avatarUrl = comment.getAuthorAvatar();

        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Picasso.get().load(avatarUrl).placeholder(R.drawable.ic_account).error(R.drawable.ic_account).fit().centerCrop().into(imgAvatar);
        } else {
            Picasso.get().cancelRequest(imgAvatar);
            imgAvatar.setImageResource(R.drawable.ic_account);
        }
    }

    public TextView getLikesView() {
        return tvLikes;
    }

    public TextView getReportView() {
        return tvReport;
    }
}