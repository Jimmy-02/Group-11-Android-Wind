package com.example.myapplication.account;
import android.content.Intent;
import com.example.myapplication.account.profile.EditProfileActivity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import com.example.myapplication.ImageLoader;
import android.view.ViewGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.myapplication.authentication.AuthSessionManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.firestore.ListenerRegistration;
public class AccountActivity extends Fragment {

    private ListenerRegistration profileListener;
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
        // Chỉ mở Edit Profile khi người dùng nhấn
        view.findViewById(R.id.layoutEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    EditProfileActivity.class
            );

            intent.putExtra("fullName", tvFullName.getText().toString());
            intent.putExtra("email", tvEmail.getText().toString());

            startActivity(intent);
        });
        View cardAdminSpace = view.findViewById(R.id.cardAdminSpace);
        cardAdminSpace.setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    com.example.myapplication.admin.AdminActivity.class
            );
            startActivity(intent);
        });

        view.findViewById(R.id.layoutLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        AuthSessionManager.logout(
                                (AppCompatActivity) requireActivity()
                        );
                    })
                    .show();
        });
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvFullName.setText("Chưa đăng nhập");
            tvEmail.setText("");
            tvRole.setText("");
            cardAdminSpace.setVisibility(View.GONE);

            view.findViewById(R.id.layoutAccountMenu)
                    .setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.layoutAccountMenu)
                    .setVisibility(View.VISIBLE);

            String userID = user.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            profileListener = db.collection("users")
                    .document(userID)
                    .addSnapshotListener((document, error) -> {

                        if (error != null) {
                            android.util.Log.e(
                                    "AccountActivity",
                                    "Không thể theo dõi hồ sơ",
                                    error
                            );
                            return;
                        }

                        if (document == null || !document.exists()) {
                            return;
                        }

                        String fullName = document.getString("fullName");
                        String email = document.getString("email");
                        String role = document.getString("role");
                        String avatar = document.getString("avatar");

                        tvFullName.setText(
                                fullName != null ? fullName : ""
                        );

                        tvEmail.setText(
                                email != null ? email : ""
                        );

                        tvRole.setText(
                                role != null ? role : "USER"
                        );
                        boolean isAdmin = "ADMIN".equalsIgnoreCase(role != null ? role.trim() : "");
                        cardAdminSpace.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                        ImageLoader.load(avatar, imgAvatar);
                    });
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }

        super.onDestroyView();
    }
}
