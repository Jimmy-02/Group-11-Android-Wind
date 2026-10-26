package com.example.myapplication.admin.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.admin.models.AdminArticleItem;

import java.util.ArrayList;
import java.util.List;

public class AdminArticleAdapter extends RecyclerView.Adapter<AdminArticleAdapter.ArticleViewHolder> {

    public interface OnArticleActionListener {
        void onApprove(AdminArticleItem article);
        void onReject(AdminArticleItem article);
        void onDelete(AdminArticleItem article);
        void onView(AdminArticleItem article);
    }

    private final Context context;
    private List<AdminArticleItem> articleList = new ArrayList<>();
    private final OnArticleActionListener listener;

    public AdminArticleAdapter(Context context, OnArticleActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<AdminArticleItem> newList) {
        this.articleList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_article_card, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        AdminArticleItem item = articleList.get(position);

        holder.tvCategory.setText(item.getCategory());
        holder.tvTitle.setText(item.getTitle());
        holder.tvAuthorInstitution.setText("Tác giả: " + item.getAuthorName() + " • " + item.getInstitution());
        holder.tvDescription.setText(item.getDescription() != null && !item.getDescription().isEmpty()
                ? item.getDescription() : "Chưa có tóm tắt nội dung bài báo.");
        holder.tvTime.setText(item.getTimeAgo());

        if (item.isPending()) {
            holder.tvStatus.setText("Chờ duyệt");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#DC2626"));

            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.GONE);
        } else if (item.isApproved()) {
            holder.tvStatus.setText("Đã duyệt");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
            holder.tvStatus.setTextColor(Color.parseColor("#15803D"));

            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else if (item.isRejected()) {
            holder.tvStatus.setText("Từ chối");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
            holder.tvStatus.setTextColor(Color.parseColor("#B91C1C"));

            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setText("Đã xóa");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_dismissed);
            holder.tvStatus.setTextColor(Color.parseColor("#64748B"));

            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(item);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });

        holder.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onView(item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onView(item);
        });
    }

    @Override
    public int getItemCount() {
        return articleList.size();
    }

    static class ArticleViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        TextView tvStatus;
        TextView tvTitle;
        TextView tvAuthorInstitution;
        TextView tvDescription;
        TextView tvTime;
        TextView btnView;
        TextView btnReject;
        TextView btnApprove;
        TextView btnDelete;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_article_category);
            tvStatus = itemView.findViewById(R.id.tv_article_status);
            tvTitle = itemView.findViewById(R.id.tv_article_title);
            tvAuthorInstitution = itemView.findViewById(R.id.tv_article_author_institution);
            tvDescription = itemView.findViewById(R.id.tv_article_description);
            tvTime = itemView.findViewById(R.id.tv_article_time);
            btnView = itemView.findViewById(R.id.btn_article_view);
            btnReject = itemView.findViewById(R.id.btn_article_reject);
            btnApprove = itemView.findViewById(R.id.btn_article_approve);
            btnDelete = itemView.findViewById(R.id.btn_article_delete);
        }
    }
}