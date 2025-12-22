package com.example.Online_Task_Management_System.repository;

import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.enums.TaskStatus;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task,Long> {
    Page<Task> findByUsers_Id(Long id, Pageable pageable);

    Optional<Task> findByUsers_IdAndId(Long id, Long taskId);

    @Query("""
   SELECT DISTINCT t FROM Task t
   LEFT JOIN t.users u
   WHERE (:status IS NULL OR t.taskStatus = :status)
     AND (:dueDate IS NULL OR t.dueDate = :dueDate)
     AND (:assignedUserId IS NULL OR u.id = :assignedUserId)
        """)
    Page<Task> filterTasks(
            @Param("status") TaskStatus status,
            @Param("dueDate") LocalDate dueDate,
            @Param("assignedUserId") Long assignedUserId,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT t FROM Task t
    JOIN t.users u
    WHERE u.id = :userId
     AND (:status IS NULL OR t.taskStatus = :status)
     AND (:dueDate IS NULL OR t.dueDate = :dueDate)
""")
    Page<Task> findEmployeeTasks(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            @Param("dueDate") LocalDate dueDate,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT *
        FROM task
        WHERE due_date = :today
        AND task_status <> 'COMPELETED'
        """,
            nativeQuery = true
    )
    List<Task> findTasksDueToday(@Param("today") LocalDateTime today);

    @Query(
            value = """
        SELECT *
        FROM task
        WHERE due_date = :tomorrow
        AND task_status <> 'COMPELETED'
        """,
            nativeQuery = true
    )
    List<Task> findTasksDueTomorrow(@Param("tomorrow") LocalDateTime tomorrow);


    @Query("""
    SELECT t
    FROM Task t
    WHERE t.dueDate BETWEEN :start AND :end
      AND t.taskStatus <> 'COMPELETED'
""")
    List<Task> findTasksDueIn1Hour(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    @Query("""
    SELECT t
    FROM Task t
    WHERE t.dueDate BETWEEN :start AND :end
      AND t.taskStatus <> 'COMPELETED'
""")
    List<Task> findTasksDueIn24Hours(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    List<Task> findByCreatedBy(Users user);
}
