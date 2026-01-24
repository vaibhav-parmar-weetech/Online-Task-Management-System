package com.example.Online_Task_Management_System.entity;

import com.example.Online_Task_Management_System.enums.TaskStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(
        name = "task",
        indexes = {
                @Index(name = "idx_task_due_date", columnList = "dueDate"),
                @Index(name = "idx_task_task_status", columnList = "taskStatus"),
                @Index(name = "inx_task_created_by", columnList = "created_by"),
                @Index(name = "idx_task_status_due_date", columnList = "task_status, due_date")
        }
)
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

    @OneToMany(
            mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<TaskComments> comments = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskFile> files = new ArrayList<>();

    public List<TaskFile> getFiles() {
        return files;
    }

    public void setFiles(List<TaskFile> files) {
        this.files = files;
    }

    public Task(Long id, String title, LocalDateTime dueDate, TaskStatus taskStatus, LocalDateTime createdAt, Set<Users> users, Users createdBy, List<TaskComments> comments, List<TaskFile> files) {
        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.taskStatus = taskStatus;
        this.createdAt = createdAt;
        this.users = users;
        this.createdBy = createdBy;
        this.comments = comments;
        this.files = files;
    }

    public List<TaskComments> getComments() {
        return comments;
    }

    public void setComments(List<TaskComments> comments) {
        this.comments = comments;
    }

    public Users getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Users createdBy) {
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
