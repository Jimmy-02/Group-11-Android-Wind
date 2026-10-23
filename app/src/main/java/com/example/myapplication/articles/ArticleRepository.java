package com.example.myapplication.articles;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;


//LỚP ARCICLE REPORSITORY SẼ Kết hợp Firestore và SQLite giúp cung cấp dữ liệu cho giao diện khi có mnjag hoặc ngắt mạng

public class ArticleRepository {

    private final FirebaseFirestore firestore;

    public ArticleRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void getLatestArticles(OnArticlesLoadedListener listener) {
        firestore.collection("articles")
                .whereEqualTo("status", "APPROVED")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Article> articles = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        if (document.contains("deletedAt")
                                && document.get("deletedAt") != null) {
                            continue;
                        }

                        Article article = document.toObject(Article.class);

                        if (article != null) {
                            article.setId(document.getId());
                            articles.add(article);
                        }
                    }

                    listener.onSuccess(articles);
                })
                .addOnFailureListener(listener::onError);
    }

    public interface OnArticlesLoadedListener {
        void onSuccess(List<Article> articles);

        void onError(Exception exception);
    }
}