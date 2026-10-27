package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.myapplication.data.local.AppDatabaseHelper;
import com.example.myapplication.data.local.ArticleLocalDataSource;
import com.google.firebase.auth.FirebaseAuth;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.articles.Article;
import com.example.myapplication.articles.ArticleAdapter;
import com.example.myapplication.articles.ArticleRepository;
import com.example.myapplication.articles.articlesDetail.ArticleDetailActivity;
import com.example.myapplication.articles.reports.ReportArticleActivity;
import com.example.myapplication.articles.creation.CreateArticleActivity;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView articleList;
    private ArticleAdapter articleAdapter;
    private ArticleRepository repository;
    private ListenerRegistration articleListener;
    private ArticleLocalDataSource localDataSource;
    private String currentCategoryId;

    public HomeFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        articleList = view.findViewById(R.id.rv_article_list);

        articleAdapter = new ArticleAdapter(
                article -> {
                    Intent intent = new Intent(requireContext(), ArticleDetailActivity.class);
                    intent.putExtra("articleId", article.getId());
                    intent.putExtra("title", article.getTitle());
                    intent.putExtra("authorName", article.getAuthorName());
                    intent.putExtra("categoryId", article.getCategoryId());
                    intent.putExtra("institution", article.getInstitution());
                    intent.putExtra("digitalObjectIdentifier", article.getDigitalObjectIdentifier());
                    intent.putExtra("description", article.getDescription());
                    intent.putExtra("imageUrl", article.getImageUrl());
                    intent.putExtra("sourceUrl", article.getSourceUrl());
                    intent.putExtra("contentUrl", article.getContentUrl());
                    intent.putExtra("createdAt", article.getCreatedAt());
                    startActivity(intent);
                },
                article -> {
                    Intent intent = new Intent(requireContext(), ReportArticleActivity.class);
                    intent.putExtra("articleId", article.getId());
                    intent.putExtra("title", article.getTitle());
                    intent.putExtra("authorName", article.getAuthorName());
                    startActivity(intent);
                },
                categoryId -> loadArticles(categoryId),
                () -> {
                    Intent intent = new Intent(requireContext(), CreateArticleActivity.class);
                    startActivity(intent);
                }
        );

        articleList.setLayoutManager(new LinearLayoutManager(requireContext()));
        articleList.setAdapter(articleAdapter);

        repository = new ArticleRepository();
        localDataSource = new ArticleLocalDataSource(requireContext());

        AppDatabaseHelper databaseHelper = new AppDatabaseHelper(requireContext());
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        Log.d("SQLiteCheck", "Database path: " + db.getPath());
        Log.d("SQLiteCheck", "Database created successfully");

        db.close();
        databaseHelper.close();

        loadArticles(null); // null = all
    }

    private void loadArticles(String categoryId) {
        if (!isAdded() || repository == null || articleAdapter == null || localDataSource == null) {
            return;
        }

        currentCategoryId = categoryId;

        if (articleListener != null) {
            articleListener.remove();
            articleListener = null;
        }

        List<Article> cachedArticles = localDataSource.getHomeArticles();

        Log.d("OfflineCache", "Cached articles count: " + cachedArticles.size());

        if (!cachedArticles.isEmpty()) {
            articleAdapter.setArticles(cachedArticles);
            Log.d("OfflineCache", "Displayed articles from SQLite");
        }

        if (categoryId != null && !categoryId.trim().isEmpty()) {
            cachedArticles.removeIf(article -> !categoryId.equals(article.getCategoryId()));
        }

        if (!cachedArticles.isEmpty()) {
            articleAdapter.setArticles(cachedArticles);
        }

        articleListener = repository.listenApprovedArticles(categoryId, new ArticleRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<Article> articles) {
                if (!isAdded() || articleAdapter == null || localDataSource == null) {
                    return;
                }

                Log.d("OfflineCache", "Firestore loaded: " + articles.size() + " articles");

                localDataSource.saveHomeArticles(articles);
                articleAdapter.setArticles(articles);
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded() || articleAdapter == null || localDataSource == null) {
                    return;
                }

                Log.e("OfflineCache", "Firestore failed; checking SQLite cache", exception);

                List<Article> cachedArticles = localDataSource.getHomeArticles();

                if (!cachedArticles.isEmpty()) {
                    articleAdapter.setArticles(cachedArticles);
                    Toast.makeText(requireContext(), "Showing saved articles offline.", Toast.LENGTH_SHORT).show();
                } else {
                    articleAdapter.setArticles(new ArrayList<>());
                    Toast.makeText(requireContext(), "Unable to load articles.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        removeArticleListener();
        articleList = null;
        articleAdapter = null;
        super.onDestroyView();
    }

    public void removeArticleListener() {
        if (articleListener != null) {
            articleListener.remove();
            articleListener = null;
        }
    }
}