
package com.example.myapplication.qna;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentSnapshot;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.articles.categories.ArticleCategory;
import com.example.myapplication.articles.categories.ArticleCategoryRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateQuestionActivity extends AppCompatActivity {
    private EditText etTitle;
    private EditText etContent;
    private EditText etImageUrl;
    private Spinner spinnerCategory;
    private Button btnSubmit;
    private Button btnCancel;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private ArticleCategoryRepository categoryRepository;
    private final List<ArticleCategory> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_create_question);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        insetsController.setAppearanceLightStatusBars(false);

        View root = findViewById(R.id.root);
        View statusBarSpacer = findViewById(R.id.status_bar_spacer);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            ViewGroup.LayoutParams params =
                    statusBarSpacer.getLayoutParams();

            params.height = bars.top;
            statusBarSpacer.setLayoutParams(params);

            v.setPadding(
                    bars.left,
                    0,
                    bars.right,
                    bars.bottom
            );

            return WindowInsetsCompat.CONSUMED;
        });

        etTitle = findViewById(R.id.et_question_title);
        etContent = findViewById(R.id.et_question_content);
        etImageUrl = findViewById(R.id.et_question_image_url);

        spinnerCategory = findViewById(R.id.spinner_question_category);

        btnSubmit = findViewById(R.id.btn_submit_question);
        btnCancel = findViewById(R.id.btn_cancel_question);

        ImageButton btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        categoryRepository = new ArticleCategoryRepository();
        loadCategories();

        btnSubmit.setOnClickListener(v -> submitQuestion());
    }

    private void loadCategories() {
        categoryRepository.getActiveCategories(
                new ArticleCategoryRepository.OnCategoriesLoadedListener() {

                    @Override
                    public void onSuccess(
                            List<ArticleCategory> loadedCategories) {

                        categories.clear();
                        categories.addAll(loadedCategories);

                        List<String> categoryNames = new ArrayList<>();
                        categoryNames.add("Select a category");

                        for (ArticleCategory category : categories) {
                            categoryNames.add(category.getName());
                        }

                        ArrayAdapter<String> adapter =
                                new ArrayAdapter<>(
                                        CreateQuestionActivity.this,
                                        android.R.layout.simple_spinner_item,
                                        categoryNames
                                );

                        adapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item
                        );

                        spinnerCategory.setAdapter(adapter);
                        spinnerCategory.setSelection(0);
                    }

                    @Override
                    public void onError(Exception exception) {
                        Log.e(
                                "CreateQuestionActivity",
                                "Failed to load categories",
                                exception
                        );

                        Toast.makeText(
                                CreateQuestionActivity.this,
                                "Unable to load categories.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void submitQuestion() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Vui lòng nhập tiêu đề câu hỏi.");
            etTitle.requestFocus();
            return;
        }

        if (content.isEmpty()) {
            etContent.setError("Vui lòng nhập nội dung câu hỏi.");
            etContent.requestFocus();
            return;
        }

        int categoryPosition = spinnerCategory.getSelectedItemPosition();

        if (categoryPosition <= 0 ||
                categoryPosition > categories.size()) {

            Toast.makeText(
                    this,
                    "Please select a category.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ArticleCategory selectedCategory =
                categories.get(categoryPosition - 1);

        String category = selectedCategory.getName();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Vui lòng đăng nhập trước khi đặt câu hỏi.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    saveQuestion(
                            currentUser,
                            userDocument,
                            title,
                            content,
                            imageUrl,
                            selectedCategory
                    );
                })
                .addOnFailureListener(error -> {
                    Log.e(
                            "CreateQuestionActivity",
                            "Failed to load author profile",
                            error
                    );

                    // Nếu không tải được hồ sơ, vẫn thử lưu câu hỏi
                    // bằng thông tin tài khoản Firebase Auth.
                    saveQuestion(
                            currentUser,
                            null,
                            title,
                            content,
                            imageUrl,
                            selectedCategory
                    );
                });
    }
    private void saveQuestion(
            FirebaseUser currentUser,
            DocumentSnapshot userDocument,
            String title,
            String content,
            String imageUrl,
            ArticleCategory selectedCategory
    ) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        String authorName = "";

        if (userDocument != null && userDocument.exists()) {
            String fullName = userDocument.getString("fullName");
            String username = userDocument.getString("username");

            if (fullName != null && !fullName.trim().isEmpty()) {
                authorName = fullName.trim();
            } else if (username != null && !username.trim().isEmpty()) {
                authorName = username.trim();
            }
        }

        if (authorName.isEmpty()
                && currentUser.getDisplayName() != null
                && !currentUser.getDisplayName().trim().isEmpty()) {
            authorName = currentUser.getDisplayName().trim();
        }

        if (authorName.isEmpty()) {
            authorName = "Người dùng";
        }

        String authorAvatar = "";

        if (userDocument != null && userDocument.exists()) {
            String avatar = userDocument.getString("avatar");

            if (avatar != null && !avatar.trim().isEmpty()) {
                authorAvatar = avatar.trim();
            }
        }

        if (authorAvatar.isEmpty() && currentUser.getPhotoUrl() != null) {
            authorAvatar = currentUser.getPhotoUrl().toString();
        }

        Map<String, Object> question = new HashMap<>();

        question.put("title", title);
        question.put("content", content);
        question.put("userId", currentUser.getUid());
        question.put("authorName", authorName);
        question.put("authorAvatar", authorAvatar);
        question.put("status", "OPEN");

        question.put("category", selectedCategory.getName());
        question.put("categoryId", selectedCategory.getId());

        ArrayList<String> selectedCategories = new ArrayList<>();
        selectedCategories.add(selectedCategory.getName());
        question.put("categories", selectedCategories);

        question.put("imageUrl", imageUrl);
        question.put("voteCount", 0);
        question.put("answerCount", 0);
        question.put("reportCount", 0);
        question.put("createdAt", FieldValue.serverTimestamp());
        question.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("questions")
                .add(question)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(
                            this,
                            "Đã gửi câu hỏi thành công!",
                            Toast.LENGTH_LONG
                    ).show();

                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(error -> {
                    Log.e(
                            "CreateQuestionActivity",
                            "Không thể gửi câu hỏi",
                            error
                    );

                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Gửi câu hỏi");

                    Toast.makeText(
                            this,
                            "Gửi câu hỏi thất bại. Vui lòng thử lại.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
