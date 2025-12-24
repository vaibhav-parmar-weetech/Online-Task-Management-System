package com.example.Online_Task_Management_System.dto.response;

import java.time.LocalDateTime;

public class TaskFileResponseDto {

    private Long id;
    private String originalFileName;
    private String fileType;
    private LocalDateTime uploadedAt;

    public TaskFileResponseDto(Long id, String originalFileName, String fileType, LocalDateTime uploadedAt) {
        this.id = id;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.uploadedAt = uploadedAt;
    }

    public TaskFileResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}