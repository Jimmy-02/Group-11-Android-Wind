package com.example.myapplication.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.qna.questionDetail.Answer;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class AnswerLocalDataSource {

    private final AppDatabaseHelper databaseHelper;

    public AnswerLocalDataSource(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    // Thay toàn bộ câu trả lời của một người dùng.
    public void replaceAnswersOfUser(String userId, List<Answer> answers) {
        if (userId == null || userId.trim().isEmpty() || answers == null) {
            return;
        }

        replaceAnswers("userId = ?", new String[]{userId}, answers);
    }

    // Thay toàn bộ câu trả lời của một câu hỏi.
    public void replaceAnswersOfQuestion(String questionId, List<Answer> answers) {
        if (questionId == null || questionId.trim().isEmpty() || answers == null) {
            return;
        }

        replaceAnswers("questionId = ?", new String[]{questionId}, answers);
    }

    private void replaceAnswers(String whereClause, String[] whereArgs, List<Answer> answers) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(AppDatabaseHelper.TABLE_ANSWERS, whereClause, whereArgs);

            for (Answer answer : answers) {
                insertAnswer(db, answer);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertAnswer(SQLiteDatabase db, Answer answer) {
        if (answer == null || answer.getId() == null || answer.getQuestionId() == null || answer.getUserId() == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("id", answer.getId());
        values.put("questionId", answer.getQuestionId());
        values.put("userId", answer.getUserId());
        values.put("content", answer.getContent());
        values.put("authorName", answer.getAuthorName());
        values.put("authorAvatar", answer.getAuthorAvatar());
        values.put("voteCount", answer.getVoteCount());
        values.put("createdAt", answer.getCreatedAt() == null ? 0L : answer.getCreatedAt().toDate().getTime());

        db.insertWithOnConflict(AppDatabaseHelper.TABLE_ANSWERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Answer> getAnswersOfUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return queryAnswers("userId = ?", userId);
    }

    public List<Answer> getAnswersOfQuestion(String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return queryAnswers("questionId = ?", questionId);
    }

    private List<Answer> queryAnswers(String selection, String argument) {
        List<Answer> answers = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(AppDatabaseHelper.TABLE_ANSWERS, null, selection, new String[]{argument}, null, null, "createdAt DESC")) {
            while (cursor.moveToNext()) {
                Answer answer = new Answer();

                answer.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
                answer.setQuestionId(cursor.getString(cursor.getColumnIndexOrThrow("questionId")));
                answer.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("userId")));
                answer.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
                answer.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow("authorName")));
                answer.setAuthorAvatar(cursor.getString(cursor.getColumnIndexOrThrow("authorAvatar")));
                answer.setVoteCount(cursor.getLong(cursor.getColumnIndexOrThrow("voteCount")));

                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
                answer.setCreatedAt(createdAt > 0 ? new Timestamp(new java.util.Date(createdAt)) : null);

                answers.add(answer);
            }
        }

        return answers;
    }

    public void close() {
        databaseHelper.close();
    }
}