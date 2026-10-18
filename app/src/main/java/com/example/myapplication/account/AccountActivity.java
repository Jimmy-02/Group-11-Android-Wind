package com.example.myapplication.account;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.R;
import com.google.firebase.firestore.FirebaseFirestore;
public class AccountActivity extends Fragment {

    public AccountActivity() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_account, container, false);

        ImageView imgAvatar = view.findViewById(R.id.imgAvatar);

        TextView tvFullName = view.findViewById(R.id.tvFullName);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvRole = view.findViewById(R.id.tvRole);

        TextView tvPendingCount = view.findViewById(R.id.tvPendingCount);
        TextView tvApprovedCount = view.findViewById(R.id.tvApprovedCount);
        TextView tvRejectedCount = view.findViewById(R.id.tvRejectedCount);
        TextView tvTotalCount = view.findViewById(R.id.tvTotalCount);

        TextView tvDiscussionCount = view.findViewById(R.id.tvDiscussionCount);
        TextView tvQuestionCount = view.findViewById(R.id.tvQuestionCount);
        TextView tvAnswerCount = view.findViewById(R.id.tvAnswerCount);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvFullName.setText("Chưa đăng nhập");
            tvEmail.setText("");
            tvRole.setText("");
/**
            (SẼ KHÔNG HIỆN THỊ CÁC CHỨC NĂNG ACCOUNT KHI CHƯA ĐĂNG NHẬP)
            view.findViewById(R.id.layoutAccountMenu)
                    .setVisibility(View.GONE);
 */
        } else {
            /**
             (SẼ HIỆN THỊ CÁC CHỨC NĂNG ACCOUNT KHI ĐĂNG NHẬP)
            view.findViewById(R.id.layoutAccountMenu)
                    .setVisibility(View.VISIBLE);
             */
            String userID = user.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userID).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String fullName = document.getString("fullName");
                            String email = document.getString("email");
                            String role = document.getString("role");

                            tvFullName.setText(fullName != null ? fullName : "");
                            tvEmail.setText(email != null ? email : "");
                            tvRole.setText(role != null ? role : "USER");
                        }
                    })
                    .addOnFailureListener(e -> {
                        //Lỗi khi lấy thông tin tài khoản)
            });

        }
        return view;
    }
}