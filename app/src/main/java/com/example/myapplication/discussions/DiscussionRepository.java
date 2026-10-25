package com.example.myapplication.discussions;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscussionRepository {

    private final FirebaseFirestore firestore;

    public DiscussionRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface OnDiscussionsLoadedListener {
        void onSuccess(List<Discussion> discussions);
        void onError(Exception exception);
    }

    public void getAllDiscussions(OnDiscussionsLoadedListener listener) {
        firestore.collection("discussions")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Discussion> discussions = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Discussion discussion = mapDiscussion(document);

                        if (discussion != null) {
                            discussions.add(discussion);
                        }
                    }

                    sortByNewest(discussions);
                    listener.onSuccess(discussions);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getMyDiscussions(
            String userId,
            OnDiscussionsLoadedListener listener
    ) {
        firestore.collection("discussions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Discussion> discussions = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Discussion discussion = mapDiscussion(document);

                        if (discussion != null) {
                            discussions.add(discussion);
                        }
                    }

                    sortByNewest(discussions);
                    listener.onSuccess(discussions);
                })
                .addOnFailureListener(listener::onError);
    }

    private Discussion mapDiscussion(DocumentSnapshot document) {
        Discussion discussion = new Discussion();

        discussion.setId(document.getId());
        discussion.setTitle(document.getString("title"));
        discussion.setContent(document.getString("content"));
        discussion.setCategory(document.getString("category"));
        discussion.setCategoryId(document.getString("categoryId"));
        discussion.setArticleId(document.getString("articleId"));
        discussion.setImageUrl(document.getString("imageUrl"));
        discussion.setUserId(document.getString("userId"));
        discussion.setStatus(document.getString("status"));

        Long likeCount = document.getLong("likeCount");
        Long commentCount = document.getLong("commentCount");

        discussion.setLikeCount(likeCount != null ? likeCount : 0);
        discussion.setCommentCount(commentCount != null ? commentCount : 0);

        Timestamp timestamp = document.getTimestamp("createdAt");

        if (timestamp != null) {
            discussion.setCreatedAt(timestamp.toDate().getTime());
        }

        return discussion;
    }

    private void sortByNewest(List<Discussion> discussions) {
        Collections.sort(
                discussions,
                (first, second) -> Long.compare(
                        second.getCreatedAt(),
                        first.getCreatedAt()
                )
        );
    }

    public interface OnLikeStatusListener {
        void onSuccess(boolean isLiked);
        void onError(Exception exception);
    }

    public interface OnLikeToggledListener {
        void onSuccess(boolean isLiked, long likeCount);
        void onError(Exception exception);
    }

    public void checkLikeStatus(String discussionId, String userId, OnLikeStatusListener listener) {
        if (discussionId == null || discussionId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("Discussion ID or user ID is invalid."));
            return;
        }

        firestore.collection("discussions").document(discussionId).collection("likes").document(userId).get()
                .addOnSuccessListener(document -> listener.onSuccess(document.exists()))
                .addOnFailureListener(listener::onError);
    }

    public void toggleLike(String discussionId, String userId, OnLikeToggledListener listener) {
        if (discussionId == null || discussionId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("Discussion ID or user ID is invalid."));
            return;
        }

        com.google.firebase.firestore.DocumentReference discussionRef = firestore.collection("discussions").document(discussionId);
        com.google.firebase.firestore.DocumentReference likeRef = discussionRef.collection("likes").document(userId);

        firestore.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot discussionDoc = transaction.get(discussionRef);
            com.google.firebase.firestore.DocumentSnapshot likeDoc = transaction.get(likeRef);

            if (!discussionDoc.exists() || !"ACTIVE".equals(discussionDoc.getString("status"))) {
                throw new IllegalStateException("This discussion is no longer available.");
            }

            Long storedCount = discussionDoc.getLong("likeCount");
            long likeCount = storedCount != null ? Math.max(0, storedCount) : 0;

            boolean isLiked;

            if (likeDoc.exists()) {
                transaction.delete(likeRef);
                likeCount = Math.max(0, likeCount - 1);
                isLiked = false;
            } else {
                java.util.Map<String, Object> likeData = new java.util.HashMap<>();
                likeData.put("userId", userId);
                likeData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                transaction.set(likeRef, likeData);
                likeCount++;
                isLiked = true;
            }

            transaction.update(discussionRef, "likeCount", likeCount);

            return new Object[]{isLiked, likeCount};
        }).addOnSuccessListener(result -> {
            Object[] values = (Object[]) result;
            listener.onSuccess((Boolean) values[0], (Long) values[1]);
        }).addOnFailureListener(listener::onError);
    }
}