package com.example.Online_Task_Management_System.service.Impl;

import com.example.Online_Task_Management_System.dto.response.PageResponse;
import com.example.Online_Task_Management_System.entity.AuditLog;
import com.example.Online_Task_Management_System.repository.AuditLogRepository;
import com.example.Online_Task_Management_System.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService {

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

    @Override
    public ResponseEntity<PageResponse<AuditLog>> viewLogs(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> logs = auditLogRepository.findAll(pageable);


        PageResponse<AuditLog> response = new PageResponse<>();

        response.setContent(logs.getContent());
        response.setCurrentPage(logs.getNumber());
        response.setTotalPages(logs.getTotalPages());
        response.setTotalElements(logs.getTotalElements());
        response.setPageSize(logs.getSize());
        response.setLast(logs.isLast());
        return ResponseEntity.ok(response);
    }
}
