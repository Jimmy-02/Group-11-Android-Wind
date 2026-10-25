package com.example.myapplication.discussions;

import com.google.firebase.firestore.Exclude;

public class Discussion {

    private String id;
    private String title;
    private String content;
    private String category;
    private String articleId;
    private String imageUrl;
    private String userId;
    private String status;
    private long createdAt;
    private long updatedAt;
    private long likeCount;
    private long commentCount;

    public Discussion() {
    }

    public Discussion(String id, String title, String content, String category, String articleId, String imageUrl, String userId, String status) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.articleId = articleId;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.status = status;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Exclude
    public long getCreatedAt() {
        return createdAt;
    }

    @Exclude
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Exclude
    public long getUpdatedAt() {
        return updatedAt;
    }

    @Exclude
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }
}