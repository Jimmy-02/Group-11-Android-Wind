package com.example.myapplication.discussions.comments;

import com.example.myapplication.discussions.CommentConstant;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Context;
import com.example.myapplication.data.local.CommentLocalDataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentRepository {

    private final FirebaseFirestore firestore;

    private final CommentLocalDataSource localDataSource;

    public CommentRepository(Context context) {
        firestore = FirebaseFirestore.getInstance();
        localDataSource = new CommentLocalDataSource(context);
    }

    public interface OnCommentsLoadedListener {
        void onSuccess(List<DiscussionComment> comments);
        void onError(Exception exception);
    }

    public interface OnCommentAddedListener {
        void onSuccess(long commentCount);
        void onError(Exception exception);
    }

    public interface OnCommentLikeToggledListener {
        void onSuccess(boolean isLiked, long likeCount);
        void onError(Exception exception);
    }

    public void getComments(String discussionId, OnCommentsLoadedListener listener) {
        if (discussionId == null || discussionId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("Discussion ID is invalid."));
            return;
        }

        firestore.collection("comments")
                .whereEqualTo("discussionId", discussionId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DiscussionComment> comments = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        DiscussionComment comment = mapComment(document);
                        if (comment != null) comments.add(comment);
                    }

                    Collections.sort(comments, (first, second) ->
                            Long.compare(second.getCreatedAt(), first.getCreatedAt()));

                    if (snapshot.getMetadata().isFromCache()) {
                        List<DiscussionComment> cachedComments = localDataSource.getComments(discussionId);

                        if (!cachedComments.isEmpty()) {
                            attachLikeStatus(cachedComments, listener);
                        } else if (!comments.isEmpty()) {
                            attachLikeStatus(comments, listener);
                        } else {
                            listener.onError(new Exception("Không có comment được lưu offline."));
                        }

                        return;
                    }

                    localDataSource.replaceComments(discussionId, comments);
                    attachLikeStatus(comments, listener);
                })
                .addOnFailureListener(exception -> {
                    List<DiscussionComment> cachedComments = localDataSource.getComments(discussionId);

                    if (!cachedComments.isEmpty()) {
                        listener.onSuccess(cachedComments);
                    } else {
                        listener.onError(exception);
                    }
                });
    }

    // Kiểm tra từng comment xem người dùng hiện tại đã thích chưa, lỗi ở comment nào thì coi như chưa thích
    private void attachLikeStatus(List<DiscussionComment> comments, OnCommentsLoadedListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null || comments.isEmpty()) {
            listener.onSuccess(comments);
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (DiscussionComment comment : comments) {
            tasks.add(firestore.collection("commentLikes").document(comment.getId() + "_" + currentUser.getUid()).get());
        }

        Tasks.whenAllComplete(tasks).addOnSuccessListener(results -> {
            for (int i = 0; i < tasks.size(); i++) {
                Task<DocumentSnapshot> task = tasks.get(i);

                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    comments.get(i).setLiked(true);
                }
            }

            listener.onSuccess(comments);
        });
    }

    public void toggleLike(String commentId, String userId, OnCommentLikeToggledListener listener) {
        if (commentId == null || commentId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("Comment ID or user ID is invalid."));
            return;
        }

        DocumentReference commentRef = firestore.collection("comments").document(commentId);
        DocumentReference likeRef = firestore.collection("commentLikes").document(commentId + "_" + userId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot commentDoc = transaction.get(commentRef);
            DocumentSnapshot likeDoc = transaction.get(likeRef);

            if (!commentDoc.exists() || !"ACTIVE".equals(commentDoc.getString("status"))) {
                throw new IllegalStateException("This comment is no longer available.");
            }

            Long storedCount = commentDoc.getLong("likeCount");
            long likeCount = storedCount != null ? Math.max(0, storedCount) : 0;

            boolean isLiked;

            if (likeDoc.exists()) {
                transaction.delete(likeRef);
                likeCount = Math.max(0, likeCount - 1);
                isLiked = false;
            } else {
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("commentId", commentId);
                likeData.put("userId", userId);
                likeData.put("createdAt", FieldValue.serverTimestamp());

                transaction.set(likeRef, likeData);
                likeCount++;
                isLiked = true;
            }

            transaction.update(commentRef, "likeCount", likeCount);

            return new Object[]{isLiked, likeCount};
        }).addOnSuccessListener(result -> {
            Object[] values = (Object[]) result;
            listener.onSuccess((Boolean) values[0], (Long) values[1]);
        }).addOnFailureListener(listener::onError);
    }

    public void addComment(String discussionId, String content, OnCommentAddedListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            listener.onError(new IllegalStateException("User is not logged in."));
            return;
        }

        if (discussionId == null || discussionId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("Discussion ID is invalid."));
            return;
        }

        String trimmedContent = content == null ? "" : content.trim();

        if (trimmedContent.isEmpty()) {
            listener.onError(new IllegalArgumentException("Comment content is empty."));
            return;
        }

        if (trimmedContent.length() > CommentConstant.MAX_CONTENT_LENGTH) {
            listener.onError(new IllegalArgumentException("Comment cannot exceed 2000 characters."));
            return;
        }

        firestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDocument -> saveComment(currentUser, userDocument, discussionId, trimmedContent, listener))
                .addOnFailureListener(exception -> saveComment(currentUser, null, discussionId, trimmedContent, listener));
    }

    private void saveComment(FirebaseUser currentUser, DocumentSnapshot userDocument, String discussionId, String content, OnCommentAddedListener listener) {
        DocumentReference discussionRef = firestore.collection("discussions").document(discussionId);
        DocumentReference commentRef = firestore.collection("comments").document();

        String authorName = resolveAuthorName(currentUser, userDocument);
        String authorAvatar = resolveAuthorAvatar(currentUser, userDocument);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot discussionDoc = transaction.get(discussionRef);

            if (!discussionDoc.exists() || !"ACTIVE".equals(discussionDoc.getString("status"))) {
                throw new IllegalStateException("This discussion is no longer available.");
            }

            Long storedCount = discussionDoc.getLong("commentCount");
            long commentCount = (storedCount != null ? Math.max(0, storedCount) : 0) + 1;

            Map<String, Object> comment = new HashMap<>();
            comment.put("discussionId", discussionId);
            comment.put("userId", currentUser.getUid());
            comment.put("authorName", authorName);
            comment.put("authorAvatar", authorAvatar);
            comment.put("content", content);
            comment.put("likeCount", 0);
            comment.put("reportCount", 0);
            comment.put("status", "ACTIVE");
            comment.put("createdAt", FieldValue.serverTimestamp());
            comment.put("updatedAt", FieldValue.serverTimestamp());

            transaction.set(commentRef, comment);
            transaction.update(discussionRef, "commentCount", commentCount);

            return commentCount;
        }).addOnSuccessListener(commentCount -> listener.onSuccess(commentCount)).addOnFailureListener(listener::onError);
    }

    // Thứ tự ưu tiên: fullName -> username -> tên hiển thị của tài khoản Firebase -> "User"
    private String resolveAuthorName(FirebaseUser currentUser, DocumentSnapshot userDocument) {
        if (userDocument != null && userDocument.exists()) {
            String fullName = userDocument.getString("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();

            String username = userDocument.getString("username");
            if (username != null && !username.trim().isEmpty()) return username.trim();
        }

        String displayName = currentUser.getDisplayName();
        if (displayName != null && !displayName.trim().isEmpty()) return displayName.trim();

        return "User";
    }

    // Không có avatar thì lưu chuỗi rỗng, phần hiển thị sẽ dùng ảnh mặc định
    private String resolveAuthorAvatar(FirebaseUser currentUser, DocumentSnapshot userDocument) {
        if (userDocument != null && userDocument.exists()) {
            String avatar = userDocument.getString("avatar");
            if (avatar != null && !avatar.trim().isEmpty()) return avatar.trim();
        }

        if (currentUser.getPhotoUrl() != null) return currentUser.getPhotoUrl().toString();

        return "";
    }

    private DiscussionComment mapComment(DocumentSnapshot document) {
        DiscussionComment comment = new DiscussionComment();

        comment.setId(document.getId());
        comment.setDiscussionId(document.getString("discussionId"));
        comment.setUserId(document.getString("userId"));
        comment.setAuthorName(document.getString("authorName"));
        comment.setAuthorAvatar(document.getString("authorAvatar"));
        comment.setContent(document.getString("content"));

        Long likeCount = document.getLong("likeCount");
        comment.setLikeCount(likeCount != null ? likeCount : 0);

        Timestamp timestamp = document.getTimestamp("createdAt");

        if (timestamp != null) {
            comment.setCreatedAt(timestamp.toDate().getTime());
        }

        return comment;
    }
}