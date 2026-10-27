package com.example.myapplication.articles;
import com.google.firebase.firestore.Exclude;
public class Article {

    private String id;
    private String title;
    private String authorName;
    private String categoryId;
    private String institution;
    private String digitalObjectIdentifier;
    private String description;
    private String imageUrl;
    private String sourceUrl;
    private String contentUrl;
    private String status;
    private long createdAt;

    public Article() {

    }

    public Article(String id, String title, String authorName, String categoryId, String institution, String digitalObjectIdentifier, String description, String imageUrl, String sourceUrl, String contentUrl) {
        this.id = id;
        this.title = title;
        this.authorName = authorName;
        this.categoryId = categoryId;
        this.institution = institution;
        this.digitalObjectIdentifier = digitalObjectIdentifier;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sourceUrl = sourceUrl;
        this.contentUrl = contentUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDigitalObjectIdentifier() {
        return digitalObjectIdentifier;
    }

    public void setDigitalObjectIdentifier(String digitalObjectIdentifier) {
        this.digitalObjectIdentifier = digitalObjectIdentifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }


    //@Exclude là annotation của Firestore, báo cho Firestore bỏ qua property đó khi chuyển đổi giữa object Java và document:
    //
    //Khi ghi (set(object)): không lưu field đó lên Firestore.
    //Khi đọc (document.toObject(Article.class)): không tự điền giá trị vào field đó.
    @Exclude
    public long getCreatedAt() {
        return createdAt;
    }

    @Exclude
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}