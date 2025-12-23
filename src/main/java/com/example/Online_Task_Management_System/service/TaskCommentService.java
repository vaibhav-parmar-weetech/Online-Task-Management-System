package com.example.Online_Task_Management_System.service;

import com.example.Online_Task_Management_System.dto.request.TaskCommentRequestDto;
import com.example.Online_Task_Management_System.dto.response.TaskCommentResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TaskCommentService {

    ResponseEntity<?> addComment(Long taskId, TaskCommentRequestDto dto);

    List<TaskCommentResponseDto> getComments(Long taskId);
}
