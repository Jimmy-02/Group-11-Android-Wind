package com.example.myapplication.articles;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ArticleRepository {

    private final FirebaseFirestore firestore;

    public ArticleRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public ListenerRegistration listenApprovedArticles(String categoryId, OnArticlesLoadedListener listener) {
        Query query = firestore.collection("articles").whereEqualTo("status", "APPROVED");

        if (categoryId != null && !categoryId.trim().isEmpty()) {
            query = query.whereEqualTo("categoryId", categoryId);
        }

        query = query.orderBy("createdAt", Query.Direction.DESCENDING);

        return query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }

            if (querySnapshot == null) {
                return;
            }

            List<Article> articles = new ArrayList<>();

            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                if (document.contains("deletedAt") && document.get("deletedAt") != null) {
                    continue;
                }

                Article article = document.toObject(Article.class);

                if (article != null) {
                    article.setId(document.getId());

                    Timestamp timestamp = document.getTimestamp("createdAt");

                    if (timestamp != null) {
                        article.setCreatedAt(timestamp.toDate().getTime());
                    }

                    articles.add(article);
                }
            }

            if (articles.isEmpty() && querySnapshot.getMetadata().isFromCache()) {
                listener.onError(new Exception("Offline: no data from server"));
                return;
            }

            listener.onSuccess(articles);
        });
    }

    public void getLatestArticles(OnArticlesLoadedListener listener) {
        listenApprovedArticles(null, listener);
    }

    public void getArticlesByCategory(String categoryId, OnArticlesLoadedListener listener) {
        listenApprovedArticles(categoryId, listener);
    }

    public interface OnArticlesLoadedListener {
        void onSuccess(List<Article> articles);

        void onError(Exception exception);
    }
}