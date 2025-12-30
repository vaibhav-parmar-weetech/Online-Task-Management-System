package com.example.Online_Task_Management_System.service.Impl;

import com.example.Online_Task_Management_System.dto.response.TaskFileResponseDto;
import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.TaskFile;
import com.example.Online_Task_Management_System.entity.Users;
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
    public ResponseEntity<?> uploadFile(Long taskId, MultipartFile file) {
        Users currentUser = getLoggedInUser();

        Optional<Task> byId = taskRepository.findById(taskId);
        if (byId.isEmpty()) return new ResponseEntity<>(Map.of("status",400,"message","Task Not Found.."),HttpStatus.NOT_FOUND);

        Task task = byId.get();

        boolean isCreator = task.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAssigned = task.getUsers().contains(currentUser);

        if (!isCreator && !isAssigned) {
            return new ResponseEntity<>(Map.of("status",403,"message","You are not assigned to this task"),HttpStatus.FORBIDDEN);
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
                    "File '" + file.getOriginalFilename() +
                            "' uploaded to Task ID: " + task.getId(),
                    "TaskFile",
                    taskFile.getId()
            );

            log.info(
                    "Task File Uploaded | taskId={} | file={} | user={}",
                    taskId, file.getOriginalFilename(), currentUser.getEmail()
            );

            return ResponseEntity.ok(Map.of("status",200,"message", "File uploaded successfully"));

        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("status",500,"message", e.getMessage()));
        }

    }

    @Override
    public ResponseEntity<?> listFiles(Long taskId) {

        Users user = getLoggedInUser();

        Optional<Task> byId = taskRepository.findById(taskId);

        if (byId.isEmpty()) return new ResponseEntity<>(Map.of("status",400,"message","Task Not Found.."),HttpStatus.NOT_FOUND);

        Task task = byId.get();

        boolean isAdmin = user.getRoles().name().contains("Admin");
        boolean isManager = user.getRoles().name().contains("Manager");
        boolean isAssignedEmployee = task.getUsers().contains(user);

        // 🔐 DATA-LEVEL SECURITY
        if (!isAdmin && !isManager && !isAssignedEmployee) {
            return new ResponseEntity<>(Map.of("status",401,"message","You are not allowed to view files of this task"),HttpStatus.UNAUTHORIZED);
        }

        List<TaskFile> files = taskFileRepository.findByTaskId(taskId);

        if (files.isEmpty()) {
            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "File Not Available"
                    )
            );
        }

        List<TaskFileResponseDto> response = files.stream()
                .map(file -> new TaskFileResponseDto(
                        file.getId(),
                        file.getOriginalFileName(),
                        file.getFileType(),
                        file.getUploadedAt()
                ))
                .toList();

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

        Optional<TaskFile> byId = taskFileRepository.findById(fileId);
        if (byId.isEmpty()) return new ResponseEntity<>(Map.of("status",404,"message","file not found"),HttpStatus.NOT_FOUND);

        TaskFile file = byId.get();

        Task task = file.getTask();

        boolean isAdmin = currentUser.getRoles().name().contains("ROLE_Admin");
        boolean isCreator = task.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAssigned = task.getUsers().contains(currentUser);

        if (!isAdmin && !isCreator && !isAssigned) {
            return new ResponseEntity<>(Map.of("status",401,"message","Not allowed to download file"),HttpStatus.UNAUTHORIZED);
        }

        try {
            Path path = Paths.get(file.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getOriginalFileName() + "\"")
                    .contentType(MediaType.parseMediaType(file.getFileType()))
                    .body(resource);

        } catch (MalformedURLException e) {
            return new ResponseEntity<>(Map.of("status",500,"message",e.getMessage()),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteFile(Long fileId) {
        Users currentUser = getLoggedInUser();

        // Admin check (extra safety)
        boolean isAdmin = currentUser.getRoles().name().contains("Admin");
        if (!isAdmin) {
            return new ResponseEntity(Map.of("status",401,"message", "Only admin can delete files"), HttpStatus.UNAUTHORIZED);
        }

        Optional<TaskFile> byId = taskFileRepository.findById(fileId);
        if (byId.isEmpty()) return new ResponseEntity<>(Map.of("status",404,"message","file not found"),HttpStatus.NOT_FOUND);

        TaskFile taskFile = byId.get();

        // Delete physical file
        try {
            Path path = Paths.get(taskFile.getFilePath());
            Files.deleteIfExists(path);
            // 🔐 AUDIT LOG
            auditLogService.log(
                    "TASK_FILE_DELETED",
                    "File '" + taskFile.getOriginalFileName() +
                            "' deleted from Task ID: " + taskFile.getTask().getId(),
                    "TaskFile",
                    taskFile.getId()
            );

            log.info(
                    "Task File Deleted | fileId={} | file={} | user={}",
                    fileId,
                    taskFile.getOriginalFileName(),
                    currentUser.getEmail()
            );

        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("status",500,"message", e.getMessage()));
        }

        // Delete DB record
        taskFileRepository.delete(taskFile);

        return ResponseEntity.ok(
                Map.of("status",200,"message", "File deleted successfully")
        );
    }
}
