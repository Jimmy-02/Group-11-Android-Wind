package com.example.myapplication.articles.articlesDetail;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.myapplication.R;
import com.example.myapplication.data.local.OfflinePdfManager;
import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

public class PdfReaderActivity extends AppCompatActivity {

    private static final String TAG = "PdfReaderActivity";

    private PDFView pdfViewer;
    private ProgressBar progressPdf;
    private TextView tvPdfTitle;
    private OfflinePdfManager offlinePdfManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_pdf_reader);

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

        ImageButton btnBack = findViewById(R.id.btn_pdf_back);
        pdfViewer = findViewById(R.id.pdf_viewer);
        progressPdf = findViewById(R.id.progress_pdf);
        tvPdfTitle = findViewById(R.id.tv_pdf_title);

        offlinePdfManager = new OfflinePdfManager(this);

        btnBack.setOnClickListener(v -> finish());

        String articleId = getIntent().getStringExtra("articleId");
        String pdfUrl = getIntent().getStringExtra("contentUrl");
        String articleTitle = getIntent().getStringExtra("articleTitle");

        if (articleTitle != null && !articleTitle.trim().isEmpty()) {
            tvPdfTitle.setText(articleTitle);
        }

        if (articleId == null || articleId.trim().isEmpty()) {
            showError("Không xác định được bài báo. Vui lòng mở lại từ trang chi tiết.");
            return;
        }

        if (pdfUrl == null || pdfUrl.trim().isEmpty()) {
            showError("Không tìm thấy đường dẫn PDF.");
            return;
        }

        openArticlePdf(articleId, pdfUrl);
    }

    private void openArticlePdf(String articleId, String pdfUrl) {
        File offlineFile = offlinePdfManager.getOfflinePdf(articleId);

        if (offlineFile != null) {
            Log.d(TAG, "Opening offline PDF: " + offlineFile.getAbsolutePath());
            displayPdf(offlineFile);
            return;
        }

        if (!isNetworkAvailable()) {
            showError("Bài báo này chưa được lưu để đọc offline. Vui lòng kết nối Internet và bấm Read Article trước.");
            return;
        }

        progressPdf.setVisibility(View.VISIBLE);

        offlinePdfManager.downloadPdf(articleId, pdfUrl, new OfflinePdfManager.PdfCallback() {
            @Override
            public void onSuccess(File pdfFile) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                Log.d(TAG, "PDF downloaded and saved: " + pdfFile.getAbsolutePath());
                displayPdf(pdfFile);
            }

            @Override
            public void onError(Exception exception) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                Log.e(TAG, "Failed to download PDF", exception);
                showError("Không thể tải bài báo. Vui lòng kiểm tra đường dẫn PDF và kết nối mạng.");
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        Network network = manager.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);

        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void displayPdf(File pdfFile) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        progressPdf.setVisibility(View.GONE);

        pdfViewer.fromFile(pdfFile)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .onError(error -> {
                    Log.e(TAG, "Unable to render PDF", error);

                    if (!isFinishing() && !isDestroyed()) {
                        showError("Không thể đọc tệp PDF này.");
                    }
                })
                .load();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        if (pdfViewer != null) {
            pdfViewer.recycle();
        }

        if (offlinePdfManager != null) {
            offlinePdfManager.shutdown();
        }

        super.onDestroy();
    }
}