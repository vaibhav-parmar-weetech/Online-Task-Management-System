package com.example.Online_Task_Management_System.service;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface TaskFileService {
    ResponseEntity<?> uploadFile(Long taskId, MultipartFile file);

    ResponseEntity<?> listFiles(Long taskId);

    ResponseEntity<?> downloadFile(Long fileId);

    ResponseEntity<?> deleteFile(Long fileId);
}
