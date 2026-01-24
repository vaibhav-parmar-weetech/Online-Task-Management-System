package com.example.Online_Task_Management_System.service.Impl;

import com.example.Online_Task_Management_System.dto.request.TaskCommentRequestDto;
import com.example.Online_Task_Management_System.dto.response.TaskCommentResponseDto;
import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.TaskComments;
import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.exception.custom.PermissionDeniedException;
import com.example.Online_Task_Management_System.exception.custom.ResourceNotFoundException;
import com.example.Online_Task_Management_System.repository.TaskCommentRepository;
import com.example.Online_Task_Management_System.repository.TaskRepository;
import com.example.Online_Task_Management_System.repository.UserRepository;
import com.example.Online_Task_Management_System.service.AuditLogService;
import com.example.Online_Task_Management_System.service.TaskCommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskCommentServiceImpl implements TaskCommentService {

    @Autowired
    TaskCommentRepository taskCommentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TaskRepository taskRepository;

    private static final Logger log = LoggerFactory.getLogger(TaskComments.class);

    @Autowired
    AuditLogService auditLogService;



    @Override
    public ResponseEntity<?> addComment(Long taskId, TaskCommentRequestDto dto) {
        try {
            Users user = getCurrentUser();

            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

            // ✅ Permission check: user assigned or creator
            boolean allowed = task.getUsers().contains(user) || task.getCreatedBy().equals(user);
            if (!allowed) {
                throw new PermissionDeniedException("You are not assigned to this task");
            }

            // ✅ Create and save comment
            TaskComments comment = new TaskComments();
            comment.setTask(task);
            comment.setUser(user);
            comment.setComment(dto.getComment());

            TaskComments saved = taskCommentRepository.save(comment);

            // ✅ Audit log
            auditLogService.log(
                    "TASK_COMMENT_ADDED",
                    "Comment added to Task ID: " + task.getId(),
                    "TaskComment",
                    saved.getId()
            );

            // ✅ Logging
            log.info(
                    "Task Comment Added | taskId={} | commentId={} | commentedBy={} | roles={}",
                    task.getId(),
                    saved.getId(),
                    saved.getUser().getEmail(),
                    saved.getUser().getRoles().name()
            );

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Comment added successfully"
            ));

        } catch (ResourceNotFoundException | PermissionDeniedException ex) {
            throw ex; // handled by GlobalExceptionHandler
        } catch (Exception ex) {
            log.error("Failed to add comment | taskId={} | userId={}", taskId, getCurrentUser().getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "message", "Failed to add comment"
                    ));
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<TaskCommentResponseDto> getComments(Long taskId) {
        Users user = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        boolean allowed = task.getUsers().contains(user) || task.getCreatedBy().equals(user);
        if (!allowed) {
            throw new PermissionDeniedException("You do not have permission to view comments for this task");
        }

        List<TaskCommentResponseDto> comments = taskCommentRepository
                .findByTaskOrderByCreatedAtAsc(task)
                .stream()
                .map(this::mapToDto)
                .toList();

        log.info("Fetched comments | taskId={} | userId={} | commentsCount={}",
                taskId, user.getId(), comments.size());

        return comments;
    }


    private TaskCommentResponseDto mapToDto(TaskComments comment) {
        TaskCommentResponseDto dto = new TaskCommentResponseDto();
        dto.setId(comment.getId());
        dto.setComment(comment.getComment());
        dto.setCommentedBy(comment.getUser().getEmail());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }

    private Users getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
