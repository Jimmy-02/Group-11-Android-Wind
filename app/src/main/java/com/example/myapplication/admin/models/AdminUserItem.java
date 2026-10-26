package com.example.myapplication.admin.models;

import com.google.firebase.Timestamp;

public class AdminUserItem {
    private String uid;
    private String username;
    private String fullName;
    private String email;
    private String avatar;
    private String role;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public AdminUserItem() {
    }

    public AdminUserItem(String uid, String username, String fullName, String email, String avatar, String role, String status) {
        this.uid = uid;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.avatar = avatar;
        this.role = role;
        this.status = status;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName != null && !fullName.trim().isEmpty() ? fullName : (username != null ? username : "Người dùng");
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRole() {
        return role != null ? role : "USER";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isBanned() {
        return "BANNED".equalsIgnoreCase(this.status);
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
}