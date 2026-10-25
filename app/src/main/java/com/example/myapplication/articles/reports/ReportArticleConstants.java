package com.example.myapplication.articles.reports;

public final class ReportArticleConstants {

    private ReportArticleConstants() {
    }

    // Report reasom
    public static final String REASON_MISINFORMATION = "Thông tin khoa học sai lệch";
    public static final String REASON_PLAGIARISM = "Đạo văn hoặc vi phạm bản quyền";
    public static final String REASON_FAKE_PUBLICATION = "Bài báo hoặc nguồn xuất bản không xác thực";
    public static final String REASON_IRRELEVANT_CONTENT = "Không liên quan đến khoa học";
    public static final String REASON_INAPPROPRIATE_CONTENT = "Nội dung không phù hợp";
    public static final String REASON_SPAM = "Spam hoặc quảng cáo";
    public static final String REASON_BROKEN_LINK = "Liên kết bài báo bị lỗi";
    public static final String REASON_OTHER = "Lý do khác";

    // Report statu
    public static final String STATUS_PENDING = "PENDING";

    // Validation message
    public static final String ERROR_OTHER_REASON_REQUIRED = "Vui lòng mô tả lý do báo cáo.";

    // Default report details limit
    public static final int MAX_DETAILS_LENGTH = 500;
}