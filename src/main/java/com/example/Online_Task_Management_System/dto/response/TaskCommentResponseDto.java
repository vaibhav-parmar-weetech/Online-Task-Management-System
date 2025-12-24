package com.example.Online_Task_Management_System.dto.response;

import java.time.LocalDateTime;

public class TaskCommentResponseDto {

    private Long id;
    private String comment;
    private String commentedBy;
    private LocalDateTime createdAt;

    public TaskCommentResponseDto(Long id, String comment, String commentedBy, LocalDateTime createdAt) {
        this.id = id;
        this.comment = comment;
        this.commentedBy = commentedBy;
        this.createdAt = createdAt;
    }

    public TaskCommentResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCommentedBy() {
        return commentedBy;
    }

    public void setCommentedBy(String commentedBy) {
        this.commentedBy = commentedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
