
package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private TextView tvPageTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPageTitle = findViewById(R.id.tv_page_title);

        BottomNavigationView bottomNavigation =
                findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                tvPageTitle.setText("Home");
            } else if (id == R.id.nav_discuss) {
                tvPageTitle.setText("Discuss");
            } else if (id == R.id.nav_qna) {
                tvPageTitle.setText("Q&A");
            } else if (id == R.id.nav_account) {
                tvPageTitle.setText("Account");
            } else {
                return false;
            }

            return true;
        });
    }
}