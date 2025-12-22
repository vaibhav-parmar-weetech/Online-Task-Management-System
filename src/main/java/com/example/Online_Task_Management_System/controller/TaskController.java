package com.example.Online_Task_Management_System.controller;

import com.example.Online_Task_Management_System.dto.request.*;
import com.example.Online_Task_Management_System.dto.response.PageResponse;
import com.example.Online_Task_Management_System.dto.response.TaskResponseDto;
import com.example.Online_Task_Management_System.enums.TaskStatus;
import com.example.Online_Task_Management_System.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController()
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    @Autowired
    TaskService taskService;

    @PostMapping("/add-task")
    public ResponseEntity<?> createTask(@RequestBody TaskRequestDto taskRequestDto){
        return taskService.createTask(taskRequestDto);
    }

    @PutMapping("/editTask/{taskId}")
    public ResponseEntity<?> editTask(@PathVariable Long taskId, @RequestBody EditTaskDto editTaskDto){
        // method updated - 1
        return taskService.editTask(taskId,editTaskDto);
    }

    @DeleteMapping("deleteTask/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId){

        return taskService.deleteTask(taskId);
    }

    @PutMapping("/{taskId}/assign")
    public ResponseEntity<?> assignTask(@PathVariable Long taskId, @RequestBody AssignTaskRequestDto assignTaskRequestDto){
        return taskService.assignTask(taskId,assignTaskRequestDto);
    }

    @GetMapping("view-task/{taskId}")
    public ResponseEntity<?> getTask(@PathVariable Long taskId){
        return taskService.getTask(taskId);
    }

    @GetMapping("v1/view-all-task")
    public ResponseEntity<?> getAllTask(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size){
        return taskService.getAllTask(page,size);
    }

    @GetMapping("v2/view-all-task")
    public ResponseEntity<PageResponse<TaskResponseDto>> filterTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setStatus(status);
        filter.setDueDate(dueDate);
        filter.setAssignedUserId(assignedUserId);

        return taskService.filterdTask(filter,page,size);
    }

    @GetMapping("v1/EMP/view-all-task")
    public ResponseEntity<?> getTasks( @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size){
        return taskService.getMyAllTasks(page,size);
    }

    @GetMapping("v2/EMP/view-all-task")
    public ResponseEntity<PageResponse<TaskResponseDto>> getMyTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate) {

        return ResponseEntity.ok(
                taskService.getMyAllMyTasks(page, size, status, dueDate)
        );
    }

    @GetMapping("my-task/{taskId}")
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId){
        return taskService.getMyTask(taskId);
    }

    @PutMapping("update-task/{taskId}")
    public ResponseEntity<?> updateStatus(@PathVariable Long taskId, @RequestBody TaskUpdateDto taskUpdateDto){
        return taskService.updateTaskStatus(taskId,taskUpdateDto);
    }

}
