package com.example.Online_Task_Management_System.service;

import org.springframework.stereotype.Service;

@Service
public interface AuditLogService {
    void log(String action, String description, String entityName, Long entityId);
}
