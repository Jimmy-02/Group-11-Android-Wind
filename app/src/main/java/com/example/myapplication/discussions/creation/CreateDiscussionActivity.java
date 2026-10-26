package com.example.myapplication.discussions.creation;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.myapplication.R;
import com.example.myapplication.articles.Article;
import com.example.myapplication.articles.categories.ArticleCategory;
import com.example.myapplication.articles.categories.ArticleCategoryRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateDiscussionActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etContent;
    private EditText etImageUrl;

    private Spinner spinnerCategory;
    private Spinner spinnerRelatedArticle;

    private Button btnCreateDiscussion;
    private Button btnCancelDiscussion;

    private ArticleCategoryRepository categoryRepository;
    private final List<ArticleCategory> categories = new ArrayList<>();
    private final List<Article> articles = new ArrayList<>();

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_create_discussion);

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.root);
        View statusBarSpacer = findViewById(R.id.status_bar_spacer);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.LayoutParams params = statusBarSpacer.getLayoutParams();
            params.height = bars.top;
            statusBarSpacer.setLayoutParams(params);
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        etTitle = findViewById(R.id.et_discussion_title);
        etContent = findViewById(R.id.et_discussion_content);
        etImageUrl = findViewById(R.id.et_discussion_image_url);

        spinnerCategory = findViewById(R.id.spinner_discussion_category);
        spinnerRelatedArticle = findViewById(R.id.spinner_related_article);

        btnCreateDiscussion = findViewById(R.id.btn_create_discussion);
        btnCancelDiscussion = findViewById(R.id.btn_cancel_discussion);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        categoryRepository = new ArticleCategoryRepository();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        btnCancelDiscussion.setOnClickListener(v -> finish());
        btnCreateDiscussion.setOnClickListener(v -> createDiscussion());

        loadCategories();
        loadApprovedArticles();
    }

    private void loadCategories() {
        categoryRepository.getActiveCategories(new ArticleCategoryRepository.OnCategoriesLoadedListener() {
            @Override
            public void onSuccess(List<ArticleCategory> loadedCategories) {
                categories.clear();
                categories.addAll(loadedCategories);

                List<String> categoryNames = new ArrayList<>();
                categoryNames.add("Select a category");

                for (ArticleCategory category : categories) {
                    categoryNames.add(category.getName());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateDiscussionActivity.this, android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);
                spinnerCategory.setSelection(0);
            }

            @Override
            public void onError(Exception exception) {
                Log.e("CreateDiscussionActivity", "Failed to load categories", exception);
                Toast.makeText(CreateDiscussionActivity.this, "Unable to load categories.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadApprovedArticles() {
        firestore.collection("articles")
                .whereEqualTo("status", "APPROVED")
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    articles.clear();

                    List<String> articleTitles = new ArrayList<>();
                    articleTitles.add("No related article");

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Article article = document.toObject(Article.class);

                        if (article != null) {
                            article.setId(document.getId());
                            articles.add(article);
                            articleTitles.add(article.getTitle());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateDiscussionActivity.this, android.R.layout.simple_spinner_item, articleTitles);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerRelatedArticle.setAdapter(adapter);
                    spinnerRelatedArticle.setSelection(0);
                })
                .addOnFailureListener(exception -> {
                    Log.e("CreateDiscussionActivity", "Failed to load approved articles", exception);
                    Toast.makeText(CreateDiscussionActivity.this, "Unable to load approved articles.", Toast.LENGTH_SHORT).show();
                });
    }

    private void createDiscussion() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Please enter the discussion title.");
            etTitle.requestFocus();
            return;
        }

        if (content.isEmpty()) {
            etContent.setError("Please enter the discussion content.");
            etContent.requestFocus();
            return;
        }

        int categoryPosition = spinnerCategory.getSelectedItemPosition();

        if (categoryPosition <= 0 || categoryPosition > categories.size()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!imageUrl.isEmpty() && (!Patterns.WEB_URL.matcher(imageUrl).matches() || !(imageUrl.startsWith("https://") || imageUrl.startsWith("http://")))) {
            etImageUrl.setError("Please enter a valid HTTP or HTTPS URL.");
            etImageUrl.requestFocus();
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please log in before creating a discussion.", Toast.LENGTH_LONG).show();
            return;
        }

        ArticleCategory selectedCategory = categories.get(categoryPosition - 1);

        String relatedArticleId = "";
        int articlePosition = spinnerRelatedArticle.getSelectedItemPosition();

        if (articlePosition > 0 && articlePosition <= articles.size()) {
            relatedArticleId = articles.get(articlePosition - 1).getId();
        }

        btnCreateDiscussion.setEnabled(false);
        btnCreateDiscussion.setText("Creating...");

        String finalRelatedArticleId = relatedArticleId;

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> saveDiscussion(currentUser, userDocument, title, content, imageUrl, selectedCategory, finalRelatedArticleId))
                .addOnFailureListener(exception -> {
                    Log.e("CreateDiscussionActivity", "Failed to load author profile, using auth profile", exception);
                    saveDiscussion(currentUser, null, title, content, imageUrl, selectedCategory, finalRelatedArticleId);
                });
    }

    private void saveDiscussion(FirebaseUser currentUser, DocumentSnapshot userDocument, String title, String content, String imageUrl, ArticleCategory selectedCategory, String relatedArticleId) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Map<String, Object> discussion = new HashMap<>();
        discussion.put("title", title);
        discussion.put("content", content);
        discussion.put("category", selectedCategory.getName());
        discussion.put("categoryId", selectedCategory.getId());
        discussion.put("articleId", relatedArticleId);
        discussion.put("imageUrl", imageUrl);
        discussion.put("userId", currentUser.getUid());
        discussion.put("authorName", resolveAuthorName(currentUser, userDocument));
        discussion.put("authorAvatar", resolveAuthorAvatar(currentUser, userDocument));
        discussion.put("status", "ACTIVE");
        discussion.put("likeCount", 0);
        discussion.put("commentCount", 0);
        discussion.put("createdAt", FieldValue.serverTimestamp());
        discussion.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("discussions")
                .add(discussion)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Discussion created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(exception -> {
                    Log.e("CreateDiscussionActivity", "Failed to create discussion", exception);
                    btnCreateDiscussion.setEnabled(true);
                    btnCreateDiscussion.setText("Create Discussion");
                    Toast.makeText(this, "Failed to create discussion. Please try again.", Toast.LENGTH_LONG).show();
                });
    }

    // Thứ tự ưu tiên: fullName -> username -> tên hiển thị của tài khoản Firebase -> "User"
    private String resolveAuthorName(FirebaseUser currentUser, DocumentSnapshot userDocument) {
        if (userDocument != null && userDocument.exists()) {
            String fullName = userDocument.getString("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();

            String username = userDocument.getString("username");
            if (username != null && !username.trim().isEmpty()) return username.trim();
        }

        String displayName = currentUser.getDisplayName();
        if (displayName != null && !displayName.trim().isEmpty()) return displayName.trim();

        return "User";
    }

    // Không có avatar thì lưu chuỗi rỗng, phần hiển thị sẽ dùng ảnh mặc định
    private String resolveAuthorAvatar(FirebaseUser currentUser, DocumentSnapshot userDocument) {
        if (userDocument != null && userDocument.exists()) {
            String avatar = userDocument.getString("avatar");
            if (avatar != null && !avatar.trim().isEmpty()) return avatar.trim();
        }

        if (currentUser.getPhotoUrl() != null) return currentUser.getPhotoUrl().toString();

        return "";
    }
}