package com.example.Online_Task_Management_System.service.Impl;

import com.example.Online_Task_Management_System.dto.request.TaskCommentRequestDto;
import com.example.Online_Task_Management_System.dto.response.TaskCommentResponseDto;
import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.TaskComments;
import com.example.Online_Task_Management_System.entity.Users;
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
        Users user = getCurrentUser();

        Optional<Task> byId = taskRepository.findById(taskId);
        if (byId.isEmpty()) return new ResponseEntity<>(Map.of("status",400,"message","Task Not Found.."),HttpStatus.NOT_FOUND);

        Task task = byId.get();
        boolean allowed = task.getUsers().contains(user) || task.getCreatedBy().equals(user);

        if (!allowed) {
            return new ResponseEntity<>(Map.of("status",403,"message","You are not assigned to this task"),HttpStatus.FORBIDDEN);
        }

        TaskComments comment = new TaskComments();
        comment.setTask(task);
        comment.setUser(user);
        comment.setComment(dto.getComment());

        TaskComments saved = taskCommentRepository.save(comment);

        auditLogService.log(
                "TASK_COMMENT_ADDED",
                "Comment added to Task ID: " + task.getId(),
                "TaskComment",
                saved.getId()
        );

        log.info(
                "Task Comment Added | taskId={} | commentId={} | commentedBy={} | role={}",
                task.getId(),
                saved.getId(),
                saved.getUser().getEmail(),
                saved.getUser().getRoles().name()
        );

        return ResponseEntity.ok(Map.of("status",200,"message","Comment Added Successfully.."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskCommentResponseDto> getComments(Long taskId) {

        Users user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Task not found"));

        boolean allowed =
                task.getUsers().contains(user)
                        || task.getCreatedBy().equals(user);

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied");
        }

        return taskCommentRepository.findByTaskOrderByCreatedAtAsc(task)
                .stream()
                .map(this::mapToDto)
                .toList();
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
