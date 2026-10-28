package com.example.myapplication.qna;

import android.content.Context;

import com.example.myapplication.data.local.QuestionLocalDataSource;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionRepository {

    private final FirebaseFirestore firestore;
    private final QuestionLocalDataSource localDataSource;

    public QuestionRepository(Context context) {
        firestore = FirebaseFirestore.getInstance();
        localDataSource = new QuestionLocalDataSource(context);
    }

    public interface OnQuestionsLoadedListener {
        void onSuccess(List<Question> questions);
        void onError(Exception exception);
    }

    // Lấy tất cả câu hỏi: ưu tiên Firestore, dự phòng SQLite.
    public void getAllQuestions(OnQuestionsLoadedListener listener) {
        firestore.collection("questions")
                .get()
                .addOnSuccessListener(snapshot -> {
                    // Offline: snapshot rỗng lấy từ cache không phải dữ liệu thật từ server.
                    if (snapshot.isEmpty() && snapshot.getMetadata().isFromCache()) {
                        deliverCached(localDataSource.getAllQuestions(), new Exception("Offline: no data from server"), listener);
                        return;
                    }

                    List<Question> questions = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Question question = mapQuestion(document);

                        if (question != null) {
                            questions.add(question);
                        }
                    }

                    sortByNewest(questions);
                    localDataSource.replaceAllQuestions(questions);
                    listener.onSuccess(questions);
                })
                .addOnFailureListener(exception -> deliverCached(localDataSource.getAllQuestions(), exception, listener));
    }

    // Lấy câu hỏi của một người dùng: ưu tiên Firestore, dự phòng SQLite.
    public void getMyQuestions(String userId, OnQuestionsLoadedListener listener) {
        if (userId == null || userId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("User ID không hợp lệ."));
            return;
        }

        firestore.collection("questions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty() && snapshot.getMetadata().isFromCache()) {
                        deliverCached(localDataSource.getMyQuestions(userId), new Exception("Offline: no data from server"), listener);
                        return;
                    }

                    List<Question> questions = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Question question = mapQuestion(document);

                        if (question != null) {
                            questions.add(question);
                        }
                    }

                    sortByNewest(questions);
                    localDataSource.replaceQuestionsOfUser(userId, questions);
                    listener.onSuccess(questions);
                })
                .addOnFailureListener(exception -> deliverCached(localDataSource.getMyQuestions(userId), exception, listener));
    }

    // Lấy một câu hỏi theo document ID.
    public void getQuestionById(String questionId, OnQuestionLoadedListener listener) {
        if (questionId == null || questionId.trim().isEmpty()) {
            listener.onError(new IllegalArgumentException("Question ID không hợp lệ."));
            return;
        }

        firestore.collection("questions")
                .document(questionId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists() && document.getMetadata().isFromCache()) {
                        deliverCachedQuestion(questionId, new Exception("Offline: no data from server"), listener);
                        return;
                    }

                    Question question = document.exists() ? mapQuestion(document) : null;

                    if (question == null) {
                        listener.onError(new IllegalStateException("Không tìm thấy câu hỏi."));
                    } else {
                        localDataSource.saveQuestions(Collections.singletonList(question));
                        listener.onSuccess(question);
                    }
                })
                .addOnFailureListener(exception -> deliverCachedQuestion(questionId, exception, listener));
    }

    public interface OnQuestionLoadedListener {
        void onSuccess(Question question);
        void onError(Exception exception);
    }

    private void deliverCached(List<Question> cachedQuestions, Exception exception, OnQuestionsLoadedListener listener) {
        if (cachedQuestions.isEmpty()) {
            listener.onError(exception);
            return;
        }

        sortByNewest(cachedQuestions);
        listener.onSuccess(cachedQuestions);
    }

    private void deliverCachedQuestion(String questionId, Exception exception, OnQuestionLoadedListener listener) {
        Question cachedQuestion = localDataSource.getQuestionById(questionId);

        if (cachedQuestion == null) {
            listener.onError(exception);
            return;
        }

        listener.onSuccess(cachedQuestion);
    }

    // Chuyển Firestore document thành Question.
    private Question mapQuestion(DocumentSnapshot document) {
        String status = document.getString("status");

        if ("DELETED".equalsIgnoreCase(status) || document.get("deletedAt") != null) {
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

        Object answerCountValue = document.get("answerCount");
        android.util.Log.d("QuestionRepository", "Question ID: " + document.getId() + ", answerCount: " + answerCountValue + ", type: " + (answerCountValue == null ? "null" : answerCountValue.getClass().getSimpleName()));

        if (answerCountValue instanceof Number) {
            question.setAnswerCount(((Number) answerCountValue).intValue());
        } else {
            question.setAnswerCount(0);
        }

        return question;
    }

    private void sortByNewest(List<Question> questions) {
        Collections.sort(questions, (first, second) -> Long.compare(getTime(second.getCreatedAt()), getTime(first.getCreatedAt())));
    }

    private long getTime(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toDate().getTime();
    }
    public List<Question> getCachedQuestions() {
        List<Question> questions = localDataSource.getAllQuestions();
        sortByNewest(questions);
        return questions;
    }
    public void close() {
        localDataSource.close();
    }
}