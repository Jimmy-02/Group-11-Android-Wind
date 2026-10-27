package com.example.myapplication.account;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import com.example.myapplication.ImageLoader;
import android.view.ViewGroup;

import com.example.myapplication.account.mydiscussion.MyDiscussionsActivity;
import com.example.myapplication.account.myqna.MyQnaActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.myapplication.authentication.AuthSessionManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.auth.UserInfo;
import android.widget.Button;
import android.widget.EditText;
import com.example.myapplication.account.ChangePasswordActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.example.myapplication.account.myarticles.MyArticlesActivity;
import java.util.Locale;
public class AccountActivity extends Fragment {

    private ListenerRegistration profileListener;
    private ListenerRegistration articlesCountListener;
    private ListenerRegistration discussionsCountListener;
    private ListenerRegistration questionsCountListener;
    private ListenerRegistration answersCountListener;
    public AccountActivity() {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_account, container, false);

        //MyArticles
        View.OnClickListener openMyArticles = v -> {
            Intent intent = new Intent(
                    requireContext(),
                    MyArticlesActivity.class
            );

            startActivity(intent);
        };
        view.findViewById(R.id.cardMyArticles).setOnClickListener(openMyArticles);

        view.findViewById(R.id.layoutArticleHeader).setOnClickListener(openMyArticles);

        view.findViewById(R.id.layoutArticleStats).setOnClickListener(openMyArticles);

        view.findViewById(R.id.tvSeeAllArticles).setOnClickListener(openMyArticles);

        //MyDiscussions
        View.OnClickListener openMyDiscussions = v -> {
            Intent intent = new Intent(
                    requireContext(),
                    MyDiscussionsActivity.class
            );

            startActivity(intent);
        };
        view.findViewById(R.id.cardMyDiscussions).setOnClickListener(openMyDiscussions);

        view.findViewById(R.id.layoutDiscussionHeader).setOnClickListener(openMyDiscussions);

        view.findViewById(R.id.tvSeeAllDiscussions).setOnClickListener(openMyDiscussions);

        //MyQnA
        View.OnClickListener openMyQna = v -> {
            Intent intent = new Intent(
                    requireContext(),
                    MyQnaActivity.class
            );

            startActivity(intent);
        };

        view.findViewById(R.id.cardMyQA).setOnClickListener(openMyQna);

        view.findViewById(R.id.layoutQAHeader).setOnClickListener(openMyQna);

        view.findViewById(R.id.tvSeeAllQA).setOnClickListener(openMyQna);

        view.findViewById(R.id.layoutQACounts).setOnClickListener(openMyQna);

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

        view.findViewById(R.id.layoutChangePassword).setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    ChangePasswordActivity.class
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

            view.findViewById(R.id.layoutAccountMenu)
                    .setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.layoutAccountMenu)
                    .setVisibility(View.VISIBLE);

            String userID = user.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            loadMyContentCounts(
                    db,
                    userID,
                    tvPendingCount,
                    tvApprovedCount,
                    tvRejectedCount,
                    tvTotalCount,
                    tvDiscussionCount,
                    tvQuestionCount,
                    tvAnswerCount
            );

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
                        ImageLoader.load(avatar, imgAvatar);
                    });
        }

        return view;
    }


    private void loadMyContentCounts(
            FirebaseFirestore db,
            String userId,
            TextView tvPendingCount,
            TextView tvApprovedCount,
            TextView tvRejectedCount,
            TextView tvTotalCount,
            TextView tvDiscussionCount,
            TextView tvQuestionCount,
            TextView tvAnswerCount
    ) {

        articlesCountListener = db.collection("articles")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (getView() == null) return;

                    if (error != null) {
                        android.util.Log.e(
                                "AccountActivity",
                                "Không thể tải thống kê bài báo",
                                error
                        );
                        return;
                    }

                    if (snapshot == null) return;

                    int pending = 0;
                    int approved = 0;
                    int rejected = 0;
                    int totalActive = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshot.getDocuments()) {

                        String status = doc.getString("status");

                        if ("DELETED".equalsIgnoreCase(status)) {
                            continue;
                        }

                        if (doc.get("deletedAt") != null) {
                            continue;
                        }

                        totalActive++;

                        if (status == null) continue;

                        status = status.trim().toUpperCase(Locale.ROOT);

                        switch (status) {
                            case "PENDING":
                                pending++;
                                break;

                            case "APPROVED":
                                approved++;
                                break;

                            case "REJECTED":
                                rejected++;
                                break;
                        }
                    }

                    tvPendingCount.setText(String.valueOf(pending));
                    tvApprovedCount.setText(String.valueOf(approved));
                    tvRejectedCount.setText(String.valueOf(rejected));

                    tvTotalCount.setText(String.valueOf(totalActive));
                });


        discussionsCountListener = db.collection("discussions")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (getView() == null) return;

                    if (error != null) {
                        android.util.Log.e(
                                "AccountActivity",
                                "Không thể tải thống kê Discussion",
                                error
                        );
                        return;
                    }

                    if (snapshot == null) return;

                    int activeDiscussionCount = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshot.getDocuments()) {

                        String status = doc.getString("status");

                        if ("DELETED".equalsIgnoreCase(status)) {
                            continue;
                        }

                        if (doc.get("deletedAt") != null) {
                            continue;
                        }

                        activeDiscussionCount++;
                    }

                    tvDiscussionCount.setText(
                            activeDiscussionCount + " bài thảo luận"
                    );
                });




        questionsCountListener = db.collection("questions")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (getView() == null) return;

                    if (error != null) {
                        android.util.Log.e(
                                "AccountActivity",
                                "Không thể tải thống kê Question",
                                error
                        );
                        return;
                    }

                    if (snapshot == null) return;

                    int activeQuestionCount = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        String status = document.getString("status");

                        if ("DELETED".equalsIgnoreCase(status)) {
                            continue;
                        }

                        com.google.firebase.Timestamp deletedAt =
                                document.getTimestamp("deletedAt");

                        if (deletedAt != null) {
                            continue;
                        }

                        activeQuestionCount++;
                    }

                    tvQuestionCount.setText(
                            activeQuestionCount + " câu hỏi"
                    );
                });


        answersCountListener = db.collection("answers")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (getView() == null) return;

                    if (error != null) {
                        android.util.Log.e(
                                "AccountActivity",
                                "Không thể tải thống kê Answer",
                                error
                        );
                        return;
                    }

                    if (snapshot == null) return;

                    tvAnswerCount.setText(
                            snapshot.size() + " câu trả lời"
                    );
                });
    }


    @Override
    public void onDestroyView() {
        if (articlesCountListener != null) {
            articlesCountListener.remove();
            articlesCountListener = null;
        }

        if (discussionsCountListener != null) {
            discussionsCountListener.remove();
            discussionsCountListener = null;
        }

        if (questionsCountListener != null) {
            questionsCountListener.remove();
            questionsCountListener = null;
        }

        if (answersCountListener != null) {
            answersCountListener.remove();
            answersCountListener = null;
        }

        super.onDestroyView();
    }
}
