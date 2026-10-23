package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.articles.Article;
import com.example.myapplication.articles.ArticleAdapter;
import com.example.myapplication.articles.ArticleRepository;

import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView articleList;
    private ArticleAdapter articleAdapter;
    private TextView articleCount;
    private View emptyState;

    private ArticleRepository repository;
    private int totalArticles = 0;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        articleList = view.findViewById(R.id.rv_article_list);
        emptyState = view.findViewById(R.id.layout_empty_articles);

        articleAdapter = new ArticleAdapter(
                article -> Toast.makeText(
                        requireContext(),
                        "Article: " + article.getTitle(),
                        Toast.LENGTH_SHORT
                ).show(),
                article -> Toast.makeText(
                        requireContext(),
                        "Report feature will be added later.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext());

        articleList.setLayoutManager(layoutManager);
        articleList.setAdapter(articleAdapter);

        repository = new ArticleRepository();

        loadArticles();
    }

    private void loadArticles() {
        articleList.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        repository.getLatestArticles(new ArticleRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<Article> articles) {
                if (!isAdded()) {
                    return;
                }

                articleAdapter.setArticles(articles);
                totalArticles = articles.size();


                emptyState.setVisibility(
                        articles.isEmpty() ? View.VISIBLE : View.GONE
                );

                articleList.setVisibility(
                        articles.isEmpty() ? View.GONE : View.VISIBLE
                );
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) {
                    return;
                }

                android.util.Log.e(
                        "HomeFragment",
                        "Failed to load articles",
                        exception
                );

                Toast.makeText(
                        requireContext(),
                        "Unable to load articles.",
                        Toast.LENGTH_SHORT
                ).show();

                articleList.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            }
        });
    }
}