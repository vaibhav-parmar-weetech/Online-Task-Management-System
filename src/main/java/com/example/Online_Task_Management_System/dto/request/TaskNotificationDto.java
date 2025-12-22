package com.example.Online_Task_Management_System.dto.request;

import java.time.LocalDateTime;

public class TaskNotificationDto {

    private Long taskId;
    private String title;
    private String message;
    private String type;   // CREATED, UPDATED, ASSIGNED, STATUS_CHANGED, COMMENT
    private LocalDateTime timestamp;

    public TaskNotificationDto() {
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TaskNotificationDto(Long taskId, String title, String message, String type, LocalDateTime timestamp) {
        this.taskId = taskId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
    }
}
