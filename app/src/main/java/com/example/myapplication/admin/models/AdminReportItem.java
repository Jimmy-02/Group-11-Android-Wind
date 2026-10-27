package com.example.myapplication.admin.models;

import java.util.ArrayList;
import java.util.List;

public class AdminReportItem {

    private String id;
    private String contentId;
    private String contentType; // "QUESTION" or "ANSWER" or "DISCUSSION" or "COMMENT" or "ARTICLE"
    private String title;
    private String content;
    private String parentQuestionTitle; // Dùng cho Answer hoặc Comment
    private String status; // "PENDING", "REPORTED_CONTENT_DELETED", "DISMISSED"
    private int reportCount;
    private String timeAgo;
    private String authorName;
    private String authorAvatar;
    private String imageUrl;
    private List<String> tags;

    public AdminReportItem() {
        this.tags = new ArrayList<>();
    }

    public AdminReportItem(String id, String contentId, String contentType, String title,
                           String content, String parentQuestionTitle, String status,
                           int reportCount, String timeAgo, String authorName,
                           String authorAvatar, List<String> tags) {
        this.id = id;
        this.contentId = contentId;
        this.contentType = contentType;
        this.title = title;
        this.content = content;
        this.parentQuestionTitle = parentQuestionTitle;
        this.status = status;
        this.reportCount = reportCount;
        this.timeAgo = timeAgo;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getParentQuestionTitle() { return parentQuestionTitle; }
    public void setParentQuestionTitle(String parentQuestionTitle) { this.parentQuestionTitle = parentQuestionTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
