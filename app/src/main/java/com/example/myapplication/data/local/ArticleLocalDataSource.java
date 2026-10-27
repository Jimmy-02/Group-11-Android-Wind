package com.example.myapplication.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.articles.Article;

import java.util.ArrayList;
import java.util.List;

public class ArticleLocalDataSource {

    private final AppDatabaseHelper databaseHelper;

    public ArticleLocalDataSource(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    public void saveArticles(String userId, List<Article> articles) {
        if (userId == null || userId.trim().isEmpty() || articles == null) {
            return;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            for (Article article : articles) {
                if (article == null || article.getId() == null) {
                    continue;
                }

                ContentValues values = new ContentValues();
                values.put("id", article.getId());
                values.put("userId", userId);
                values.put("title", article.getTitle());
                values.put("authorName", article.getAuthorName());
                values.put("categoryId", article.getCategoryId());
                values.put("institution", article.getInstitution());
                values.put("digitalObjectIdentifier", article.getDigitalObjectIdentifier());
                values.put("description", article.getDescription());
                values.put("imageUrl", article.getImageUrl());
                values.put("sourceUrl", article.getSourceUrl());
                values.put("contentUrl", article.getContentUrl());
                values.put("status", article.getStatus());
                values.put("createdAt", article.getCreatedAt());

                db.insertWithOnConflict(AppDatabaseHelper.TABLE_ARTICLES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void saveHomeArticles(List<Article> articles) {
        saveArticles("HOME_PUBLIC", articles);
    }
    public List<Article> getHomeArticles() {
        return getArticles("HOME_PUBLIC");
    }
    public List<Article> getArticles(String userId) {
        List<Article> articles = new ArrayList<>();

        if (userId == null || userId.trim().isEmpty()) {
            return articles;
        }

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                AppDatabaseHelper.TABLE_ARTICLES,
                null,
                "userId = ?",
                new String[]{userId},
                null,
                null,
                "createdAt DESC"
        )) {
            while (cursor.moveToNext()) {
                Article article = new Article();

                article.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
                article.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                article.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow("authorName")));
                article.setCategoryId(cursor.getString(cursor.getColumnIndexOrThrow("categoryId")));
                article.setInstitution(cursor.getString(cursor.getColumnIndexOrThrow("institution")));
                article.setDigitalObjectIdentifier(cursor.getString(cursor.getColumnIndexOrThrow("digitalObjectIdentifier")));
                article.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                article.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("imageUrl")));
                article.setSourceUrl(cursor.getString(cursor.getColumnIndexOrThrow("sourceUrl")));
                article.setContentUrl(cursor.getString(cursor.getColumnIndexOrThrow("contentUrl")));
                article.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                article.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")));

                articles.add(article);
            }
        } finally {
            db.close();
        }

        return articles;
    }

    public void deleteArticles(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        try {
            db.delete(AppDatabaseHelper.TABLE_ARTICLES, "userId = ?", new String[]{userId});
        } finally {
            db.close();
        }
    }

    public void close() {
        databaseHelper.close();
    }
}