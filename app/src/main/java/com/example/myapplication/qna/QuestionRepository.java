
package com.example.myapplication.qna;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionRepository {

    private final FirebaseFirestore firestore;

    public QuestionRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface OnQuestionsLoadedListener {
        void onSuccess(List<Question> questions);
        void onError(Exception exception);
    }

    // Lấy tất cả câu hỏi chưa bị xóa.
    public void getAllQuestions(OnQuestionsLoadedListener listener) {
        firestore.collection("questions")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Question> questions = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Question question = mapQuestion(document);

                        if (question != null) {
                            questions.add(question);
                        }
                    }

                    sortByNewest(questions);
                    listener.onSuccess(questions);
                })
                .addOnFailureListener(listener::onError);
    }

    // Lấy câu hỏi của một người dùng.
    public void getMyQuestions(
            String userId,
            OnQuestionsLoadedListener listener
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            listener.onError(
                    new IllegalArgumentException("User ID không hợp lệ.")
            );
            return;
        }

        firestore.collection("questions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Question> questions = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Question question = mapQuestion(document);

                        if (question != null) {
                            questions.add(question);
                        }
                    }

                    sortByNewest(questions);
                    listener.onSuccess(questions);
                })
                .addOnFailureListener(listener::onError);
    }

    // Lấy một câu hỏi theo document ID.
    public void getQuestionById(
            String questionId,
            OnQuestionLoadedListener listener
    ) {
        if (questionId == null || questionId.trim().isEmpty()) {
            listener.onError(
                    new IllegalArgumentException("Question ID không hợp lệ.")
            );
            return;
        }

        firestore.collection("questions")
                .document(questionId)
                .get()
                .addOnSuccessListener(document -> {
                    Question question = document.exists()
                            ? mapQuestion(document)
                            : null;

                    if (question == null) {
                        listener.onError(
                                new IllegalStateException(
                                        "Không tìm thấy câu hỏi."
                                )
                        );
                    } else {
                        listener.onSuccess(question);
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public interface OnQuestionLoadedListener {
        void onSuccess(Question question);
        void onError(Exception exception);
    }

    // Chuyển Firestore document thành Question.
    private Question mapQuestion(DocumentSnapshot document) {
        String status = document.getString("status");

        if ("DELETED".equalsIgnoreCase(status)
                || document.get("deletedAt") != null) {
            return null;
        }

        Question question = new Question();

        question.setId(document.getId());
        question.setTitle(document.getString("title"));
        question.setContent(document.getString("content"));
        question.setUserId(document.getString("userId"));
        question.setAuthorName(document.getString("authorName"));
        question.setAuthorAvatar(document.getString("authorAvatar"));
        question.setImageUrl(document.getString("imageUrl"));
        question.setStatus(status == null ? "OPEN" : status);

        Object categoriesValue = document.get("categories");
        List<String> categories = new ArrayList<>();

        if (categoriesValue instanceof List<?>) {
            for (Object item : (List<?>) categoriesValue) {
                if (item instanceof String) {
                    categories.add((String) item);
                }
            }
        } else {
            String category = document.getString("category");

            if (category != null && !category.trim().isEmpty()) {
                categories.add(category);
            }
        }

        question.setCategories(categories);

        Timestamp createdAt = document.getTimestamp("createdAt");
        question.setCreatedAt(createdAt);

        Long voteCount = document.getLong("voteCount");
        question.setVoteCount(voteCount == null ? 0 : voteCount);

        Long answerCount = document.getLong("answerCount");
        question.setAnswerCount(answerCount == null ? 0 : answerCount.intValue());

        return question;
    }

    private void sortByNewest(List<Question> questions) {
        Collections.sort(
                questions,
                (first, second) -> Long.compare(
                        getTime(second.getCreatedAt()),
                        getTime(first.getCreatedAt())
                )
        );
    }

    private long getTime(Timestamp timestamp) {
        return timestamp == null
                ? 0L
                : timestamp.toDate().getTime();
    }
}
