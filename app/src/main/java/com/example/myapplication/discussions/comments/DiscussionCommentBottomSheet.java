package com.example.myapplication.discussions.comments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.discussions.Discussion;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiscussionCommentBottomSheet extends BottomSheetDialogFragment {

    public interface OnCommentsChangedListener {
        void onCommentsChanged();
    }

    private static final String ARG_ID = "discussion_id";
    private static final String ARG_TITLE = "discussion_title";
    private static final String ARG_CONTENT = "discussion_content";
    private static final String ARG_CATEGORY = "discussion_category";
    private static final String ARG_IMAGE_URL = "discussion_image_url";
    private static final String ARG_AUTHOR_NAME = "discussion_author_name";
    private static final String ARG_AUTHOR_AVATAR = "discussion_author_avatar";
    private static final String ARG_LIKE_COUNT = "discussion_like_count";

    // Chiều cao bottom sheet so với màn hình, chỉnh số này nếu muốn cao hoặc thấp hơn
    private static final float SHEET_HEIGHT_RATIO = 0.8f;
    private static final int MAX_COMMENT_LENGTH = 500;

    private ProgressBar progressComments;
    private TextView tvCommentsEmpty;
    private TextView tvCommentsTitle;
    private TextView tvCommentCount;
    private RecyclerView rvComments;
    private View layoutCommentInput;
    private EditText etComment;
    private ImageButton btnSendComment;

    private DiscussionCommentAdapter commentAdapter;
    private CommentRepository commentRepository;
    private String discussionId;

    private boolean isSubmitting = false;
    private boolean commentsChanged = false;

    // Các comment đang chờ xử lý thích/bỏ thích, tránh bấm liên tục làm lệch số đếm
    private final Set<String> likingCommentIds = new HashSet<>();

    public static DiscussionCommentBottomSheet newInstance(Discussion discussion) {
        Bundle args = new Bundle();
        args.putString(ARG_ID, discussion.getId());
        args.putString(ARG_TITLE, discussion.getTitle());
        args.putString(ARG_CONTENT, discussion.getContent());
        args.putString(ARG_CATEGORY, discussion.getCategory());
        args.putString(ARG_IMAGE_URL, discussion.getImageUrl());
        args.putString(ARG_AUTHOR_NAME, discussion.getAuthorName());
        args.putString(ARG_AUTHOR_AVATAR, discussion.getAuthorAvatar());
        args.putLong(ARG_LIKE_COUNT, discussion.getLikeCount());

        DiscussionCommentBottomSheet sheet = new DiscussionCommentBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_discussion_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressComments = view.findViewById(R.id.progress_comments);
        tvCommentsEmpty = view.findViewById(R.id.tv_comments_empty);
        tvCommentsTitle = view.findViewById(R.id.tv_sheet_comments_title);
        tvCommentCount = view.findViewById(R.id.tv_sheet_comment_count);
        rvComments = view.findViewById(R.id.rv_comments);
        layoutCommentInput = view.findViewById(R.id.layout_comment_input);
        etComment = view.findViewById(R.id.et_comment);
        btnSendComment = view.findViewById(R.id.btn_send_comment);

        Bundle args = requireArguments();
        discussionId = args.getString(ARG_ID);

        bindDiscussionHeader(view, args);

        boolean isLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        String currentUserId = isLoggedIn ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        commentAdapter = new DiscussionCommentAdapter(currentUserId);
        commentAdapter.setOnCommentActionListener(new DiscussionCommentAdapter.OnCommentActionListener() {
            @Override
            public void onLikeClick(DiscussionComment comment) {
                toggleCommentLike(comment);
            }

            @Override
            public void onReportClick(DiscussionComment comment) {
                // Làm ở bước 4
            }
        });
        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComments.setAdapter(commentAdapter);

        commentRepository = new CommentRepository();

        // Chỉ người đã đăng nhập mới thấy thanh nhập bình luận
        layoutCommentInput.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        etComment.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_COMMENT_LENGTH)});
        btnSendComment.setOnClickListener(v -> submitComment());

        loadComments();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() == null) return;

        if (getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) return;

        int height = (int) (getResources().getDisplayMetrics().heightPixels * SHEET_HEIGHT_RATIO);
        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = height;
        bottomSheet.setLayoutParams(params);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setPeekHeight(height);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        // Báo cho màn Discussion tải lại để card hiện đúng số bình luận mới
        if (commentsChanged && getParentFragment() instanceof OnCommentsChangedListener) {
            ((OnCommentsChangedListener) getParentFragment()).onCommentsChanged();
        }
    }

    private void bindDiscussionHeader(View view, Bundle args) {
        ImageView imgAuthorAvatar = view.findViewById(R.id.img_sheet_author_avatar);
        TextView tvAuthorName = view.findViewById(R.id.tv_sheet_author_name);
        TextView tvCategory = view.findViewById(R.id.tv_sheet_category);
        TextView tvTitle = view.findViewById(R.id.tv_sheet_title);
        TextView tvContent = view.findViewById(R.id.tv_sheet_content);
        ImageView imgDiscussion = view.findViewById(R.id.img_sheet_discussion);
        TextView tvLikes = view.findViewById(R.id.tv_sheet_likes);

        String authorName = args.getString(ARG_AUTHOR_NAME);
        tvAuthorName.setText(authorName != null && !authorName.trim().isEmpty() ? authorName : "User");

        String category = args.getString(ARG_CATEGORY);
        tvCategory.setText(category != null && !category.trim().isEmpty() ? category : "General");

        tvTitle.setText(args.getString(ARG_TITLE, ""));
        tvContent.setText(args.getString(ARG_CONTENT, ""));
        tvLikes.setText("♡ " + args.getLong(ARG_LIKE_COUNT, 0) + " lượt thích");

        String avatarUrl = args.getString(ARG_AUTHOR_AVATAR);

        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Picasso.get().load(avatarUrl).placeholder(R.drawable.ic_account).error(R.drawable.ic_account).fit().centerCrop().into(imgAuthorAvatar);
        } else {
            imgAuthorAvatar.setImageResource(R.drawable.ic_account);
        }

        String imageUrl = args.getString(ARG_IMAGE_URL);

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            imgDiscussion.setVisibility(View.VISIBLE);
            Picasso.get().load(imageUrl.trim()).fit().centerCrop().into(imgDiscussion);
        } else {
            imgDiscussion.setVisibility(View.GONE);
        }
    }

    private void loadComments() {
        progressComments.setVisibility(View.VISIBLE);
        tvCommentsEmpty.setVisibility(View.GONE);

        commentRepository.getComments(discussionId, new CommentRepository.OnCommentsLoadedListener() {
            @Override
            public void onSuccess(List<DiscussionComment> comments) {
                if (!isAdded()) return;
                progressComments.setVisibility(View.GONE);
                commentAdapter.setComments(comments);
                tvCommentsTitle.setText("Bình luận (" + comments.size() + ")");
                tvCommentCount.setText("💬 " + comments.size() + " bình luận");
                tvCommentsEmpty.setText("Chưa có bình luận nào. Hãy là người đầu tiên!");
                tvCommentsEmpty.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) return;
                android.util.Log.e("DiscussionComments", "Failed to load comments", exception);
                progressComments.setVisibility(View.GONE);
                tvCommentsEmpty.setText("Không thể tải bình luận. Vui lòng thử lại.");
                tvCommentsEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void toggleCommentLike(DiscussionComment comment) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để thích bình luận.", Toast.LENGTH_SHORT).show();
            return;
        }

        String commentId = comment.getId();

        if (commentId == null || !likingCommentIds.add(commentId)) return;

        commentRepository.toggleLike(commentId, currentUser.getUid(), new CommentRepository.OnCommentLikeToggledListener() {
            @Override
            public void onSuccess(boolean isLiked, long likeCount) {
                likingCommentIds.remove(commentId);
                if (!isAdded()) return;
                commentAdapter.updateLike(commentId, isLiked, likeCount);
            }

            @Override
            public void onError(Exception exception) {
                likingCommentIds.remove(commentId);
                if (!isAdded()) return;
                android.util.Log.e("DiscussionComments", "Failed to toggle comment like", exception);
                Toast.makeText(requireContext(), "Không thể thích bình luận. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitComment() {
        if (isSubmitting) return;

        String content = etComment.getText().toString().trim();

        if (content.isEmpty()) {
            etComment.setError("Vui lòng nhập nội dung bình luận.");
            etComment.requestFocus();
            return;
        }

        isSubmitting = true;
        btnSendComment.setEnabled(false);

        commentRepository.addComment(discussionId, content, new CommentRepository.OnCommentAddedListener() {
            @Override
            public void onSuccess(long commentCount) {
                if (!isAdded()) return;
                isSubmitting = false;
                btnSendComment.setEnabled(true);
                commentsChanged = true;
                etComment.setText("");
                hideKeyboard();
                Toast.makeText(requireContext(), "Đã đăng bình luận.", Toast.LENGTH_SHORT).show();
                loadComments();
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) return;
                android.util.Log.e("DiscussionComments", "Failed to add comment", exception);
                isSubmitting = false;
                btnSendComment.setEnabled(true);
                Toast.makeText(requireContext(), "Không thể đăng bình luận. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager keyboard = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
        }
    }
}