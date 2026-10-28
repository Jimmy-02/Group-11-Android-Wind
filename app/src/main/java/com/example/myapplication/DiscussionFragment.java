package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.discussions.Discussion;
import com.example.myapplication.discussions.DiscussionAdapter;
import com.example.myapplication.discussions.DiscussionRepository;
import com.example.myapplication.discussions.comments.DiscussionCommentBottomSheet;
import com.example.myapplication.discussions.creation.CreateDiscussionActivity;
import com.example.myapplication.discussions.reports.ReportDiscussionActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiscussionFragment extends Fragment implements DiscussionAdapter.OnDiscussionClickListener, DiscussionCommentBottomSheet.OnCommentsChangedListener {

    private RecyclerView rvDiscussions;
    private LinearLayout layoutDiscussionEmpty;
    private EditText etDiscussionSearch;
    private Chip chipAllDiscussions;
    private Chip chipMyDiscussions;
    private TextView tvDiscussionSection;
    private FloatingActionButton fabCreateDiscussion;

    private DiscussionAdapter discussionAdapter;
    private DiscussionRepository discussionRepository;
    private final List<Discussion> allDiscussions = new ArrayList<>();
    private boolean isMyDiscussionsSelected = false;

    public DiscussionFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discussion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvDiscussions = view.findViewById(R.id.rv_discussions);
        layoutDiscussionEmpty = view.findViewById(R.id.layout_discussion_empty);
        etDiscussionSearch = view.findViewById(R.id.et_discussion_search);
        chipAllDiscussions = view.findViewById(R.id.chip_all_discussions);
        chipMyDiscussions = view.findViewById(R.id.chip_my_discussions);
        tvDiscussionSection = view.findViewById(R.id.tv_discussion_section);
        fabCreateDiscussion = view.findViewById(R.id.fab_create_discussion);

        discussionRepository = new DiscussionRepository(requireContext());
        discussionAdapter = new DiscussionAdapter(this);

        rvDiscussions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDiscussions.setAdapter(discussionAdapter);

        fabCreateDiscussion.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateDiscussionActivity.class);
            startActivity(intent);
        });

        chipAllDiscussions.setOnClickListener(v -> {
            isMyDiscussionsSelected = false;
            tvDiscussionSection.setText("Latest Discussions");
            loadDiscussions();
        });

        chipMyDiscussions.setOnClickListener(v -> {
            isMyDiscussionsSelected = true;
            tvDiscussionSection.setText("My Discussions");
            loadDiscussions();
        });

        etDiscussionSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDiscussions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadDiscussions();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded() && discussionRepository != null) {
            loadDiscussions();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && discussionRepository != null) {
            loadDiscussions();
        }
    }

    private void loadDiscussions() {
        if (!isAdded()) return;

        if (isMyDiscussionsSelected) {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                allDiscussions.clear();
                updateDiscussionList();
                Toast.makeText(requireContext(), "Please log in to view your discussions.", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            discussionRepository.getMyDiscussions(userId, createLoadListener());
        } else {
            discussionRepository.getAllDiscussions(createLoadListener());
        }
    }

    private DiscussionRepository.OnDiscussionsLoadedListener createLoadListener() {
        return new DiscussionRepository.OnDiscussionsLoadedListener() {
            @Override
            public void onSuccess(List<Discussion> discussions) {
                if (!isAdded()) return;

                allDiscussions.clear();
                if (discussions != null) {
                    allDiscussions.addAll(discussions);
                }

                filterDiscussions(etDiscussionSearch.getText().toString());
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) return;

                android.util.Log.e("DiscussionFragment", "Failed to load discussions", exception);
                Toast.makeText(requireContext(), "Không thể tải bài thảo luận: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
    }

    private void filterDiscussions(String keyword) {
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        List<Discussion> filteredDiscussions = new ArrayList<>();

        for (Discussion discussion : allDiscussions) {
            String title = discussion.getTitle() != null ? discussion.getTitle().toLowerCase(Locale.ROOT) : "";
            String content = discussion.getContent() != null ? discussion.getContent().toLowerCase(Locale.ROOT) : "";
            String category = discussion.getCategory() != null ? discussion.getCategory().toLowerCase(Locale.ROOT) : "";

            if (title.contains(query) || content.contains(query) || category.contains(query)) {
                filteredDiscussions.add(discussion);
            }
        }

        discussionAdapter.setDiscussions(filteredDiscussions);
        updateDiscussionList();
    }

    private void updateDiscussionList() {
        boolean hasDiscussions = discussionAdapter != null && discussionAdapter.getItemCount() > 0;

        rvDiscussions.setVisibility(hasDiscussions ? View.VISIBLE : View.GONE);
        layoutDiscussionEmpty.setVisibility(hasDiscussions ? View.GONE : View.VISIBLE);

        ViewGroup.LayoutParams params = layoutDiscussionEmpty.getLayoutParams();
        if (params != null) {
            layoutDiscussionEmpty.setLayoutParams(params);
        }
    }

    @Override
    public void onReportClick(Discussion discussion) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để báo cáo bài thảo luận.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), ReportDiscussionActivity.class);
        intent.putExtra("discussionId", discussion.getId());
        intent.putExtra("title", discussion.getTitle());
        intent.putExtra("content", discussion.getContent());
        startActivity(intent);
    }

    @Override
    public void onLikeClick(Discussion discussion) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to like discussions.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        discussionRepository.toggleLike(discussion.getId(), userId, new DiscussionRepository.OnLikeToggledListener() {
            @Override
            public void onSuccess(boolean isLiked, long likeCount) {
                if (!isAdded()) return;
                loadDiscussions();
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) return;
                android.util.Log.e("DiscussionLike", "Failed to toggle Like", exception);
                Toast.makeText(requireContext(), "Like failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCommentsClick(Discussion discussion) {
        openCommentsBottomSheet(discussion);
    }

    @Override
    public void onDiscussionClick(Discussion discussion) {
        openCommentsBottomSheet(discussion);
    }

    @Override
    public void onCommentsChanged() {
        if (!isAdded()) return;
        loadDiscussions();
    }

    private void openCommentsBottomSheet(Discussion discussion) {
        if (!isAdded()) return;
        DiscussionCommentBottomSheet.newInstance(discussion).show(getChildFragmentManager(), "discussion_comments");
    }
}