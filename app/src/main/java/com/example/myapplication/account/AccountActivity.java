package com.example.myapplication.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.myapplication.ImageLoader;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.account.myarticles.MyArticlesActivity;
import com.example.myapplication.account.mydiscussion.MyDiscussionsActivity;
import com.example.myapplication.account.myqna.MyQnaActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class AccountActivity extends Fragment {

    private ListenerRegistration profileListener;
    private ListenerRegistration articlesCountListener;
    private ListenerRegistration discussionsCountListener;
    private ListenerRegistration questionsCountListener;
    private ListenerRegistration answersCountListener;

    public AccountActivity() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_account, container, false);

        View.OnClickListener openMyArticles = v -> {
            Intent intent = new Intent(requireContext(), MyArticlesActivity.class);
            startActivity(intent);
        };

        view.findViewById(R.id.cardMyArticles).setOnClickListener(openMyArticles);
        view.findViewById(R.id.layoutArticleHeader).setOnClickListener(openMyArticles);
        view.findViewById(R.id.layoutArticleStats).setOnClickListener(openMyArticles);
        view.findViewById(R.id.tvSeeAllArticles).setOnClickListener(openMyArticles);

        View.OnClickListener openMyDiscussions = v -> {
            Intent intent = new Intent(requireContext(), MyDiscussionsActivity.class);
            startActivity(intent);
        };

        view.findViewById(R.id.cardMyDiscussions).setOnClickListener(openMyDiscussions);
        view.findViewById(R.id.layoutDiscussionHeader).setOnClickListener(openMyDiscussions);
        view.findViewById(R.id.tvSeeAllDiscussions).setOnClickListener(openMyDiscussions);

        View.OnClickListener openMyQna = v -> {
            Intent intent = new Intent(requireContext(), MyQnaActivity.class);
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

        view.findViewById(R.id.layoutEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            intent.putExtra("fullName", tvFullName.getText().toString());
            intent.putExtra("email", tvEmail.getText().toString());
            startActivity(intent);
        });

        view.findViewById(R.id.layoutChangePassword).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.layoutLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        removeFirestoreListeners();
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity()).logoutAndNavigateToLogin();
                        }
                    })
                    .show();
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvFullName.setText("Chưa đăng nhập");
            tvEmail.setText("");
            tvRole.setText("");
            view.findViewById(R.id.layoutAccountMenu).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.layoutAccountMenu).setVisibility(View.VISIBLE);

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
                        if (getView() == null) return;

                        if (error != null) {
                            android.util.Log.e("AccountActivity", "Không thể theo dõi hồ sơ", error);
                            return;
                        }

                        if (document == null || !document.exists()) return;

                        String fullName = document.getString("fullName");
                        String email = document.getString("email");
                        String role = document.getString("role");
                        String avatar = document.getString("avatar");

                        tvFullName.setText(fullName != null ? fullName : "");
                        tvEmail.setText(email != null ? email : "");
                        tvRole.setText(role != null ? role : "USER");

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
                        android.util.Log.e("AccountActivity", "Không thể tải thống kê bài báo", error);
                        return;
                    }

                    if (snapshot == null) return;

                    int pending = 0;
                    int approved = 0;
                    int rejected = 0;
                    int totalActive = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");

                        if ("DELETED".equalsIgnoreCase(status) || doc.get("deletedAt") != null) {
                            continue;
                        }

                        totalActive++;

                        if (status == null) continue;

                        switch (status.trim().toUpperCase(Locale.ROOT)) {
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
                        android.util.Log.e("AccountActivity", "Không thể tải thống kê Discussion", error);
                        return;
                    }

                    if (snapshot == null) return;

                    int activeDiscussionCount = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");

                        if ("DELETED".equalsIgnoreCase(status) || doc.get("deletedAt") != null) {
                            continue;
                        }

                        activeDiscussionCount++;
                    }

                    tvDiscussionCount.setText(activeDiscussionCount + " bài thảo luận");
                });

        questionsCountListener = db.collection("questions")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (getView() == null) return;

                    if (error != null) {
                        android.util.Log.e("AccountActivity", "Không thể tải thống kê Question", error);
                        return;
                    }

                    if (snapshot == null) return;

                    int activeQuestionCount = 0;

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String status = document.getString("status");
                        Timestamp deletedAt = document.getTimestamp("deletedAt");

                        if ("DELETED".equalsIgnoreCase(status) || deletedAt != null) {
                            continue;
                        }

                        activeQuestionCount++;
                    }

                    tvQuestionCount.setText(activeQuestionCount + " câu hỏi");
                });

        answersCountListener = db.collection("answers")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (getView() == null) return;

                    if (error != null) {
                        android.util.Log.e("AccountActivity", "Không thể tải thống kê Answer", error);
                        return;
                    }

                    if (snapshot == null) return;

                    int activeAnswerCount = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        String status = document.getString("status");

                        if ("DELETE".equalsIgnoreCase(status)
                                || "DELETED".equalsIgnoreCase(status)
                                || document.get("deletedAt") != null) {
                            continue;
                        }

                        activeAnswerCount++;
                    }

                    tvAnswerCount.setText(
                            activeAnswerCount + " câu trả lời"
                    );
                });
    }

    private void removeFirestoreListeners() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }

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
    }

    public void removeFirestoreListenersForLogout() {
        removeFirestoreListeners();
    }

    @Override
    public void onDestroyView() {
        removeFirestoreListeners();
        super.onDestroyView();
    }
}