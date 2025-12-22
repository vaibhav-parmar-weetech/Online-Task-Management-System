package com.example.Online_Task_Management_System.service;

import com.example.Online_Task_Management_System.dto.request.*;
import com.example.Online_Task_Management_System.dto.response.PageResponse;
import com.example.Online_Task_Management_System.dto.response.TaskResponseDto;
import com.example.Online_Task_Management_System.enums.TaskStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public interface TaskService {
    ResponseEntity<?> createTask(TaskRequestDto taskRequestDto);

    ResponseEntity<?> editTask(Long taskId, EditTaskDto editTaskDto);

    ResponseEntity<?> deleteTask(Long taskId);

    ResponseEntity<?> getTask(Long taskId);

    ResponseEntity<?> getAllTask(int page, int size);

    ResponseEntity<?> assignTask(Long taskId, AssignTaskRequestDto assignTaskRequestDto);

    ResponseEntity<?> getMyAllTasks(int page, int size);

    ResponseEntity<?> getMyTask(Long taskId);

    ResponseEntity<?> updateTaskStatus(Long taskId, TaskUpdateDto taskUpdateDto);

    ResponseEntity<PageResponse<TaskResponseDto>> filterdTask(TaskFilterRequest filter, int page, int size);

    PageResponse<TaskResponseDto> getMyAllMyTasks(int page, int size, TaskStatus status, LocalDate dueDate);
}
