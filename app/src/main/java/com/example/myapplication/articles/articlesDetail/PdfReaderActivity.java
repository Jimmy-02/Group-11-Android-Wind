package com.example.myapplication.articles.articlesDetail;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
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
import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfReaderActivity extends AppCompatActivity {

    private PDFView pdfViewer;
    private ProgressBar progressPdf;
    private TextView tvPdfTitle;
    private ExecutorService executorService;

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
        executorService = Executors.newSingleThreadExecutor();

        btnBack.setOnClickListener(v -> finish());

        String pdfUrl = getIntent().getStringExtra("contentUrl");
        String articleTitle = getIntent().getStringExtra("articleTitle");

        if (articleTitle != null && !articleTitle.trim().isEmpty()) {
            tvPdfTitle.setText(articleTitle);
        }

        if (pdfUrl == null || pdfUrl.trim().isEmpty()) {
            showError("Không tìm thấy đường dẫn PDF.");
            return;
        }

        if (!isNetworkAvailable()) {
            showError("Thiết bị không có kết nối mạng. Vui lòng kiểm tra Wi-Fi hoặc dữ liệu di động.");
            return;
        }

        loadPdf(pdfUrl);
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

        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void loadPdf(String pdfUrl) {
        progressPdf.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            HttpURLConnection connection = null;
            File pdfFile = null;

            try {
                URL url = new URL(pdfUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Máy chủ trả về mã HTTP " + responseCode);
                }

                pdfFile = File.createTempFile("article_", ".pdf", getCacheDir());

                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(pdfFile)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                if (pdfFile.length() == 0) {
                    throw new Exception("Tệp PDF rỗng.");
                }

                File finalPdfFile = pdfFile;

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        progressPdf.setVisibility(View.GONE);

                        pdfViewer.fromFile(finalPdfFile)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .defaultPage(0)
                                .onError(error -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        showError("Không thể đọc tệp PDF này.");
                                    }
                                })
                                .load();
                    }
                });

            } catch (java.net.SocketTimeoutException e) {
                showErrorOnUiThread("Kết nối quá lâu. Vui lòng thử lại.");

            } catch (java.net.UnknownHostException e) {
                showErrorOnUiThread("Không thể kết nối máy chủ. Hãy kiểm tra mạng Internet.");

            } catch (Exception e) {
                android.util.Log.e("PdfReaderActivity", "Failed to load PDF", e);
                showErrorOnUiThread("Không thể tải bài báo. Vui lòng thử lại.");

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void showErrorOnUiThread(String message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                progressPdf.setVisibility(View.GONE);
                showError(message);
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}