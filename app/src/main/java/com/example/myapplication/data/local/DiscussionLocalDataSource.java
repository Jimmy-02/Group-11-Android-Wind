package com.example.myapplication.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.discussions.Discussion;

import java.util.ArrayList;
import java.util.List;

public class DiscussionLocalDataSource {

    public static final String ALL_DISCUSSIONS_KEY = "ALL_DISCUSSIONS";

    private final AppDatabaseHelper databaseHelper;

    public DiscussionLocalDataSource(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    public void saveDiscussions(String ownerKey, List<Discussion> discussions) {
        if (ownerKey == null || ownerKey.trim().isEmpty() || discussions == null) {
            return;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(AppDatabaseHelper.TABLE_DISCUSSIONS, "userId = ?", new String[]{ownerKey});

            for (Discussion discussion : discussions) {
                if (discussion == null || discussion.getId() == null) {
                    continue;
                }

                ContentValues values = new ContentValues();
                values.put("id", discussion.getId());
                values.put("userId", ownerKey);
                values.put("title", discussion.getTitle());
                values.put("content", discussion.getContent());
                values.put("category", discussion.getCategory());
                values.put("categoryId", discussion.getCategoryId());
                values.put("articleId", discussion.getArticleId());
                values.put("imageUrl", discussion.getImageUrl());
                values.put("authorId", discussion.getUserId());
                values.put("authorName", discussion.getAuthorName());
                values.put("authorAvatar", discussion.getAuthorAvatar());
                values.put("status", discussion.getStatus());
                values.put("likeCount", discussion.getLikeCount());
                values.put("commentCount", discussion.getCommentCount());
                values.put("createdAt", discussion.getCreatedAt());

                db.insertWithOnConflict(AppDatabaseHelper.TABLE_DISCUSSIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Discussion> getDiscussions(String ownerKey) {
        List<Discussion> discussions = new ArrayList<>();

        if (ownerKey == null || ownerKey.trim().isEmpty()) {
            return discussions;
        }

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(AppDatabaseHelper.TABLE_DISCUSSIONS, null, "userId = ?", new String[]{ownerKey}, null, null, "createdAt DESC")) {
            while (cursor.moveToNext()) {
                Discussion discussion = new Discussion();

                discussion.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
                discussion.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                discussion.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
                discussion.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
                discussion.setCategoryId(cursor.getString(cursor.getColumnIndexOrThrow("categoryId")));
                discussion.setArticleId(cursor.getString(cursor.getColumnIndexOrThrow("articleId")));
                discussion.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("imageUrl")));
                discussion.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("authorId")));
                discussion.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow("authorName")));
                discussion.setAuthorAvatar(cursor.getString(cursor.getColumnIndexOrThrow("authorAvatar")));
                discussion.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                discussion.setLikeCount(cursor.getLong(cursor.getColumnIndexOrThrow("likeCount")));
                discussion.setCommentCount(cursor.getLong(cursor.getColumnIndexOrThrow("commentCount")));
                discussion.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")));

                discussions.add(discussion);
            }
        }

        return discussions;
    }

    public void close() {
        databaseHelper.close();
    }
}