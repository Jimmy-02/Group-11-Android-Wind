package com.example.myapplication.discussions;

public final class DiscussionConstant {

    private DiscussionConstant() {
        // Prevent instantiation
    }

    // Discussion title validation
    public static final int MIN_TITLE_LENGTH = 5;
    public static final int MAX_TITLE_LENGTH = 200;

    // Discussion content validation
    public static final int MIN_CONTENT_LENGTH = 10;
    public static final int MAX_CONTENT_LENGTH = 5000;

    // Image URL validation
    public static final int MAX_IMAGE_URL_LENGTH = 2048;
}