package com.example.myapplication;

import android.app.Application;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FirestoreSetup", "MyApplication.onCreate chay");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build();
        firestore.setFirestoreSettings(settings);
        Log.d("FirestoreSetup", "Cache settings: " + firestore.getFirestoreSettings().getCacheSettings());
    }
}