package com.example.myapplication.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "scishare.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_ARTICLES = "articles";
    public static final String TABLE_DISCUSSIONS = "discussions";
    public static final String TABLE_QUESTIONS = "questions";
    public static final String TABLE_ANSWERS = "answers";
    public static final String TABLE_CATEGORIES = "categories";

    public AppDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createArticlesTable(db);
        createDiscussionsTable(db);
        createQuestionsTable(db);
        createAnswersTable(db);
        createCategoriesTable(db);
    }

    private void createArticlesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ARTICLES + " (" +
                "id TEXT NOT NULL, " +
                "userId TEXT NOT NULL, " +
                "title TEXT, " +
                "authorName TEXT, " +
                "categoryId TEXT, " +
                "institution TEXT, " +
                "digitalObjectIdentifier TEXT, " +
                "description TEXT, " +
                "imageUrl TEXT, " +
                "sourceUrl TEXT, " +
                "contentUrl TEXT, " +
                "status TEXT, " +
                "createdAt INTEGER, " +
                "deletedAt INTEGER, " +
                "PRIMARY KEY (id, userId))");
    }

    private void createDiscussionsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_DISCUSSIONS + " (" +
                "id TEXT NOT NULL, " +
                "userId TEXT NOT NULL, " +
                "title TEXT, " +
                "content TEXT, " +
                "category TEXT, " +
                "categoryId TEXT, " +
                "articleId TEXT, " +
                "imageUrl TEXT, " +
                "authorId TEXT, " +
                "authorName TEXT, " +
                "authorAvatar TEXT, " +
                "status TEXT, " +
                "likeCount INTEGER DEFAULT 0, " +
                "commentCount INTEGER DEFAULT 0, " +
                "createdAt INTEGER, " +
                "PRIMARY KEY (id, userId))");
    }

    private void createQuestionsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_QUESTIONS + " (" +
                "id TEXT NOT NULL, " +
                "userId TEXT NOT NULL, " +
                "title TEXT, " +
                "content TEXT, " +
                "categories TEXT, " +
                "imageUrl TEXT, " +
                "authorName TEXT, " +
                "authorAvatar TEXT, " +
                "status TEXT, " +
                "voteCount INTEGER DEFAULT 0, " +
                "answerCount INTEGER DEFAULT 0, " +
                "createdAt INTEGER, " +
                "PRIMARY KEY (id, userId))");
    }

    private void createAnswersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ANSWERS + " (" +
                "id TEXT NOT NULL, " +
                "questionId TEXT NOT NULL, " +
                "userId TEXT NOT NULL, " +
                "content TEXT, " +
                "authorName TEXT, " +
                "authorAvatar TEXT, " +
                "voteCount INTEGER DEFAULT 0, " +
                "createdAt INTEGER, " +
                "PRIMARY KEY (id, userId))");
    }

    private void createCategoriesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (id TEXT NOT NULL PRIMARY KEY, name TEXT, displayOrder INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createCategoriesTable(db);
        }

        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_DISCUSSIONS + " ADD COLUMN authorId TEXT");
        }
    }
}