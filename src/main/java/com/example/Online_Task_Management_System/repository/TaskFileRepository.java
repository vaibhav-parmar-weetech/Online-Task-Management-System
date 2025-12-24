package com.example.Online_Task_Management_System.repository;

import com.example.Online_Task_Management_System.entity.TaskFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskFileRepository extends JpaRepository<TaskFile,Long> {
    List<TaskFile> findByTaskId(Long taskId);
}
