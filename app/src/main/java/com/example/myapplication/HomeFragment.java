package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView articleList;
    private ArticleAdapter articleAdapter;
    private ArticleRepository repository;
    private ListenerRegistration articleListener;

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

        loadArticles(null); // null = all
    }

    private void loadArticles(String categoryId) {
        // Không ẩn articleList: header (search + chip category) nằm trong RecyclerView,
        // ẩn đi thì người dùng không đổi category được nữa.
        if (!isAdded() || repository == null || articleAdapter == null) {
            return;
        }

        if (articleListener != null) {
            articleListener.remove();
            articleListener = null;
        }

        articleListener = repository.listenApprovedArticles(categoryId, new ArticleRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<Article> articles) {
                if (!isAdded() || articleAdapter == null) {
                    return;
                }

                // Rỗng thì chỉ trống phần bên dưới "Latest Articles"
                articleAdapter.setArticles(articles);
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) {
                    return;
                }

                android.util.Log.e("HomeFragment", "Failed to load articles", exception);
                Toast.makeText(requireContext(), "Unable to load articles.", Toast.LENGTH_SHORT).show();
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