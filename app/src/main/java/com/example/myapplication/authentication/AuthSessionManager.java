package com.example.myapplication.authentication;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.authentication.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AuthSessionManager {

    public static void logout(AppCompatActivity activity) {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(activity, LoginActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        activity.startActivity(intent);
        activity.finish();
    }
}