
package com.example.myapplication.account.mydiscussion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.discussions.Discussion;
import com.example.myapplication.discussions.DiscussionAdapter;
import com.example.myapplication.discussions.DiscussionRepository;
import com.example.myapplication.discussions.comments.DiscussionCommentBottomSheet;
import com.example.myapplication.discussions.reports.ReportDiscussionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MyDiscussionsActivity extends AppCompatActivity
        implements DiscussionAdapter.OnDiscussionClickListener,
        DiscussionCommentBottomSheet.OnCommentsChangedListener {

    private static final String TAG = "MyDiscussionsActivity";

    private ProgressBar progressBar;
    private TextView tvSummary;
    private TextView tvEmpty;
    private RecyclerView rvMyDiscussions;

    private DiscussionAdapter adapter;
    private DiscussionRepository repository;

    private final List<Discussion> myDiscussions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_discussions);

        View backLayout = findViewById(R.id.myDiscussionsBackLayout);

        progressBar = findViewById(R.id.pbMyDiscussions);
        tvSummary = findViewById(R.id.tvMyDiscussionsSummary);
        tvEmpty = findViewById(R.id.tvMyDiscussionsEmpty);
        rvMyDiscussions = findViewById(R.id.rvMyDiscussions);

        backLayout.setOnClickListener(v -> finish());

        repository = new DiscussionRepository();
        adapter = new DiscussionAdapter(this);

        rvMyDiscussions.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvMyDiscussions.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyDiscussions();
    }

    private void loadMyDiscussions() {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập để xem thảo luận của bạn.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        repository.getMyDiscussions(
                user.getUid(),
                new DiscussionRepository.OnDiscussionsLoadedListener() {

                    @Override
                    public void onSuccess(List<Discussion> discussions) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        progressBar.setVisibility(View.GONE);

                        myDiscussions.clear();

                        if (discussions != null) {
                            myDiscussions.addAll(discussions);
                        }

                        adapter.setDiscussions(myDiscussions);

                        tvSummary.setText(
                                "Bạn có " + myDiscussions.size()
                                        + " bài thảo luận"
                        );

                        if (myDiscussions.isEmpty()) {
                            tvEmpty.setText(
                                    "Bạn chưa có bài thảo luận nào."
                            );

                            tvEmpty.setVisibility(View.VISIBLE);
                            rvMyDiscussions.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            rvMyDiscussions.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        progressBar.setVisibility(View.GONE);

                        Log.e(TAG, "Không thể tải thảo luận", exception);

                        tvEmpty.setText(
                                "Không thể tải danh sách thảo luận. "
                                        + "Vui lòng thử lại."
                        );

                        tvEmpty.setVisibility(View.VISIBLE);
                        rvMyDiscussions.setVisibility(View.GONE);

                        Toast.makeText(
                                MyDiscussionsActivity.this,
                                "Tải thảo luận thất bại.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    @Override
    public void onDiscussionClick(Discussion discussion) {
        openComments(discussion);
    }

    @Override
    public void onCommentsClick(Discussion discussion) {
        openComments(discussion);
    }

    private void openComments(Discussion discussion) {
        DiscussionCommentBottomSheet.newInstance(discussion)
                .show(
                        getSupportFragmentManager(),
                        "my_discussion_comments"
                );
    }

    @Override
    public void onReportClick(Discussion discussion) {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập để báo cáo.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent = new Intent(
                this,
                ReportDiscussionActivity.class
        );

        intent.putExtra("discussionId", discussion.getId());
        intent.putExtra("title", discussion.getTitle());
        intent.putExtra("content", discussion.getContent());

        startActivity(intent);
    }

    @Override
    public void onLikeClick(Discussion discussion) {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập để thích thảo luận.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        repository.toggleLike(
                discussion.getId(),
                user.getUid(),
                new DiscussionRepository.OnLikeToggledListener() {

                    @Override
                    public void onSuccess(
                            boolean isLiked,
                            long likeCount
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        loadMyDiscussions();
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Log.e(
                                TAG,
                                "Không thể cập nhật lượt thích",
                                exception
                        );

                        Toast.makeText(
                                MyDiscussionsActivity.this,
                                "Không thể cập nhật lượt thích.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    @Override
    public void onCommentsChanged() {
        loadMyDiscussions();
    }
}
