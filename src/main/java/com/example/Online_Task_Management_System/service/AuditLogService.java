package com.example.Online_Task_Management_System.service;

import com.example.Online_Task_Management_System.dto.response.PageResponse;
import com.example.Online_Task_Management_System.entity.AuditLog;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface AuditLogService {
    void log(String action, String description, String entityName, Long entityId);

    ResponseEntity<PageResponse<AuditLog>> viewLogs(int page, int size);
}
