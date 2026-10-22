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

public class AdminQnaFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_placeholder, container, false);
        ImageView icon = view.findViewById(R.id.img_placeholder_icon);
        TextView title = view.findViewById(R.id.tv_placeholder_title);
        TextView desc = view.findViewById(R.id.tv_placeholder_desc);

        icon.setImageResource(R.drawable.ic_qna);
        title.setText("Quản lý Hỏi & Đáp (Q&A)");
        desc.setText("Kiểm duyệt và xử lý các câu hỏi hoặc câu trả lời bị người dùng phản ánh vi phạm.");
        return view;
    }
}
