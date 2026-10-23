package com.example.myapplication.admin.models;

public class UserReportDetail {
    private String reportId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String reason;
    private String detailReason;
    private String timeAgo;

    public UserReportDetail() {}

    public UserReportDetail(String reportId, String userId, String userName, String userAvatar, String reason, String detailReason, String timeAgo) {
        this.reportId = reportId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.reason = reason;
        this.detailReason = detailReason;
        this.timeAgo = timeAgo;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetailReason() {
        return detailReason;
    }

    public void setDetailReason(String detailReason) {
        this.detailReason = detailReason;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }
}
