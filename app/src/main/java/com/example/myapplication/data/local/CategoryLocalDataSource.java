package com.example.myapplication.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.articles.categories.ArticleCategory;

import java.util.ArrayList;
import java.util.List;

public class CategoryLocalDataSource {

    private final AppDatabaseHelper databaseHelper;

    public CategoryLocalDataSource(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    public void saveCategories(List<ArticleCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(AppDatabaseHelper.TABLE_CATEGORIES, null, null);

            for (ArticleCategory category : categories) {
                if (category == null || category.getId() == null || category.getName() == null) {
                    continue;
                }

                ContentValues values = new ContentValues();
                values.put("id", category.getId());
                values.put("name", category.getName());
                values.put("displayOrder", category.getDisplayOrder());

                db.insertWithOnConflict(AppDatabaseHelper.TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<ArticleCategory> getCategories() {
        List<ArticleCategory> categories = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(AppDatabaseHelper.TABLE_CATEGORIES, null, null, null, null, null, "displayOrder ASC")) {
            while (cursor.moveToNext()) {
                ArticleCategory category = new ArticleCategory();

                category.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                category.setDisplayOrder(cursor.getInt(cursor.getColumnIndexOrThrow("displayOrder")));
                category.setActive(true);

                categories.add(category);
            }
        }

        return categories;
    }
}