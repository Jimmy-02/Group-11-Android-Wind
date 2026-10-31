package com.example.myapplication.articles.creation;

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
import com.example.myapplication.articles.ArticleConstant;
import com.example.myapplication.articles.categories.ArticleCategory;
import com.example.myapplication.articles.categories.ArticleCategoryRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateArticleActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etAuthorName;
    private EditText etInstitution;
    private EditText etDescription;
    private EditText etDoi;
    private EditText etSourceUrl;
    private EditText etContentUrl;
    private EditText etImageUrl;

    private Spinner spinnerCategory;
    private Button btnSubmitArticle;

    private ArticleCategoryRepository categoryRepository;
    private final List<ArticleCategory> categories = new ArrayList<>();

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_create_article);

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

        etTitle = findViewById(R.id.et_article_title);
        etAuthorName = findViewById(R.id.et_author_name);
        etInstitution = findViewById(R.id.et_institution);
        etDescription = findViewById(R.id.et_description);
        etDoi = findViewById(R.id.et_doi);
        etSourceUrl = findViewById(R.id.et_source_url);
        etContentUrl = findViewById(R.id.et_content_url);
        etImageUrl = findViewById(R.id.et_image_url);

        spinnerCategory = findViewById(R.id.spinner_category);
        btnSubmitArticle = findViewById(R.id.btn_submit_article);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        categoryRepository = new ArticleCategoryRepository();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        btnSubmitArticle.setOnClickListener(v -> submitArticle());

        loadCategories();
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

                ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateArticleActivity.this, android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);
                spinnerCategory.setSelection(0);
            }

            @Override
            public void onError(Exception exception) {
                Log.e("CreateArticleActivity", "Failed to load categories", exception);
                Toast.makeText(CreateArticleActivity.this, "Unable to load categories.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitArticle() {
        String title = etTitle.getText().toString().trim();
        String authorName = etAuthorName.getText().toString().trim();
        String institution = etInstitution.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String doi = etDoi.getText().toString().trim();
        String sourceUrl = etSourceUrl.getText().toString().trim();
        String contentUrl = etContentUrl.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();

// Validate title
        if (title.isEmpty()) {
            etTitle.setError("Please enter the article title.");
            etTitle.requestFocus();
            return;
        }

        if (title.length() < ArticleConstant.MIN_TITLE_LENGTH) {
            etTitle.setError("Title must contain at least 5 characters.");
            etTitle.requestFocus();
            return;
        }

        if (title.length() > ArticleConstant.MAX_TITLE_LENGTH) {
            etTitle.setError("Title cannot exceed 200 characters.");
            etTitle.requestFocus();
            return;
        }

        // Validate author name
        if (authorName.isEmpty()) {
            etAuthorName.setError("Please enter the author name.");
            etAuthorName.requestFocus();
            return;
        }

        if (authorName.length() < ArticleConstant.MIN_AUTHOR_NAME_LENGTH) {
            etAuthorName.setError("Author name must contain at least 2 characters.");
            etAuthorName.requestFocus();
            return;
        }

        if (authorName.length() > ArticleConstant.MAX_AUTHOR_NAME_LENGTH) {
            etAuthorName.setError("Author name cannot exceed 150 characters.");
            etAuthorName.requestFocus();
            return;
        }

        // Validate institution
        if (institution.isEmpty()) {
            etInstitution.setError("Please enter the institution.");
            etInstitution.requestFocus();
            return;
        }

        if (institution.length() < ArticleConstant.MIN_INSTITUTION_LENGTH) {
            etInstitution.setError("Institution must contain at least 2 characters.");
            etInstitution.requestFocus();
            return;
        }

        if (institution.length() > ArticleConstant.MAX_INSTITUTION_LENGTH) {
            etInstitution.setError("Institution cannot exceed 200 characters.");
            etInstitution.requestFocus();
            return;
        }

        // Validate description
        if (description.isEmpty()) {
            etDescription.setError("Please enter the article description.");
            etDescription.requestFocus();
            return;
        }

        if (description.length() < ArticleConstant.MIN_DESCRIPTION_LENGTH) {
            etDescription.setError("Description must contain at least 20 characters.");
            etDescription.requestFocus();
            return;
        }

        if (description.length() > ArticleConstant.MAX_DESCRIPTION_LENGTH) {
            etDescription.setError("Description cannot exceed 2000 characters.");
            etDescription.requestFocus();
            return;
        }

        // Validate DOI
        if (doi.length() > ArticleConstant.MAX_DOI_LENGTH) {
            etDoi.setError("DOI cannot exceed 255 characters.");
            etDoi.requestFocus();
            return;
        }

        // Validate category
        int categoryPosition = spinnerCategory.getSelectedItemPosition();

        if (categoryPosition <= 0 || categoryPosition > categories.size()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate optional URLs
        if (!isValidOptionalUrl(sourceUrl, "Source URL") ||
                !isValidOptionalUrl(contentUrl, "Article Content URL") ||
                !isValidOptionalUrl(imageUrl, "Image URL")) {
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please log in before submitting an article.", Toast.LENGTH_LONG).show();
            return;
        }

        ArticleCategory selectedCategory = categories.get(categoryPosition - 1);

        Map<String, Object> article = new HashMap<>();
        article.put("title", title);
        article.put("authorName", authorName);
        article.put("categoryId", selectedCategory.getId());
        article.put("institution", institution);
        article.put("digitalObjectIdentifier", doi);
        article.put("description", description);
        article.put("imageUrl", imageUrl);
        article.put("sourceUrl", sourceUrl);
        article.put("contentUrl", contentUrl);
        article.put("userId", currentUser.getUid());
        article.put("status", "PENDING");
        article.put("reportCount", 0);
        article.put("createdAt", FieldValue.serverTimestamp());
        article.put("updatedAt", FieldValue.serverTimestamp());

        btnSubmitArticle.setEnabled(false);
        btnSubmitArticle.setText("Submitting...");

        firestore.collection("articles")
                .add(article)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Article submitted successfully! Please wait for admin approval.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(exception -> {
                    Log.e("CreateArticleActivity", "Failed to submit article", exception);
                    btnSubmitArticle.setEnabled(true);
                    btnSubmitArticle.setText("Submit Article");
                    Toast.makeText(this, "Failed to submit article. Please try again.", Toast.LENGTH_LONG).show();
                });
    }

    private boolean isValidOptionalUrl(String url, String fieldName) {
        if (url.isEmpty()) {
            return true;
        }

        if (!Patterns.WEB_URL.matcher(url).matches() || !(url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(this, fieldName + " must be a valid HTTP or HTTPS URL.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}