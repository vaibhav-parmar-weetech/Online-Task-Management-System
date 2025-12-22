package com.example.Online_Task_Management_System.service;

import com.example.Online_Task_Management_System.entity.AuditLog;
import com.example.Online_Task_Management_System.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService{

    @Autowired
    CustomUserDetailsService userDetailsService;
    @Autowired
    AuditLogRepository auditLogRepository;

    @Override
    public void log(String action, String description, String entityName, Long entityId) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setDescription(description);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setPerformedBy(userDetailsService.getCurrentUserEmail());

        auditLogRepository.save(log);
    }
}
