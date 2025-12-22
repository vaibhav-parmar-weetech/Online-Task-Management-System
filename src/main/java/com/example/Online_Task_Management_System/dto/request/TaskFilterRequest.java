package com.example.Online_Task_Management_System.dto.request;

import com.example.Online_Task_Management_System.enums.TaskStatus;

import java.time.LocalDate;

public class TaskFilterRequest {
    private TaskStatus status;
    private LocalDate dueDate;
    private Long assignedUserId;

    public TaskFilterRequest() {
    }

    public TaskFilterRequest(TaskStatus status, LocalDate dueDate, Long assignedUserId) {
        this.status = status;
        this.dueDate = dueDate;
        this.assignedUserId = assignedUserId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }
}
