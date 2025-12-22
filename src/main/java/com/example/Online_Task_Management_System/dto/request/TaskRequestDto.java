package com.example.Online_Task_Management_System.dto.request;

import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Schema(description = "Task creation request")
public class TaskRequestDto {
    @Schema(example = "Design Database Schema")
    private String title;
    @Schema(example = "2025-01-20 11:00:00")
    private LocalDateTime dueDate;
//    private TaskStatus taskStatus;
    private Set<Long> userIds = new HashSet<>();

    public TaskRequestDto() {
    }

    public TaskRequestDto(String title, LocalDateTime dueDate, Set<Long> userIds) {
        this.title = title;
        this.dueDate = dueDate;
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

    public Set<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(Set<Long> userIds) {
        this.userIds = userIds;
    }
}
