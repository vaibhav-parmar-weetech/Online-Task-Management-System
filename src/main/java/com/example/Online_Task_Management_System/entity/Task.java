package com.example.Online_Task_Management_System.entity;

import com.example.Online_Task_Management_System.enums.TaskStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDateTime dueDate;

    @Enumerated(value = EnumType.STRING)
    private TaskStatus taskStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "tasks")
    private Set<Users> users = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Users createdBy;

    public Users getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Users createdBy) {
        this.createdBy = createdBy;
    }


    public Task(Long id, String title, LocalDateTime dueDate, TaskStatus taskStatus, LocalDateTime createdAt, Set<Users> users, Users createdBy) {
        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.taskStatus = taskStatus;
        this.createdAt = createdAt;
        this.users = users;
        this.createdBy = createdBy;
    }

    public Task() {
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

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<Users> getUsers() {
        return users;
    }

    public void setUsers(Set<Users> users) {
        this.users = users;
    }
}
