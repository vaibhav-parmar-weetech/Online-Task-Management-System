package com.example.Online_Task_Management_System.repository;

import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.TaskComments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComments,Long> {
    List<TaskComments> findByTaskOrderByCreatedAtAsc(Task task);
}
