package com.example.myapplication.articles.categories;

import com.google.firebase.firestore.PropertyName;

public class ArticleCategory {

    private String id;
    private String name;
    private String slug;
    private String description;
    private String color;
    private String icon;
    private int displayOrder;
    private boolean isActive;

    public ArticleCategory() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }


    //Field trong Firestore tên là isActive, nhưng với getter isActive() và setter setActive(), Firestore sẽ map thành thuộc tính active. Kết quả là isActive trong object luôn là false
    //Thêm @PropertyName để map đúng
    @PropertyName("isActive")
    public boolean isActive() {
        return isActive;
    }

    @PropertyName("isActive")
    public void setActive(boolean active) {
        isActive = active;
    }
}