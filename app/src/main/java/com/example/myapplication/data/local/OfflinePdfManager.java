package com.example.myapplication.data.local;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OfflinePdfManager {

    private static final String TAG = "OfflinePdfManager";

    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface PdfCallback {
        void onSuccess(File pdfFile);
        void onError(Exception exception);
    }

    public OfflinePdfManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public File getOfflinePdf(String articleId) {
        if (articleId == null || articleId.trim().isEmpty()) {
            return null;
        }

        File pdfFile = getPdfFile(articleId);
        return pdfFile.exists() && pdfFile.length() > 0 ? pdfFile : null;
    }

    public void downloadPdf(String articleId, String pdfUrl, PdfCallback callback) {
        if (articleId == null || articleId.trim().isEmpty()
                || pdfUrl == null || pdfUrl.trim().isEmpty()) {
            postError(callback, new IllegalArgumentException("Article ID or PDF URL is missing."));
            return;
        }

        executorService.execute(() -> {
            HttpURLConnection connection = null;
            File tempFile = null;

            try {
                URL url = new URL(pdfUrl.trim());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                int responseCode = connection.getResponseCode();

                if (responseCode < 200 || responseCode >= 300) {
                    throw new Exception("Unable to download PDF. HTTP " + responseCode);
                }

                String contentType = connection.getContentType();

                if (contentType != null
                        && !contentType.toLowerCase(java.util.Locale.ROOT).contains("pdf")
                        && !contentType.toLowerCase(java.util.Locale.ROOT).contains("octet-stream")) {
                    throw new Exception("The provided URL does not return a PDF file.");
                }

                File pdfFile = getPdfFile(articleId);
                File parent = pdfFile.getParentFile();

                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new Exception("Unable to create PDF storage directory.");
                }

                tempFile = new File(parent, pdfFile.getName() + ".tmp");

                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.flush();
                }

                if (tempFile.length() == 0) {
                    throw new Exception("Downloaded PDF is empty.");
                }

                if (pdfFile.exists() && !pdfFile.delete()) {
                    throw new Exception("Unable to replace the previous PDF file.");
                }

                if (!tempFile.renameTo(pdfFile)) {
                    throw new Exception("Unable to save the downloaded PDF.");
                }

                Log.d(TAG, "PDF saved: " + pdfFile.getAbsolutePath());

                File finalPdfFile = pdfFile;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(finalPdfFile);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "PDF download failed", e);

                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }

                postError(callback, e);

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private File getPdfFile(String articleId) {
        String safeFileName = articleId.replaceAll("[^a-zA-Z0-9_-]", "_");
        File directory = new File(context.getFilesDir(), "offline_pdfs");
        return new File(directory, safeFileName + ".pdf");
    }

    private void postError(PdfCallback callback, Exception exception) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(exception);
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}