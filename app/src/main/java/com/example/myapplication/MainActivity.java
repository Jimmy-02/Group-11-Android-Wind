package com.example.myapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.myapplication.account.AccountActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigation =
                findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            openFragment(new HomeFragment());
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                openFragment(new HomeFragment());
            } else if (id == R.id.nav_qna) {
                openFragment(new QnaFragment());
            } else if (id == R.id.nav_account) {
                openFragment(new AccountActivity());
            } else {
                return false;
            }

            return true;
        });
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_container, fragment)
                .commit();
    }
}