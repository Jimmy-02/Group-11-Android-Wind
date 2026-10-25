package com.example.myapplication.discussions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class DiscussionAdapter extends RecyclerView.Adapter<DiscussionViewHolder> {

    private final List<Discussion> discussions = new ArrayList<>();
    private final OnDiscussionClickListener listener;

    public interface OnDiscussionClickListener {
        void onDiscussionClick(Discussion discussion);

        void onReportClick(Discussion discussion);

        void onLikeClick(Discussion discussion);

        void onCommentsClick(Discussion discussion);
    }

    public DiscussionAdapter(OnDiscussionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DiscussionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discussion, parent, false);

        return new DiscussionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull DiscussionViewHolder holder,
            int position
    ) {
        Discussion discussion = discussions.get(position);

        holder.bind(discussion);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDiscussionClick(discussion);
            }
        });

        holder.getReportView().setOnClickListener(v -> {
            if (listener != null) {
                listener.onReportClick(discussion);
            }
        });

        holder.getLikesView().setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClick(discussion);
            }
        });

        holder.getCommentsView().setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommentsClick(discussion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return discussions.size();
    }

    public void setDiscussions(List<Discussion> newDiscussions) {
        discussions.clear();

        if (newDiscussions != null) {
            discussions.addAll(newDiscussions);
        }

        notifyDataSetChanged();
    }

    public List<Discussion> getDiscussions() {
        return new ArrayList<>(discussions);
    }
}