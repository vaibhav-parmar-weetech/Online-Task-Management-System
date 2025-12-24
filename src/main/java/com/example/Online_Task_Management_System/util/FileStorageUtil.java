package com.example.Online_Task_Management_System.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class FileStorageUtil {

    private static final String BASE_DIR = "uploads/tasks";

    public String saveFile(Long taskId, MultipartFile file) throws IOException {

        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String storedName = UUID.randomUUID() + extension;

        Path taskDir = Paths.get(BASE_DIR, taskId.toString());
        Files.createDirectories(taskDir);

        Path filePath = taskDir.resolve(storedName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING); // file creation on Uploads folder

        return filePath.toString();
    }
}
