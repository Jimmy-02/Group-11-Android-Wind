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

public class AdminArticlesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_placeholder, container, false);
        ImageView icon = view.findViewById(R.id.img_placeholder_icon);
        TextView title = view.findViewById(R.id.tv_placeholder_title);
        TextView desc = view.findViewById(R.id.tv_placeholder_desc);

        icon.setImageResource(R.drawable.ic_my_article);
        title.setText("Quản lý Bài báo");
        desc.setText("Kiểm duyệt các bài báo khoa học mới gửi lên và xử lý các bài báo bị người dùng báo cáo.");
        return view;
    }
}
