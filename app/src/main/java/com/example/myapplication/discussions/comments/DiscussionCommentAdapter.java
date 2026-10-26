package com.example.myapplication.discussions.comments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class DiscussionCommentAdapter extends RecyclerView.Adapter<DiscussionCommentViewHolder> {

    public interface OnCommentActionListener {
        void onLikeClick(DiscussionComment comment);

        void onReportClick(DiscussionComment comment);
    }

    private final List<DiscussionComment> comments = new ArrayList<>();
    private final String currentUserId;
    private OnCommentActionListener listener;

    public DiscussionCommentAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnCommentActionListener(OnCommentActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DiscussionCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discussion_comment, parent, false);
        return new DiscussionCommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscussionCommentViewHolder holder, int position) {
        DiscussionComment comment = comments.get(position);

        holder.bind(comment, currentUserId);

        holder.getLikesView().setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClick(comment);
            }
        });

        holder.getReportView().setOnClickListener(v -> {
            if (listener != null) {
                listener.onReportClick(comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void setComments(List<DiscussionComment> newComments) {
        comments.clear();

        if (newComments != null) {
            comments.addAll(newComments);
        }

        notifyDataSetChanged();
    }

    // Cập nhật lượt thích của đúng một comment, không phải vẽ lại cả danh sách
    public void updateLike(String commentId, boolean isLiked, long likeCount) {
        for (int i = 0; i < comments.size(); i++) {
            DiscussionComment comment = comments.get(i);

            if (comment.getId() != null && comment.getId().equals(commentId)) {
                comment.setLiked(isLiked);
                comment.setLikeCount(likeCount);
                notifyItemChanged(i);
                return;
            }
        }
    }
}