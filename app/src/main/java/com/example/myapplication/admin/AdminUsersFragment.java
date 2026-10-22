package com.example.myapplication.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;

public class AdminUsersFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_placeholder, container, false);
        ImageView icon = view.findViewById(R.id.img_placeholder_icon);
        TextView title = view.findViewById(R.id.tv_placeholder_title);
        TextView desc = view.findViewById(R.id.tv_placeholder_desc);

        icon.setImageResource(R.drawable.ic_account);
        title.setText("Quản lý Người dùng");
        desc.setText("Tra cứu tài khoản người dùng, lọc trạng thái và thực hiện Khóa (Ban) hoặc Mở khóa (Unban).");
        return view;
    }
}
