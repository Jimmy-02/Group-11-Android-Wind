package com.example.myapplication.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.ImageLoader;
import com.example.myapplication.R;
import com.example.myapplication.admin.models.UserReportDetail;

import java.util.ArrayList;
import java.util.List;

public class UserReportDetailAdapter extends RecyclerView.Adapter<UserReportDetailAdapter.ViewHolder> {

    private final List<UserReportDetail> items = new ArrayList<>();

    public void setItems(List<UserReportDetail> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_user_reason, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserReportDetail item = items.get(position);

        holder.tvReporterName.setText(item.getUserName() != null ? item.getUserName() : "Người dùng");
        holder.tvReportTime.setText(item.getTimeAgo() != null ? item.getTimeAgo() : "");

        String reason = item.getReason() != null ? item.getReason() : "Vi phạm quy định";
        holder.tvReportReason.setText("Lý do: " + reason);

        if (item.getDetailReason() != null && !item.getDetailReason().trim().isEmpty()) {
            holder.tvReportDetail.setText(item.getDetailReason().trim());
            holder.tvReportDetail.setVisibility(View.VISIBLE);
        } else {
            holder.tvReportDetail.setVisibility(View.GONE);
        }

        ImageLoader.load(item.getUserAvatar(), holder.imgReporterAvatar);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgReporterAvatar;
        TextView tvReporterName;
        TextView tvReportTime;
        TextView tvReportReason;
        TextView tvReportDetail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgReporterAvatar = itemView.findViewById(R.id.imgReporterAvatar);
            tvReporterName = itemView.findViewById(R.id.tvReporterName);
            tvReportTime = itemView.findViewById(R.id.tvReportTime);
            tvReportReason = itemView.findViewById(R.id.tvReportReason);
            tvReportDetail = itemView.findViewById(R.id.tvReportDetail);
        }
    }
}
