package com.example.myapplication.qna.questionDetail;

import android.content.Context;

import com.example.myapplication.data.local.AnswerLocalDataSource;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnswerRepository {

    private final FirebaseFirestore firestore;
    private final AnswerLocalDataSource localDataSource;

    public AnswerRepository(Context context) {
        firestore = FirebaseFirestore.getInstance();
        localDataSource = new AnswerLocalDataSource(context);
    }

    public interface OnAnswersLoadedListener {
        void onSuccess(List<Answer> answers);
        void onError(Exception exception);
    }

    public interface OnAnswerLoadedListener {
        void onSuccess(Answer answer);
        void onError(Exception exception);
    }

    public void getAnswersByQuestionId(String questionId, OnAnswersLoadedListener listener) {
        if (questionId == null || questionId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("Question ID không hợp lệ."));
            return;
        }

        firestore.collection("answers")
                .whereEqualTo("questionId", questionId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Answer> answers = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Answer answer = mapAnswer(document);
                        if (answer != null) answers.add(answer);
                    }

                    sortByNewest(answers);

                    if (snapshot.getMetadata().isFromCache()) {
                        List<Answer> cachedAnswers = localDataSource.getAnswersOfQuestion(questionId);

                        if (!cachedAnswers.isEmpty()) {
                            listener.onSuccess(cachedAnswers);
                        } else if (!answers.isEmpty()) {
                            listener.onSuccess(answers);
                        } else {
                            listener.onError(new Exception("Chưa có câu trả lời được lưu offline."));
                        }
                        return;
                    }

                    localDataSource.replaceAnswersOfQuestion(questionId, answers);
                    listener.onSuccess(answers);
                })
                .addOnFailureListener(exception -> {
                    List<Answer> cachedAnswers = localDataSource.getAnswersOfQuestion(questionId);

                    if (!cachedAnswers.isEmpty()) {
                        listener.onSuccess(cachedAnswers);
                    } else {
                        listener.onError(exception);
                    }
                });
    }

    public void getMyAnswers(String userId, OnAnswersLoadedListener listener) {
        if (userId == null || userId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("User ID không hợp lệ."));
            return;
        }

        firestore.collection("answers")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Answer> answers = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Answer answer = mapAnswer(document);
                        if (answer != null) answers.add(answer);
                    }

                    sortByNewest(answers);

                    if (snapshot.getMetadata().isFromCache()) {
                        List<Answer> cachedAnswers = localDataSource.getAnswersOfUser(userId);

                        if (!cachedAnswers.isEmpty()) {
                            listener.onSuccess(cachedAnswers);
                        } else if (!answers.isEmpty()) {
                            listener.onSuccess(answers);
                        } else {
                            listener.onError(new Exception("Chưa có câu trả lời được lưu offline."));
                        }
                        return;
                    }

                    localDataSource.replaceAnswersOfUser(userId, answers);
                    listener.onSuccess(answers);
                })
                .addOnFailureListener(exception -> {
                    List<Answer> cachedAnswers = localDataSource.getAnswersOfUser(userId);

                    if (!cachedAnswers.isEmpty()) {
                        listener.onSuccess(cachedAnswers);
                    } else {
                        listener.onError(exception);
                    }
                });
    }

    public void getAnswerById(String answerId, OnAnswerLoadedListener listener) {
        if (answerId == null || answerId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("Answer ID không hợp lệ."));
            return;
        }

        firestore.collection("answers")
                .document(answerId)
                .get()
                .addOnSuccessListener(document -> {
                    Answer answer = document.exists() ? mapAnswer(document) : null;

                    if (answer == null) {
                        listener.onError(new IllegalStateException("Không tìm thấy câu trả lời."));
                    } else {
                        List<Answer> cachedAnswers = new ArrayList<>();
                        cachedAnswers.add(answer);
                        localDataSource.replaceAnswersOfQuestion(answer.getQuestionId(), mergeWithCachedAnswers(answer));
                        listener.onSuccess(answer);
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    private List<Answer> mergeWithCachedAnswers(Answer answer) {
        List<Answer> answers = localDataSource.getAnswersOfQuestion(answer.getQuestionId());
        answers.removeIf(item -> answer.getId().equals(item.getId()));
        answers.add(answer);
        sortByNewest(answers);
        return answers;
    }

    private Answer mapAnswer(DocumentSnapshot document) {
        String status = document.getString("status");

        if ("DELETED".equalsIgnoreCase(status) || document.get("deletedAt") != null) {
            return null;
        }

        Answer answer = new Answer();
        answer.setId(document.getId());
        answer.setQuestionId(document.getString("questionId"));
        answer.setUserId(document.getString("userId"));
        answer.setAuthorName(document.getString("authorName"));
        answer.setAuthorAvatar(document.getString("authorAvatar"));
        answer.setContent(document.getString("content"));

        Timestamp createdAt = document.getTimestamp("createdAt");
        answer.setCreatedAt(createdAt);

        Long voteCount = document.getLong("voteCount");
        answer.setVoteCount(voteCount == null ? 0 : voteCount);

        return answer;
    }

    private void sortByNewest(List<Answer> answers) {
        Collections.sort(answers, (first, second) ->
                Long.compare(getTime(second.getCreatedAt()), getTime(first.getCreatedAt())));
    }

    private long getTime(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toDate().getTime();
    }

    public void close() {
        localDataSource.close();
    }
}