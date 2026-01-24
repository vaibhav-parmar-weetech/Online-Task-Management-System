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
import com.example.Online_Task_Management_System.exception.custom.BadRequestException;
import com.example.Online_Task_Management_System.exception.custom.FileStorageException;
import com.example.Online_Task_Management_System.exception.custom.ResourceNotFoundException;
import com.example.Online_Task_Management_System.exception.custom.UnauthorizedException;
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
                throw new BadRequestException("Please enter a valid due date");
            }

            if (taskRequestDto.getUserIds() == null || taskRequestDto.getUserIds().isEmpty()) {
                throw new BadRequestException("Please assign users");
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
                throw new BadRequestException("Invalid user IDs :"+invalidUserIds);
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
                    savedTask.getCreatedBy().getEmail(),new TaskNotificationDto(
                            savedTask.getId(),
                            savedTask.getTitle(),
                            "Task created successfully",
                            "CREATED",
                            LocalDateTime.now()
                    )
            );

            for (Users user : savedTask.getUsers()) {
                webSocketNotificationService.send(
                        user.getEmail(),new TaskNotificationDto(
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
            throw new RuntimeException(e.getMessage());
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

            Optional<Task> oldTask = taskRepository.findById(taskId);

            // Checking task is exist or not...
            if(oldTask.isEmpty()) throw new ResourceNotFoundException("Task Not Found");
            Task old = oldTask.get();

            // Manager restriction
            if (loggedInUser.hasRole(Roles.ROLE_Manager) &&
                    !old.getCreatedBy().getId().equals(loggedInUser.getId())) {
                throw new UnauthorizedException("You are not authorized to access this resource");
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
                    throw new BadRequestException("Please enter a valid future due date");

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
                    save.getCreatedBy().getEmail(),new TaskNotificationDto(
                            save.getId(),
                            save.getTitle(),
                            "Task details have been updated. Please review the changes",
                            "UPDATED",
                            LocalDateTime.now()
                    )
            );

            for (Users user : save.getUsers()) {
                if (!user.getId().equals(loggedInUser.getId())) {
                    webSocketNotificationService.send(
                            user.getEmail(),
                            new TaskNotificationDto(
                                    save.getId(),
                                    save.getTitle(),
                                    "Task details have been updated",
                                    "UPDATED",
                                    LocalDateTime.now()
                            )
                    );
                }
            }

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

        }catch (UnauthorizedException | ResourceNotFoundException e){
            throw e;
        }catch (Exception e) {
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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found with id: " + taskId));

        // MANAGER permission check
        if (loggedInUser.hasRole(Roles.ROLE_Manager) &&
                !task.getCreatedBy().getId().equals(loggedInUser.getId())) {

            log.warn("Unauthorized Task Delete | taskId={} | userId={}",
                    taskId, loggedInUser.getId());

            throw new UnauthorizedException(
                    "You are not authorized to delete this task");
        }

        // DELETE COMMENTS
        int commentCount = taskCommentRepository.deleteByTaskId(taskId);
        log.info("Deleted {} comments | taskId={}", commentCount, taskId);

        // DELETE FILES (disk + DB)
        List<TaskFile> files = taskFileRepository.findByTaskId(taskId);

        for (TaskFile file : files) {
            try {
                Path path = Paths.get(file.getFilePath());
                Files.deleteIfExists(path);

                log.info("Deleted task file | fileId={} | name={}",
                        file.getId(), file.getOriginalFileName());

            } catch (IOException e) {
                throw new FileStorageException(
                        "Failed to delete task files from disk", e);
            }
        }
        taskFileRepository.deleteAll(files);

        // REMOVE USER RELATIONS
        task.getUsers().forEach(user -> user.getTasks().remove(task));
        task.getUsers().clear();

        // DELETE TASK
        taskRepository.delete(task);

        // AUDIT LOG
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
            if(oldTask.isEmpty()) throw new ResourceNotFoundException("task not found.");
            Task task = oldTask.get();

            // task map to taskresponseDto
            TaskResponseDto taskResponseDto = mapToDto(task);

            return ResponseEntity.ok(taskResponseDto);
        }catch (ResourceNotFoundException e){
            throw e;
        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
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
            if (page < 0 || size <= 0) {
                throw new BadRequestException("Page must be >= 0 and size must be > 0");
            }

            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by("createdAt").descending()
            );

            Page<Task> taskPage = taskRepository.findAll(pageable);

            if (taskPage.isEmpty()) {
                throw new ResourceNotFoundException("No tasks available");
            }

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

            log.info("Fetched tasks | page={} | size={} | totalElements={}",
                    page, size, taskPage.getTotalElements());

            return ResponseEntity.ok(pageResponse);

        }
        catch (BadRequestException | ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch tasks | page={} | size={}",
                    page, size, ex);

            throw new RuntimeException("Failed to fetch tasks");
        }
    }


    @Override
    @Transactional
    public ResponseEntity<?> assignTask(Long taskId, AssignTaskRequestDto dto) {

        Users loggedInUser = getLoggedInUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found with id: " + taskId));

        if (loggedInUser.hasRole(Roles.ROLE_Manager) &&
                !task.getCreatedBy().getId().equals(loggedInUser.getId())) {

            log.warn("Unauthorized Task Assign | taskId={} | userId={}",
                    taskId, loggedInUser.getId());

            throw new UnauthorizedException(
                    "You are not authorized to assign this task");
        }

        Set<Users> users = new HashSet<>(
                userRepository.findAllById(dto.getUserIds())
        );

        if (users.isEmpty()) {
            throw new ResourceNotFoundException("No valid users found for assignment");
        }

        for (Users user : users) {
            user.getTasks().add(task);
        }
        task.setUsers(users);
        taskRepository.save(task);

        // SEND NOTIFICATIONS
        users.forEach(user ->
                webSocketNotificationService.send(
                        user.getEmail(),
                        new TaskNotificationDto(
                                task.getId(),
                                task.getTitle(),
                                "You have been assigned to a task",
                                "ASSIGNED",
                                LocalDateTime.now()
                        )
                )
        );

        // AUDIT LOG
        auditLogService.log(
                "ASSIGN_TASK",
                "Task assigned to users: " + dto.getUserIds(),
                "Task",
                task.getId()
        );

        log.info("Task assigned successfully | taskId={} | assignedUsers={}",
                taskId, dto.getUserIds());

        return ResponseEntity.ok(mapToDto(task));
    }


    @Override
    public ResponseEntity<?> getMyAllTasks(int page, int size) {
        try {
            if (page < 0 || size <= 0) {
                throw new BadRequestException("Page must be >= 0 and size must be > 0");
            }

            Users user = getLoggedInUser();
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            Page<Task> taskPage = taskRepository.findByUsers_Id(user.getId(), pageable);

            if (taskPage.isEmpty()) {
                throw new ResourceNotFoundException("No tasks found for the current user");
            }

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

            log.info("Fetched tasks for user | userId={} | page={} | size={} | totalTasks={}",
                    user.getId(), page, size, taskPage.getTotalElements());

            return ResponseEntity.ok(pageResponse);

        } catch (BadRequestException | ResourceNotFoundException ex) {
            throw ex;
        }catch (Exception ex) {
            log.error("Failed to fetch tasks for user | page={} | size={}", page, size, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "message", "Failed to fetch tasks"
                    ));
        }
    }


    @Override
    public ResponseEntity<?> getMyTask(Long taskId) {
        try {
            Users user = getLoggedInUser();

            Task task = taskRepository.findByUsers_IdAndId(user.getId(), taskId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Task not found with id: " + taskId));

            TaskResponseDto taskResponseDto = mapToDto(task);

            log.info("Fetched task | taskId={} | userId={}", taskId, user.getId());

            return ResponseEntity.ok(taskResponseDto);

        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch task | taskId={} | userId={}", taskId, getLoggedInUser().getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "message", "Failed to fetch task"
                    ));
        }
    }


    @Override
    public ResponseEntity<?> updateTaskStatus(Long taskId, TaskUpdateDto taskUpdateDto) {
        try {
            Users user = getLoggedInUser();

            Task task = taskRepository.findByUsers_IdAndId(user.getId(), taskId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Task not found with id: " + taskId));

            String oldStatus = task.getTaskStatus().name();
            task.setTaskStatus(taskUpdateDto.getTaskStatus());
            Task savedTask = taskRepository.save(task);

            webSocketNotificationService.send(
                    task.getCreatedBy().getEmail(),
                    new TaskNotificationDto(
                            task.getId(),
                            task.getTitle(),
                            "Task status changed to " + task.getTaskStatus(),
                            "UPDATED",
                            LocalDateTime.now()
                    )
            );

            for (Users u : task.getUsers()) {
                if (!u.getId().equals(user.getId())) { // optional: skip current user if desired
                    webSocketNotificationService.send(
                            u.getEmail(),
                            new TaskNotificationDto(
                                    task.getId(),
                                    task.getTitle(),
                                    "Task status updated to " + task.getTaskStatus(),
                                    "UPDATED",
                                    LocalDateTime.now()
                            )
                    );
                }
            }

            auditLogService.log(
                    "TASK_STATUS_UPDATE",
                    "Task status changed from " + oldStatus + " to " + savedTask.getTaskStatus().name(),
                    "Task",
                    savedTask.getId()
            );

            log.info("Task status updated | taskId={} | oldStatus={} | newStatus={}",
                    taskId, oldStatus, taskUpdateDto.getTaskStatus().name());

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Task status updated successfully"
            ));

        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to update task status | taskId={} | userId={}", taskId, getLoggedInUser().getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "message", "Failed to update task status"
                    ));
        }
    }


    @Override
    public ResponseEntity<PageResponse<TaskResponseDto>> filterdTask(TaskFilterRequest filter, int page, int size) {
        try {
            if (page < 0 || size <= 0) {
                throw new BadRequestException("Page must be >= 0 and size must be > 0");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

            Page<Task> taskPage = taskRepository.filterTasks(filter.getStatus(), filter.getDueDate(), filter.getAssignedUserId(), pageable);

            if (taskPage.isEmpty()) {
                throw new ResourceNotFoundException("No tasks found for given filter criteria");
            }

            List<TaskResponseDto> data = taskPage.getContent()
                    .stream()
                    .map(this::mapToDto)
                    .toList();

            PageResponse<TaskResponseDto> response = new PageResponse<>();
            response.setContent(data);
            response.setCurrentPage(taskPage.getNumber());
            response.setTotalPages(taskPage.getTotalPages());
            response.setTotalElements(taskPage.getTotalElements());
            response.setPageSize(taskPage.getSize());
            response.setLast(taskPage.isLast());

            log.info(
                    "Filtered tasks fetched | page={} | size={} | totalElements={} | filter={}",
                    page, size, taskPage.getTotalElements(), filter
            );

            return ResponseEntity.ok(response);

        } catch (BadRequestException | ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(
                    "Failed to filter tasks | page={} | size={} | filter={}",
                    page, size, filter, ex
            );
            throw new RuntimeException("Failed to fetch filtered tasks");
        }
    }


    @Override
    public PageResponse<TaskResponseDto> getMyAllMyTasks(
            int page,
            int size,
            TaskStatus status,
            LocalDate dueDate) {

        if (page < 0 || size <= 0) {
            throw new BadRequestException("Page must be >= 0 and size must be > 0");
        }

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

        if (taskPage.isEmpty()) {
            throw new ResourceNotFoundException("No tasks found for the current user with given filters");
        }

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

        log.info(
                "Fetched filtered tasks for user | userId={} | page={} | size={} | status={} | dueDate={} | totalTasks={}",
                user.getId(), page, size, status, dueDate, taskPage.getTotalElements()
        );
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
