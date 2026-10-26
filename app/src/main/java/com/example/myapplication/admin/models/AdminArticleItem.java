package com.example.myapplication.admin.models;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminArticleItem {
    private String id;
    private String title;
    private String authorName;
    private String institution;
    private String category;
    private String categoryId;
    private String description;
    private String contentUrl;
    private String sourceUrl;
    private String doi;
    private String status;
    private int reportCount;
    private String userId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public AdminArticleItem() {
    }

    public AdminArticleItem(String id, String title, String authorName, String institution, String category, String description, String status) {
        this.id = id;
        this.title = title;
        this.authorName = authorName;
        this.institution = institution;
        this.category = category;
        this.description = description;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null && !title.trim().isEmpty() ? title : "Không có tiêu đề bài báo";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthorName() {
        return authorName != null && !authorName.trim().isEmpty() ? authorName : "Tác giả ẩn danh";
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getInstitution() {
        return institution != null && !institution.trim().isEmpty() ? institution : "Đơn vị nghiên cứu";
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getCategory() {
        if (category != null && !category.trim().isEmpty()) return category;
        if (categoryId != null && !categoryId.trim().isEmpty()) return categoryId;
        return "Khoa học chung";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getStatus() {
        return status != null && !status.trim().isEmpty() ? status : "PENDING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(getStatus());
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(getStatus());
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(getStatus());
    }

    public boolean isDeleted() {
        return "DELETED".equalsIgnoreCase(getStatus());
    }

    public String getTimeAgo() {
        if (createdAt == null) return "Vừa xong";
        long diff = System.currentTimeMillis() - createdAt.toDate().getTime();
        long minutes = diff / (1000 * 60);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 7) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(createdAt.toDate());
        }
        if (days > 0) return days + " ngày trước";
        if (hours > 0) return hours + " giờ trước";
        if (minutes > 0) return minutes + " phút trước";
        return "Vừa xong";
    }
}