package com.example.myapplication.account.mydiscussion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.DiscussionLocalDataSource;
import com.example.myapplication.discussions.Discussion;
import com.example.myapplication.discussions.DiscussionAdapter;
import com.example.myapplication.discussions.DiscussionRepository;
import com.example.myapplication.discussions.comments.DiscussionCommentBottomSheet;
import com.example.myapplication.discussions.reports.ReportDiscussionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MyDiscussionsActivity extends AppCompatActivity implements DiscussionAdapter.OnDiscussionClickListener, DiscussionCommentBottomSheet.OnCommentsChangedListener {

    private static final String TAG = "MyDiscussionsActivity";

    private ProgressBar progressBar;
    private TextView tvSummary;
    private TextView tvEmpty;
    private RecyclerView rvMyDiscussions;

    private DiscussionAdapter adapter;
    private DiscussionRepository repository;
    private DiscussionLocalDataSource localDataSource;

    private final List<Discussion> myDiscussions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_my_discussions);

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        insetsController.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.root);
        View statusBarSpacer = findViewById(R.id.status_bar_spacer);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            ViewGroup.LayoutParams params = statusBarSpacer.getLayoutParams();

            params.height = bars.top;
            statusBarSpacer.setLayoutParams(params);

            v.setPadding(bars.left, 0, bars.right, bars.bottom);

            return WindowInsetsCompat.CONSUMED;
        });

        View backLayout = findViewById(R.id.myDiscussionsBackLayout);
        progressBar = findViewById(R.id.pbMyDiscussions);
        tvSummary = findViewById(R.id.tvMyDiscussionsSummary);
        tvEmpty = findViewById(R.id.tvMyDiscussionsEmpty);
        rvMyDiscussions = findViewById(R.id.rvMyDiscussions);

        backLayout.setOnClickListener(v -> finish());

        repository = new DiscussionRepository(this);
        localDataSource = new DiscussionLocalDataSource(this);
        adapter = new DiscussionAdapter(this);

        rvMyDiscussions.setLayoutManager(new LinearLayoutManager(this));

        rvMyDiscussions.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyDiscussions();
    }

    private void loadMyDiscussions() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem thảo luận của bạn.", Toast.LENGTH_SHORT).show();

            finish();
            return;
        }

        String userId = user.getUid();

        tvEmpty.setVisibility(View.GONE);

        // Hiện ngay dữ liệu đã lưu trong SQLite nếu màn hình chưa có gì, Firestore sẽ cập nhật sau.
        if (myDiscussions.isEmpty()) {
            List<Discussion> cachedDiscussions = readCachedDiscussions(userId);

            Log.d("OfflineCache", "My discussions cached: " + cachedDiscussions.size());

            if (cachedDiscussions.isEmpty()) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                myDiscussions.addAll(cachedDiscussions);
                progressBar.setVisibility(View.GONE);
                showDiscussions();
            }
        }

        repository.getMyDiscussions(userId, new DiscussionRepository.OnDiscussionsLoadedListener() {

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

                Log.d("OfflineCache", "My discussions Firestore: " + myDiscussions.size());

                try {
                    localDataSource.saveDiscussions(userId, myDiscussions);
                    Log.d("OfflineCache", "My discussions saved to SQLite: " + myDiscussions.size());
                } catch (Exception e) {
                    Log.e(TAG, "Không thể lưu cache SQLite", e);
                }

                showDiscussions();
            }

            @Override
            public void onError(Exception exception) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                progressBar.setVisibility(View.GONE);

                Log.e(TAG, "Không thể tải thảo luận", exception);

                if (!myDiscussions.isEmpty()) {
                    Toast.makeText(MyDiscussionsActivity.this, "Đang hiển thị dữ liệu đã lưu offline.", Toast.LENGTH_SHORT).show();
                    return;
                }

                tvSummary.setText("Bạn có 0 bài thảo luận");
                tvEmpty.setText("Không có dữ liệu offline. Hãy kết nối mạng và mở lại trang này.");
                tvEmpty.setVisibility(View.VISIBLE);
                rvMyDiscussions.setVisibility(View.GONE);

                Toast.makeText(MyDiscussionsActivity.this, "Tải thảo luận thất bại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<Discussion> readCachedDiscussions(String userId) {
        try {
            return localDataSource.getDiscussions(userId);
        } catch (Exception e) {
            Log.e(TAG, "Không thể đọc cache SQLite", e);
            return new ArrayList<>();
        }
    }

    private void showDiscussions() {
        adapter.setDiscussions(myDiscussions);

        tvSummary.setText("Bạn có " + myDiscussions.size() + " bài thảo luận");

        if (myDiscussions.isEmpty()) {
            tvEmpty.setText("Bạn chưa có bài thảo luận nào.");

            tvEmpty.setVisibility(View.VISIBLE);
            rvMyDiscussions.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvMyDiscussions.setVisibility(View.VISIBLE);
        }
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
        DiscussionCommentBottomSheet.newInstance(discussion).show(getSupportFragmentManager(), "my_discussion_comments");
    }

    @Override
    public void onReportClick(Discussion discussion) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để báo cáo.", Toast.LENGTH_SHORT).show();

            return;
        }

        Intent intent = new Intent(this, ReportDiscussionActivity.class);

        intent.putExtra("discussionId", discussion.getId());
        intent.putExtra("title", discussion.getTitle());
        intent.putExtra("content", discussion.getContent());

        startActivity(intent);
    }

    @Override
    public void onLikeClick(Discussion discussion) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thích thảo luận.", Toast.LENGTH_SHORT).show();

            return;
        }

        repository.toggleLike(discussion.getId(), user.getUid(), new DiscussionRepository.OnLikeToggledListener() {

            @Override
            public void onSuccess(boolean isLiked, long likeCount) {
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

                Log.e(TAG, "Không thể cập nhật lượt thích", exception);

                Toast.makeText(MyDiscussionsActivity.this, "Không thể cập nhật lượt thích.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCommentsChanged() {
        loadMyDiscussions();
    }

    @Override
    protected void onDestroy() {
        if (localDataSource != null) {
            localDataSource.close();
        }

        super.onDestroy();
    }
}