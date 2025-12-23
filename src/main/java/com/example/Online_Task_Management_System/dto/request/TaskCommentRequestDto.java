package com.example.Online_Task_Management_System.dto.request;

public class TaskCommentRequestDto {
    private String comment;

    public TaskCommentRequestDto() {
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public TaskCommentRequestDto(String comment) {
        this.comment = comment;
    }
}
