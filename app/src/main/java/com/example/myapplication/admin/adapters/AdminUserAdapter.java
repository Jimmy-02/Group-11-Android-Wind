package com.example.myapplication.admin.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.admin.models.AdminUserItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onBanUser(AdminUserItem user);
        void onUnbanUser(AdminUserItem user);
        void onUserClick(AdminUserItem user);
    }

    private final Context context;
    private List<AdminUserItem> userList = new ArrayList<>();
    private final OnUserActionListener listener;

    public AdminUserAdapter(Context context, OnUserActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<AdminUserItem> newList) {
        this.userList = (newList != null) ? new ArrayList<>(newList) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        AdminUserItem user = userList.get(position);

        holder.tvName.setText(user.getFullName());
        holder.tvEmail.setText(user.getEmail());

        if (user.isBanned()) {
            holder.tvStatus.setText("BANNED");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_user_status_banned);
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444"));
        } else {
            holder.tvStatus.setText("ACTIVE");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_user_status_active);
            holder.tvStatus.setTextColor(Color.parseColor("#22C55E"));
        }

        if (user.getAvatar() != null && !user.getAvatar().trim().isEmpty()) {
            Picasso.get()
                    .load(user.getAvatar())
                    .placeholder(R.drawable.ic_account)
                    .error(R.drawable.ic_account)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_account);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });

        holder.btnMore.setOnClickListener(v -> showPopupMenu(v, user));
    }

    private void showPopupMenu(View anchor, AdminUserItem user) {
        PopupMenu popup = new PopupMenu(context, anchor);
        if (user.isBanned()) {
            popup.getMenu().add(0, 1, 0, "Mở khóa tài khoản (Unban)");
        } else {
            popup.getMenu().add(0, 2, 0, "Khóa tài khoản (Ban User)");
        }

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                if (listener != null) listener.onUnbanUser(user);
                return true;
            } else if (item.getItemId() == 2) {
                if (listener != null) listener.onBanUser(user);
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName;
        TextView tvEmail;
        TextView tvStatus;
        ImageButton btnMore;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_user_avatar);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvStatus = itemView.findViewById(R.id.tv_user_status);
            btnMore = itemView.findViewById(R.id.btn_user_action_more);
        }
    }
}