package com.example.Online_Task_Management_System.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskCommentSummaryDto {
    Long id;
    String comment;
    LocalDateTime createdAt;

    public TaskCommentSummaryDto() {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TaskCommentSummaryDto(Long id, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.comment = comment;
        this.createdAt = createdAt;
    }
}
