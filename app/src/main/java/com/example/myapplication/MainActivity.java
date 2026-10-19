
package com.example.myapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.myapplication.account.AccountActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Fragment homeFragment;
    private Fragment qnaFragment;
    private Fragment accountFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();

        homeFragment = fm.findFragmentByTag("HOME");
        qnaFragment = fm.findFragmentByTag("QNA");
        accountFragment = fm.findFragmentByTag("ACCOUNT");

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }

        if (qnaFragment == null) {
            qnaFragment = new QnaFragment();
        }

        if (accountFragment == null) {
            accountFragment = new AccountActivity();
        }

        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.content_container, homeFragment, "HOME")
                    .add(R.id.content_container, qnaFragment, "QNA")
                    .hide(qnaFragment)
                    .add(R.id.content_container, accountFragment, "ACCOUNT")
                    .hide(accountFragment)
                    .commit();
        }

        BottomNavigationView bottomNavigation =
                findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                showFragment(homeFragment);
            } else if (id == R.id.nav_qna) {
                showFragment(qnaFragment);
            } else if (id == R.id.nav_account) {
                showFragment(accountFragment);
            } else {
                return false;
            }

            return true;
        });
    }

    private void showFragment(Fragment target) {
        FragmentManager fm = getSupportFragmentManager();

        fm.beginTransaction()
                .hide(homeFragment)
                .hide(qnaFragment)
                .hide(accountFragment)
                .show(target)
                .commit();
    }
}
