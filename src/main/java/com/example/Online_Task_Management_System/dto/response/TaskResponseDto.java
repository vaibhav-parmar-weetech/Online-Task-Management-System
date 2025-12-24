package com.example.Online_Task_Management_System.dto.response;


import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class TaskResponseDto {
    private Long id;
    private String title;
    private LocalDateTime dueDate;
    private String taskStatus;
    private LocalDateTime createdAt;
    private UserSummaryDto createdBy;

    private Set<UserSummaryDto> assignedUsers;

    private List<TaskCommentSummaryDto> comments;

    public List<TaskCommentSummaryDto> getComments() {
        return comments;
    }

    public void setComments(List<TaskCommentSummaryDto> comments) {
        this.comments = comments;
    }

    public UserSummaryDto getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserSummaryDto createdBy) {
        this.createdBy = createdBy;
    }

    public TaskResponseDto(Long id, String title, LocalDateTime dueDate, String taskStatus, LocalDateTime createdAt, UserSummaryDto createdBy, Set<UserSummaryDto> assignedUsers, List<TaskCommentSummaryDto> comments) {
        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.taskStatus = taskStatus;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.assignedUsers = assignedUsers;
        this.comments = comments;
    }

    public TaskResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<UserSummaryDto> getAssignedUsers() {
        return assignedUsers;
    }

    public void setAssignedUsers(Set<UserSummaryDto> assignedUsers) {
        this.assignedUsers = assignedUsers;
    }
}
