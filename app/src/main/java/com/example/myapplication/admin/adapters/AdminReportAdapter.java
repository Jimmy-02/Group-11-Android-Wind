package com.example.myapplication.admin.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.ImageLoader;
import com.example.myapplication.R;
import com.example.myapplication.admin.models.AdminReportItem;

import java.util.ArrayList;
import java.util.List;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ReportViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(AdminReportItem item);
    }

    private List<AdminReportItem> items = new ArrayList<>();
    private OnReportClickListener listener;

    public AdminReportAdapter(OnReportClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AdminReportItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_report_card, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        AdminReportItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {

        private TextView tvStatus, tvReportCount, tvTime, tvTitle, tvSubtitle;
        private LinearLayout layoutTags;
        private TextView tvTag1, tvTag2;
        private ImageView ivAvatar;
        private TextView tvAuthor;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tv_report_status);
            tvReportCount = itemView.findViewById(R.id.tv_report_count);
            tvTime = itemView.findViewById(R.id.tv_report_time);
            tvTitle = itemView.findViewById(R.id.tv_report_title);
            tvSubtitle = itemView.findViewById(R.id.tv_report_subtitle);
            layoutTags = itemView.findViewById(R.id.layout_report_tags);
            tvTag1 = itemView.findViewById(R.id.tv_tag_1);
            tvTag2 = itemView.findViewById(R.id.tv_tag_2);
            ivAvatar = itemView.findViewById(R.id.iv_author_avatar);
            tvAuthor = itemView.findViewById(R.id.tv_author_name);
        }

        public void bind(AdminReportItem item, OnReportClickListener listener) {
            // 1. Status badge
            String status = item.getStatus() != null ? item.getStatus() : "PENDING";
            if ("REPORTED_CONTENT_DELETED".equalsIgnoreCase(status)) {
                tvStatus.setText("Đã xử lý");
                tvStatus.setBackgroundResource(R.drawable.bg_status_resolved);
                tvStatus.setTextColor(Color.parseColor("#D97706"));
            } else if ("DISMISSED".equalsIgnoreCase(status)) {
                tvStatus.setText("Đã bác bỏ");
                tvStatus.setBackgroundResource(R.drawable.bg_status_dismissed);
                tvStatus.setTextColor(Color.parseColor("#475569"));
            } else {
                tvStatus.setText("Chờ xử lý");
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatus.setTextColor(Color.parseColor("#DC2626"));
            }

            // 2. Report count & Time
            tvReportCount.setText(item.getReportCount() + " báo cáo");
            tvTime.setText(item.getTimeAgo() != null ? item.getTimeAgo() : "");

            // 3. Title or Content
            tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");

            // 4. Tags or Parent question/discussion subtitle
            boolean isChildItem = "ANSWER".equalsIgnoreCase(item.getContentType()) || "COMMENT".equalsIgnoreCase(item.getContentType());
            if (isChildItem) {
                layoutTags.setVisibility(View.GONE);
                tvSubtitle.setVisibility(View.VISIBLE);
                String parentTitle = item.getParentQuestionTitle() != null ? item.getParentQuestionTitle() : "Bài liên quan";
                String prefix = "COMMENT".equalsIgnoreCase(item.getContentType()) ? "Trong thảo luận: " : "Trong câu hỏi: ";
                tvSubtitle.setText(prefix + parentTitle);
            } else {
                tvSubtitle.setVisibility(View.GONE);
                List<String> tags = item.getTags();
                if (tags != null && !tags.isEmpty()) {
                    layoutTags.setVisibility(View.VISIBLE);
                    tvTag1.setText(tags.get(0));
                    tvTag1.setVisibility(View.VISIBLE);
                    if (tags.size() > 1) {
                        tvTag2.setText(tags.get(1));
                        tvTag2.setVisibility(View.VISIBLE);
                    } else {
                        tvTag2.setVisibility(View.GONE);
                    }
                } else {
                    layoutTags.setVisibility(View.GONE);
                }
            }

            // 5. Author info
            tvAuthor.setText(item.getAuthorName() != null ? item.getAuthorName() : "Người dùng");
            ImageLoader.load(item.getAuthorAvatar(), ivAvatar);

            // 6. Click action
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(item);
                }
            });
        }
    }
}
