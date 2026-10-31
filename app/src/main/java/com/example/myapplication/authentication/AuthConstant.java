package com.example.myapplication.authentication;

public final class AuthConstant {

    //giúp không cần tạo đối tượng cho những class khác khi sử dụng auth constant
    private AuthConstant() {
        // Prevent instantiation
    }

    // Full name validation
    public static final int MIN_FULL_NAME_LENGTH = 2;
    public static final int MAX_FULL_NAME_LENGTH = 100;

    // Username validation
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 30;

    // Email validation
    public static final int MAX_EMAIL_LENGTH = 254;

    // Password validation
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 128;
}