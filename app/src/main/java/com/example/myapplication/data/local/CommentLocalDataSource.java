package com.example.myapplication.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.discussions.comments.DiscussionComment;

import java.util.ArrayList;
import java.util.List;

public class CommentLocalDataSource {

    private final AppDatabaseHelper databaseHelper;

    public CommentLocalDataSource(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    public void replaceComments(String discussionId, List<DiscussionComment> comments) {
        if (discussionId == null || discussionId.trim().isEmpty() || comments == null) return;

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(AppDatabaseHelper.TABLE_COMMENTS, "discussionId = ?", new String[]{discussionId});

            for (DiscussionComment comment : comments) {
                if (comment == null || comment.getId() == null) continue;

                ContentValues values = new ContentValues();
                values.put("id", comment.getId());
                values.put("discussionId", discussionId);
                values.put("userId", comment.getUserId());
                values.put("authorName", comment.getAuthorName());
                values.put("authorAvatar", comment.getAuthorAvatar());
                values.put("content", comment.getContent());
                values.put("likeCount", comment.getLikeCount());
                values.put("createdAt", comment.getCreatedAt());

                db.insertWithOnConflict(AppDatabaseHelper.TABLE_COMMENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<DiscussionComment> getComments(String discussionId) {
        List<DiscussionComment> comments = new ArrayList<>();

        if (discussionId == null || discussionId.trim().isEmpty()) return comments;

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                AppDatabaseHelper.TABLE_COMMENTS,
                null,
                "discussionId = ?",
                new String[]{discussionId},
                null,
                null,
                "createdAt DESC")) {

            while (cursor.moveToNext()) {
                DiscussionComment comment = new DiscussionComment();

                comment.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
                comment.setDiscussionId(cursor.getString(cursor.getColumnIndexOrThrow("discussionId")));
                comment.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("userId")));
                comment.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow("authorName")));
                comment.setAuthorAvatar(cursor.getString(cursor.getColumnIndexOrThrow("authorAvatar")));
                comment.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
                comment.setLikeCount(cursor.getLong(cursor.getColumnIndexOrThrow("likeCount")));
                comment.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")));
                comment.setLiked(false);

                comments.add(comment);
            }
        }

        return comments;
    }

    public void close() {
        databaseHelper.close();
    }
}