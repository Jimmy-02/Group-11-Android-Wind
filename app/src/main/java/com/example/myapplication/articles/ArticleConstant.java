package com.example.myapplication.articles;

public final class ArticleConstant {

    private ArticleConstant() {
        // Prevent instantiation
    }

    // Article title validation
    public static final int MIN_TITLE_LENGTH = 5;
    public static final int MAX_TITLE_LENGTH = 200;

    // Author name validation
    public static final int MIN_AUTHOR_NAME_LENGTH = 2;
    public static final int MAX_AUTHOR_NAME_LENGTH = 150;

    // Institution validation
    public static final int MIN_INSTITUTION_LENGTH = 2;
    public static final int MAX_INSTITUTION_LENGTH = 200;

    // Description validation
    public static final int MIN_DESCRIPTION_LENGTH = 10;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    // DOI validation
    public static final int MAX_DOI_LENGTH = 255;

    // URL validation
    public static final int MAX_URL_LENGTH = 2048;
}