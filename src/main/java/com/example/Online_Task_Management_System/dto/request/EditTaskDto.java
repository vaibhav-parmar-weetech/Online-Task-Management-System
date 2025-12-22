package com.example.Online_Task_Management_System.dto.request;

import com.example.Online_Task_Management_System.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public class EditTaskDto {
    private String title;

    private LocalDateTime dueDate;

    private TaskStatus taskStatus;

    private Set<Long> userIds; // re-assign users (optional)

    public EditTaskDto(String title, LocalDateTime dueDate, TaskStatus taskStatus, Set<Long> userIds) {
        this.title = title;
        this.dueDate = dueDate;
        this.taskStatus = taskStatus;
        this.userIds = userIds;
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

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public Set<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(Set<Long> userIds) {
        this.userIds = userIds;
    }

    public EditTaskDto() {
    }
}
