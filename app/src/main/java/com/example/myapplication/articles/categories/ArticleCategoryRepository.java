package com.example.myapplication.articles.categories;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ArticleCategoryRepository {

    private final FirebaseFirestore firestore;

    public ArticleCategoryRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void getActiveCategories(OnCategoriesLoadedListener listener) {
        firestore.collection("articleCategories")
                .whereEqualTo("isActive", true)
                .orderBy("displayOrder", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ArticleCategory> categories = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        ArticleCategory category = document.toObject(ArticleCategory.class);

                        if (category != null) {
                            category.setId(document.getId());
                            categories.add(category);
                        }
                    }

                    listener.onSuccess(categories);
                }).addOnFailureListener(listener::onError);
    }

    public interface OnCategoriesLoadedListener {
        void onSuccess(List<ArticleCategory> categories);
        void onError(Exception exception);
    }
}