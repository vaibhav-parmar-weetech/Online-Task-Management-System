package com.example.Online_Task_Management_System.service.Impl;

import com.example.Online_Task_Management_System.dto.request.*;
import com.example.Online_Task_Management_System.dto.response.PageResponse;
import com.example.Online_Task_Management_System.dto.response.TaskCommentSummaryDto;
import com.example.Online_Task_Management_System.dto.response.TaskResponseDto;
import com.example.Online_Task_Management_System.dto.response.UserSummaryDto;
import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.TaskFile;
import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.enums.Roles;
import com.example.Online_Task_Management_System.enums.TaskStatus;
import com.example.Online_Task_Management_System.repository.TaskCommentRepository;
import com.example.Online_Task_Management_System.repository.TaskFileRepository;
import com.example.Online_Task_Management_System.repository.TaskRepository;
import com.example.Online_Task_Management_System.repository.UserRepository;
import com.example.Online_Task_Management_System.service.AuditLogService;
import com.example.Online_Task_Management_System.service.TaskFileService;
import com.example.Online_Task_Management_System.service.notifications.NotificationService;
import com.example.Online_Task_Management_System.service.TaskService;
import com.example.Online_Task_Management_System.service.notifications.WebSocketNotificationService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    AuditLogService auditLogService;

    @Autowired
    TaskFileRepository taskFileRepository;

    @Autowired
    TaskCommentRepository taskCommentRepository;


    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    NotificationService notificationService;

    @Autowired
    WebSocketNotificationService webSocketNotificationService;

    @Override
    @Transactional
    public ResponseEntity<?> createTask(TaskRequestDto taskRequestDto) {
        try {

            Users loggedInUser = getLoggedInUser();

            if (taskRequestDto.getDueDate().isBefore(LocalDateTime.now())) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "status", 400,
                                "message", "Please enter a valid due date"
                        ));
            }

            if (taskRequestDto.getUserIds() == null || taskRequestDto.getUserIds().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "status", 400,
                                "message", "Please assign users"
                        ));
            }

            Set<Long> requestedUserIds = taskRequestDto.getUserIds();
            List<Users> foundUsers = userRepository.findAllById(requestedUserIds);

            Set<Long> foundUserIds = foundUsers.stream()
                    .map(Users::getId)
                    .collect(Collectors.toSet());

            List<Long> invalidUserIds = requestedUserIds.stream()
                    .filter(id -> !foundUserIds.contains(id))
                    .toList();

            if (!invalidUserIds.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "status", 400,
                                "message", "Invalid user IDs",
                                "invalidUserIds", invalidUserIds
                        ));
            }

            // ✅ Create task
            Task newTask = new Task();
            newTask.setTitle(taskRequestDto.getTitle());
            newTask.setDueDate(taskRequestDto.getDueDate());
            newTask.setTaskStatus(TaskStatus.PENDING);
            newTask.setCreatedBy(loggedInUser);

            Set<Users> users = new HashSet<>(foundUsers);
            users.forEach(user -> user.getTasks().add(newTask));
            newTask.setUsers(users);

            Task savedTask = taskRepository.save(newTask);

            // email notifications
//            notificationService.sendTaskEmail(
//                    savedTask.getCreatedBy(),
//                    "Task Created Successfully",
//                    buildTaskCreatedMessage(savedTask, savedTask.getCreatedBy())
//            );
//
//            // Email to assigned users
//            for (Users user : savedTask.getUsers()) {
//                notificationService.sendTaskEmail(
//                        user,
//                        "You have been assigned a new task",
//                        buildTaskAssignedMessage(savedTask, user)
//                );
//            }

            // ✅ WebSocket notifications
            webSocketNotificationService.send(
                    new TaskNotificationDto(
                            savedTask.getId(),
                            savedTask.getTitle(),
                            "Task created successfully",
                            "CREATED",
                            LocalDateTime.now()
                    )
            );

            for (Users user : savedTask.getUsers()) {
                webSocketNotificationService.send(
                        new TaskNotificationDto(
                                savedTask.getId(),
                                savedTask.getTitle(),
                                "You have been assigned a task",
                                "ASSIGNED",
                                LocalDateTime.now()
                        )
                );
            }

            // ✅ Audit log
            auditLogService.log(
                    "CREATE_TASK",
                    "Task created with title: " + savedTask.getTitle(),
                    "Task",
                    savedTask.getId()
            );

            log.info("Task created successfully | taskId={}", savedTask.getId());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of(
                            "status", 200,
                            "message", "Task created successfully"
                    ));

        } catch (Exception e) {
            log.error("Error creating task", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "message", "Something went wrong"
                    ));
        }
    }




    private Users getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
    }


    @Override
    @Transactional
    public ResponseEntity<?> editTask(Long taskId, EditTaskDto editTaskDto) {
        try {
            Users loggedInUser = getLoggedInUser();

            Task old = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Task Not Found"));

            // Manager restriction
            if (loggedInUser.hasRole(Roles.ROLE_Manager) &&
                    !old.getCreatedBy().getId().equals(loggedInUser.getId())) {

                return new ResponseEntity<>(Map.of("status", 401,
                        "error", "Unauthorized",
                        "message","You are not authorized to access this resource"),HttpStatus.UNAUTHORIZED);
            }

            StringBuilder auditDesc = new StringBuilder("Updated task fields: ");

            // update title
            if (StringUtils.hasText(editTaskDto.getTitle())) {
                auditDesc.append("title (")
                        .append(old.getTitle()).append(" → ")
                        .append(editTaskDto.getTitle()).append("), ");
                old.setTitle(editTaskDto.getTitle());
            }

            // update due date
            if (editTaskDto.getDueDate() != null) {
                if (editTaskDto.getDueDate().isBefore(LocalDateTime.now())) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("status",400,"message", "Please enter a valid due date"));

                }

                auditDesc.append("dueDate (")
                        .append(old.getDueDate()).append(" → ")
                        .append(editTaskDto.getDueDate()).append("), ");
                old.setDueDate(editTaskDto.getDueDate());
            }

            // update status
            if (editTaskDto.getTaskStatus() != null) {
                auditDesc.append("status (")
                        .append(old.getTaskStatus()).append(" → ")
                        .append(editTaskDto.getTaskStatus()).append("), ");
                old.setTaskStatus(editTaskDto.getTaskStatus());
            }

            // re-assign users
            if (editTaskDto.getUserIds() != null && !editTaskDto.getUserIds().isEmpty()) {

                auditDesc.append("assigned users updated, ");

                // remove old relations
                old.getUsers().forEach(u -> u.getTasks().remove(old));
                old.getUsers().clear();

                // assign new users
                Set<Users> users = new HashSet<>(
                        userRepository.findAllById(editTaskDto.getUserIds())
                );

                users.forEach(u -> u.getTasks().add(old));
                old.setUsers(users);
            }

            Task save = taskRepository.save(old);

            webSocketNotificationService.send(
                    new TaskNotificationDto(
                            save.getId(),
                            save.getTitle(),
                            "Task details have been updated. Please review the changes",
                            "UPDATED",
                            LocalDateTime.now()
                    )
            );


            // ✅ AUDIT LOG
            auditLogService.log(
                    "EDIT_TASK",
                    auditDesc.toString(),
                    "Task",
                    old.getId()
            );
            log.info("Task updated | taskId={} | updatedBy={}",
                    taskId, loggedInUser.getEmail());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of("status",200,"message", "Task Updated Successfully.."));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status",500,"message", e.getMessage()));
        }
    }


    @Override
    @Transactional
    public ResponseEntity<?> deleteTask(Long taskId) {

        Users loggedInUser = getLoggedInUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.warn("Task Deletion failed (Task not found) | taskId={}", taskId);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Task Not Found");
                });

        // MANAGER permission check
        if (loggedInUser.hasRole(Roles.ROLE_Manager) &&
                !task.getCreatedBy().getId().equals(loggedInUser.getId())) {

            log.warn("Unauthorized Task Delete | taskId={} | userId={}",
                    taskId, loggedInUser.getId());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", 401,
                            "error", "Unauthorized",
                            "message", "You are not authorized to access this resource"
                    ));
        }

        //  DELETE COMMENTS
        int commentCount = taskCommentRepository.deleteByTaskId(taskId);
        log.info("Deleted {} comments | taskId={}", commentCount, taskId);

        //  DELETE FILES
        List<TaskFile> files = taskFileRepository.findByTaskId(taskId);

        for (TaskFile file : files) {
            try {
                Path path = Paths.get(file.getFilePath());
                Files.deleteIfExists(path);
                log.info("Deleted task file | fileId={} | name={}",
                        file.getId(), file.getOriginalFileName());
            } catch (IOException e) {
                log.error("Failed to delete file from disk | fileId={}", file.getId(), e);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to delete task files");
            }
        }
        taskFileRepository.deleteAll(files);

        task.getUsers().forEach(user -> user.getTasks().remove(task));
        task.getUsers().clear();

        taskRepository.delete(task);

        auditLogService.log(
                "DELETE_TASK",
                "Task deleted with files & comments",
                "Task",
                taskId
        );

        log.info("Task Deleted Successfully | taskId={}", taskId);

        return ResponseEntity.ok(
                Map.of("status", 200, "message", "Task Deleted Successfully"));
    }




    @Override
    public ResponseEntity<?> getTask(Long taskId) {
        try{
            Optional<Task> oldTask = taskRepository.findById(taskId);

            // Checking task is exist or not...
            if(oldTask.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status",404,"message", "Task Not Found.."));
            Task task = oldTask.get();

            // task map to taskresponseDto
            TaskResponseDto taskResponseDto = mapToDto(task);

            return ResponseEntity.ok(taskResponseDto);
        }catch (Exception e){
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status",500,"message", e.getMessage()));
        }
    }

    private TaskResponseDto mapToDto(Task task) {
        TaskResponseDto response = new TaskResponseDto();

        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setTaskStatus(task.getTaskStatus().name());
        response.setDueDate(task.getDueDate());
        response.setCreatedAt(task.getCreatedAt());

        if (task.getCreatedBy() != null) {
            UserSummaryDto createdBy = new UserSummaryDto();
            createdBy.setId(task.getCreatedBy().getId());
            createdBy.setName(task.getCreatedBy().getName());
            createdBy.setEmail(task.getCreatedBy().getEmail());
            response.setCreatedBy(createdBy);
        }

        List<TaskCommentSummaryDto> comments = task.getComments().stream()
                .map(cmt ->{
                    TaskCommentSummaryDto dto = new TaskCommentSummaryDto();
                    dto.setComment(cmt.getComment());
                    dto.setCreatedAt(cmt.getCreatedAt());
                    dto.setId(cmt.getId());
                    return dto;
                }).collect(Collectors.toList());

        Set<UserSummaryDto> users = task.getUsers().stream()
                .map(user -> {
                    UserSummaryDto dto = new UserSummaryDto();
                    dto.setId(user.getId());
                    dto.setName(user.getName());
                    dto.setEmail(user.getEmail());
                    return dto;
                })
                .collect(Collectors.toSet());
        response.setComments(comments);
        response.setAssignedUsers(users);
        return response;
    }


    @Override
    public ResponseEntity<?> getAllTask(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            Page<Task> taskPage = taskRepository.findAll(pageable);

            if (taskPage.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status",404,"message", "Task is not Available.."));
            }

            List<TaskResponseDto> responseDtoList = taskPage.getContent()
                    .stream()
                    .map(task -> {

                        return mapToDto(task);
                    })
                    .toList();

            PageResponse<TaskResponseDto> pageResponse = new PageResponse<>();
            pageResponse.setContent(responseDtoList);
            pageResponse.setCurrentPage(taskPage.getNumber());
            pageResponse.setTotalPages(taskPage.getTotalPages());
            pageResponse.setTotalElements(taskPage.getTotalElements());
            pageResponse.setPageSize(taskPage.getSize());
            pageResponse.setLast(taskPage.isLast());

            return ResponseEntity.ok(pageResponse);
        }catch (Exception e){
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status",500,"message", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> assignTask(Long taskId, AssignTaskRequestDto assignTaskRequestDto) {
        try{
            Users loggedInUser = getLoggedInUser();

            Optional<Task> oldTask = taskRepository.findById(taskId);

            // Checking task is exist or not...
            if(oldTask.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status",404,"message", "Task Not Found.."));
            Task task = oldTask.get();

            if (loggedInUser.hasRole(Roles.valueOf("Manager")) &&
                    !task.getCreatedBy().getId().equals(loggedInUser.getId())) {

                return new ResponseEntity<>(Map.of("status", 401,
                        "error", "Unauthorized",
                        "message","You are not authorized to access this resource"),HttpStatus.UNAUTHORIZED);
            }

            Set<Users> users = new HashSet<>(
                    userRepository.findAllById(assignTaskRequestDto.getUserIds())
            );

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status",404,"message", "No Valid User Found..."));
            }

            // UPDATE BOTH SIDES
            for (Users user : users) {
                user.getTasks().add(task);
            }
            task.setUsers(users);

            // SAVE OWNING SIDE
            userRepository.saveAll(users);

            auditLogService.log(
                    "ASSIGN_TASK",
                    "Task assigned to users: " + assignTaskRequestDto.getUserIds(),
                    "Task",
                    task.getId()
            );
            log.info("Task assigned | taskId={} | assignedUsers={}",
                    taskId, assignTaskRequestDto.getUserIds());


            return ResponseEntity.ok(mapToDto(task));
        }catch (Exception e){
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status",500,"message", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> getMyAllTasks(int page, int size) {
        try{
            Users user = getLoggedInUser();
            Pageable pageable = PageRequest.of(page, size);

            Page<Task> taskPage = taskRepository.findByUsers_Id(user.getId(), pageable);

            if (taskPage.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status",404,"message", "Task Not Found.."));
            }

            List<TaskResponseDto> responseDtoList = taskPage.getContent()
                    .stream()
                    .map(task -> {

                        return mapToDto(task);
                    })
                    .toList();

            PageResponse<TaskResponseDto> pageResponse = new PageResponse<>();
            pageResponse.setContent(responseDtoList);
            pageResponse.setCurrentPage(taskPage.getNumber());
            pageResponse.setTotalPages(taskPage.getTotalPages());
            pageResponse.setTotalElements(taskPage.getTotalElements());
            pageResponse.setPageSize(taskPage.getSize());
            pageResponse.setLast(taskPage.isLast());

            return ResponseEntity.ok(pageResponse);
        }catch (Exception e){
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status",500,"message", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> getMyTask(Long taskId) {
        try{
            Users user = getLoggedInUser();
            Optional<Task> task = taskRepository.findByUsers_IdAndId(user.getId(),taskId);
            if(task.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status",404,"message", "Task Not Found.."));
            Task tk = task.get();
            TaskResponseDto taskResponseDto = mapToDto(tk);
            return ResponseEntity.ok(taskResponseDto);
        }catch (Exception e){
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status",500,"message", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> updateTaskStatus(Long taskId, TaskUpdateDto taskUpdateDto) {
        try{
            Users user = getLoggedInUser();
            Optional<Task> task = taskRepository.findByUsers_IdAndId(user.getId(),taskId);
            if(task.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status",404,"message", "Task Not Found.."));
            Task tk = task.get();
            String oldStatus = tk.getTaskStatus().name();
            tk.setTaskStatus(taskUpdateDto.getTaskStatus());
            Task save = taskRepository.save(tk);

            auditLogService.log(
                    "TASK_STATUS_UPDATE",
                    "Task status changed from " + oldStatus + " to " + save.getTaskStatus().name(),
                    "Task",
                    save.getId()
            );

            log.info("Task Status Updated | taskId={} | UpdatedTaskStatus={}",
                    taskId, taskUpdateDto.getTaskStatus().name());

            return ResponseEntity.status(HttpStatus.OK).body(Map.of("status",200,"message", "Task Updated Successful.."));
        }catch (Exception e){
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status",500,"message", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<PageResponse<TaskResponseDto>> filterdTask(TaskFilterRequest filter, int page, int size) {

        PageResponse<TaskResponseDto> response = new PageResponse<>();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        Page<Task> taskPage = taskRepository.filterTasks(
                filter.getStatus(),
                filter.getDueDate(),
                filter.getAssignedUserId(),
                pageable
        );

        List<TaskResponseDto> data = taskPage.getContent()
                .stream()
                .map(this::mapToDto)
                .toList();

        response.setContent(data);
        response.setCurrentPage(taskPage.getNumber());
        response.setTotalPages(taskPage.getTotalPages());
        response.setTotalElements(taskPage.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @Override
    public PageResponse<TaskResponseDto> getMyAllMyTasks(int page, int size, TaskStatus status, LocalDate dueDate) {
        Users user = getLoggedInUser();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<Task> taskPage = taskRepository.findEmployeeTasks(
                user.getId(),
                status,
                dueDate,
                pageable
        );

        List<TaskResponseDto> responseDtoList = taskPage.getContent()
                .stream()
                .map(this::mapToDto)
                .toList();

        PageResponse<TaskResponseDto> pageResponse = new PageResponse<>();
        pageResponse.setContent(responseDtoList);
        pageResponse.setCurrentPage(taskPage.getNumber());
        pageResponse.setTotalPages(taskPage.getTotalPages());
        pageResponse.setTotalElements(taskPage.getTotalElements());
        pageResponse.setPageSize(taskPage.getSize());
        pageResponse.setLast(taskPage.isLast());

        return pageResponse;
    }


    private String buildTaskCreatedMessage(Task task, Users user) {
        return """
            👋 Hello %s,

            🎉 Great news! Your task has been successfully created.

            📌 Task Details:
            ─────────────────────
            📝 Title     : %s
            ⏰ Due Date  : %s
            📊 Status   : %s
            ─────────────────────

            🚀 You can start working on it anytime. Stay productive!

            Regards,
            ✅ Task Management System
            """.formatted(
                user.getName(),
                task.getTitle(),
                task.getDueDate(),
                task.getTaskStatus()
        );
    }

    private String buildTaskAssignedMessage(Task task, Users user) {
        return """
            👋 Hello %s,

            🆕 You’ve been assigned a new task!

            📌 Task Details:
            ─────────────────────
            📝 Title     : %s
            ⏰ Due Date  : %s
            📊 Status   : %s
            ─────────────────────

            💡 Please review the task and start working on it at your convenience.
            ⏳ Make sure to complete it before the due date.

            Best of luck! 👍

            Regards,
            ✅ Task Management System
            """.formatted(
                user.getName(),
                task.getTitle(),
                task.getDueDate(),
                task.getTaskStatus()
        );
    }
}
