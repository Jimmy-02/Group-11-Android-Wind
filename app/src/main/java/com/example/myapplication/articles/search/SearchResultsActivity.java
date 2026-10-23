package com.example.myapplication.articles.search;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class SearchResultsActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY = "extra_query";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        String query = getIntent().getStringExtra(EXTRA_QUERY);

        TextView searchQuery = findViewById(R.id.tv_search_query);
        searchQuery.setText(query == null ? "" : query);
    }
}