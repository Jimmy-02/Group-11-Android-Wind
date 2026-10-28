package com.example.myapplication.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.qna.Question;
import com.google.firebase.Timestamp;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class QuestionLocalDataSource {

    private final AppDatabaseHelper databaseHelper;

    public QuestionLocalDataSource(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    // Lưu thêm hoặc cập nhật, không xóa dữ liệu cũ.
    public void saveQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            insertQuestions(db, questions);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Thay toàn bộ cache bằng danh sách mới (dùng khi vừa tải đủ tất cả câu hỏi từ server).
    public void replaceAllQuestions(List<Question> questions) {
        if (questions == null) {
            return;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(AppDatabaseHelper.TABLE_QUESTIONS, null, null);
            insertQuestions(db, questions);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Thay toàn bộ câu hỏi của một người dùng.
    public void replaceQuestionsOfUser(String userId, List<Question> questions) {
        if (userId == null || userId.trim().isEmpty() || questions == null) {
            return;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(AppDatabaseHelper.TABLE_QUESTIONS, "userId = ?", new String[]{userId});
            insertQuestions(db, questions);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateAnswerCount(String questionId, int answerCount) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.execSQL("UPDATE " + AppDatabaseHelper.TABLE_QUESTIONS + " SET answerCount = ? WHERE id = ?", new Object[]{answerCount, questionId});
    }

    private void insertQuestions(SQLiteDatabase db, List<Question> questions) {
        for (Question question : questions) {
            if (question == null || question.getId() == null || question.getUserId() == null) {
                continue;
            }

            ContentValues values = new ContentValues();
            values.put("id", question.getId());
            values.put("userId", question.getUserId());
            values.put("title", question.getTitle());
            values.put("content", question.getContent());
            values.put("categories", serializeCategories(question.getCategories()));
            values.put("imageUrl", question.getImageUrl());
            values.put("authorName", question.getAuthorName());
            values.put("authorAvatar", question.getAuthorAvatar());
            values.put("status", question.getStatus());
            values.put("voteCount", question.getVoteCount());
            values.put("answerCount", question.getAnswerCount());
            values.put("createdAt", question.getCreatedAt() == null ? 0L : question.getCreatedAt().toDate().getTime());

            db.insertWithOnConflict(AppDatabaseHelper.TABLE_QUESTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public List<Question> getAllQuestions() {
        return getQuestions(null, null);
    }

    public List<Question> getMyQuestions(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return getQuestions("userId = ?", new String[]{userId});
    }

    public Question getQuestionById(String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return null;
        }

        List<Question> questions = getQuestions("id = ?", new String[]{questionId});
        return questions.isEmpty() ? null : questions.get(0);
    }

    private List<Question> getQuestions(String selection, String[] selectionArgs) {
        List<Question> questions = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(AppDatabaseHelper.TABLE_QUESTIONS, null, selection, selectionArgs, null, null, "createdAt DESC")) {
            while (cursor.moveToNext()) {
                Question question = new Question();

                question.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
                question.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("userId")));
                question.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                question.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
                question.setCategories(deserializeCategories(cursor.getString(cursor.getColumnIndexOrThrow("categories"))));
                question.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("imageUrl")));
                question.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow("authorName")));
                question.setAuthorAvatar(cursor.getString(cursor.getColumnIndexOrThrow("authorAvatar")));
                question.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                question.setVoteCount(cursor.getLong(cursor.getColumnIndexOrThrow("voteCount")));
                question.setAnswerCount(cursor.getInt(cursor.getColumnIndexOrThrow("answerCount")));

                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
                question.setCreatedAt(createdAt > 0 ? new Timestamp(new java.util.Date(createdAt)) : null);

                if (!"DELETED".equalsIgnoreCase(question.getStatus())) {
                    questions.add(question);
                }
            }
        }

        return questions;
    }

    private String serializeCategories(List<String> categories) {
        JSONArray jsonArray = new JSONArray();

        if (categories != null) {
            for (String category : categories) {
                jsonArray.put(category);
            }
        }

        return jsonArray.toString();
    }

    private List<String> deserializeCategories(String value) {
        List<String> categories = new ArrayList<>();

        if (value == null || value.trim().isEmpty()) {
            return categories;
        }

        try {
            JSONArray jsonArray = new JSONArray(value);

            for (int i = 0; i < jsonArray.length(); i++) {
                categories.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            android.util.Log.e("QuestionLocalDataSource", "Không thể đọc danh mục câu hỏi từ SQLite", e);
        }

        return categories;
    }

    public void close() {
        databaseHelper.close();
    }
}