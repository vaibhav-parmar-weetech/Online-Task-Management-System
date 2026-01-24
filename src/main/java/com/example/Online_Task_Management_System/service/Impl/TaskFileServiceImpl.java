package com.example.Online_Task_Management_System.service.Impl;

import com.example.Online_Task_Management_System.dto.response.TaskFileResponseDto;
import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.TaskFile;
import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.exception.custom.PermissionDeniedException;
import com.example.Online_Task_Management_System.exception.custom.ResourceNotFoundException;
import com.example.Online_Task_Management_System.repository.AuditLogRepository;
import com.example.Online_Task_Management_System.repository.TaskFileRepository;
import com.example.Online_Task_Management_System.repository.TaskRepository;
import com.example.Online_Task_Management_System.repository.UserRepository;
import com.example.Online_Task_Management_System.service.AuditLogService;
import com.example.Online_Task_Management_System.service.TaskFileService;
import com.example.Online_Task_Management_System.util.FileStorageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskFileServiceImpl implements TaskFileService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskFileRepository taskFileRepository;

    @Autowired
    AuditLogService auditLogService;

    @Autowired
    FileStorageUtil fileStorageUtil;

    private static final Logger log = LoggerFactory.getLogger(TaskFile.class);



    @Override
    @Transactional
    public ResponseEntity<?> uploadFile(Long taskId, MultipartFile file) {
        Users currentUser = getLoggedInUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        boolean isCreator = task.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAssigned = task.getUsers().contains(currentUser);

        if (!isCreator && !isAssigned) {
            throw new PermissionDeniedException("You are not assigned to this task");
        }

        try {
            String filePath = fileStorageUtil.saveFile(taskId, file);

            TaskFile taskFile = new TaskFile();
            taskFile.setOriginalFileName(file.getOriginalFilename());
            taskFile.setStoredFileName(new File(filePath).getName());
            taskFile.setFileType(file.getContentType());
            taskFile.setFilePath(filePath);
            taskFile.setTask(task);
            taskFile.setUploadedBy(currentUser);

            taskFileRepository.save(taskFile);

            auditLogService.log(
                    "TASK_FILE_UPLOADED",
                    "File '" + file.getOriginalFilename() + "' uploaded to Task ID: " + task.getId(),
                    "TaskFile",
                    taskFile.getId()
            );

            log.info("Task File Uploaded | taskId={} | file={} | user={}",
                    taskId, file.getOriginalFilename(), currentUser.getEmail());

            return ResponseEntity.ok(Map.of("status", 200, "message", "File uploaded successfully"));

        } catch (IOException ex) {
            log.error("Failed to upload file | taskId={} | user={} | file={}",
                    taskId, currentUser.getEmail(), file.getOriginalFilename(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "message", "Failed to upload file: " + ex.getMessage()
                    ));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> listFiles(Long taskId) {

        Users user = getLoggedInUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        boolean isAdmin = user.getRoles().name().contains("Admin");
        boolean isManager = user.getRoles().name().contains("Manager");
        boolean isAssignedEmployee = task.getUsers().contains(user);

        if (!isAdmin && !isManager && !isAssignedEmployee) {
            throw new PermissionDeniedException("You are not allowed to view files of this task");
        }

        List<TaskFile> files = taskFileRepository.findByTaskId(taskId);

        if (files.isEmpty()) {
            log.info("No files found | taskId={} | userId={}", taskId, user.getId());
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "No files available for this task"
            ));
        }

        List<TaskFileResponseDto> response = files.stream()
                .map(file -> new TaskFileResponseDto(
                        file.getId(),
                        file.getOriginalFileName(),
                        file.getFileType(),
                        file.getUploadedAt()
                ))
                .toList();

        log.info("Listed files | taskId={} | filesCount={} | userId={}", taskId, response.size(), user.getId());

        return ResponseEntity.ok(response);
    }



    private Users getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadFile(Long fileId) {

        Users currentUser = getLoggedInUser();

        TaskFile file = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        Task task = file.getTask();

        boolean isAdmin = currentUser.getRoles().name().contains("ROLE_Admin");
        boolean isCreator = task.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAssigned = task.getUsers().contains(currentUser);

        if (!isAdmin && !isCreator && !isAssigned) {
            throw new PermissionDeniedException("You are not allowed to download this file");
        }

        try {
            Path path = Paths.get(file.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            log.info("File download initiated | fileId={} | taskId={} | userId={}",
                    fileId, task.getId(), currentUser.getId());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getOriginalFileName() + "\"")
                    .contentType(MediaType.parseMediaType(file.getFileType()))
                    .body(resource);

        } catch (MalformedURLException ex) {
            log.error("Failed to load file | fileId={} | userId={}", fileId, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "message", "Failed to load file: " + ex.getMessage()
                    ));
        }
    }


    @Override
    @Transactional
    public ResponseEntity<?> deleteFile(Long fileId) {

        Users currentUser = getLoggedInUser();

        boolean isAdmin = currentUser.getRoles().name().contains("Admin");
        if (!isAdmin) {
            throw new PermissionDeniedException("Only admin can delete files");
        }

        TaskFile taskFile = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        try {
            Path path = Paths.get(taskFile.getFilePath());
            Files.deleteIfExists(path);

            log.info("Task File Deleted | fileId={} | file={} | user={}",
                    fileId, taskFile.getOriginalFileName(), currentUser.getEmail());

            auditLogService.log(
                    "TASK_FILE_DELETED",
                    "File '" + taskFile.getOriginalFileName() + "' deleted from Task ID: " + taskFile.getTask().getId(),
                    "TaskFile",
                    taskFile.getId()
            );

        } catch (IOException ex) {
            log.error("Failed to delete file from disk | fileId={} | user={}", fileId, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "message", "Failed to delete file: " + ex.getMessage()
                    ));
        }

        taskFileRepository.delete(taskFile);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "File deleted successfully"
        ));
    }

}
