package com.example.Online_Task_Management_System.service.notifications;

import com.example.Online_Task_Management_System.dto.request.TaskNotificationDto;
import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.repository.TaskRepository;
import com.example.Online_Task_Management_System.service.Impl.UserServiceImpl;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskReminderScheduler {

    @Autowired
    TaskRepository taskRepository;

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    NotificationService notificationService;

    @Autowired
    WebSocketNotificationService webSocketNotificationService;

    @Scheduled(cron = "0 0 10 * * *")
    public void reminder24hour() {
        LocalDateTime tomorrow = LocalDate.now().atTime(10, 0).plusDays(1);
        System.out.println(tomorrow);
        List<Task> tasks = taskRepository.findTasksDueTomorrow(tomorrow);
        tasks.forEach(task ->
                log.warn(
                        "[24-HOUR REMINDER] Task '{}' is due Tomorrow at 10:00 AM",
                        task.getTaskStatus()));
    }


    @Scheduled(cron = "0 0 9 * * *")
    public void OnehrReminder() {
        LocalDateTime today = LocalDate.now().atTime(10, 0);
//        System.out.println(today);

        List<Task> tasks = taskRepository.findTasksDueToday(today);

        tasks.forEach(task ->
                log.warn(
                        "[1-HOUR REMINDER] Task '{}' is due today at 10:00 AM",
                        task.getTitle()
                )
        );

    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void taskReminderScheduler() {
        System.out.println(LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();

        // 24-hour
        LocalDateTime start24 = now.plusHours(24).minusSeconds(5);
        LocalDateTime end24 = start24.plusMinutes(5);

        // 1-hour
        LocalDateTime start1 = now.plusHours(1).minusSeconds(5);
        LocalDateTime end1 = start1.plusMinutes(5);

        List<Task> dueIn24Hours =
                taskRepository.findTasksDueIn24Hours(start24, end24);


        for (Task t : dueIn24Hours) {
            send24HourReminder(t);
        }

//        NotifyEmail(dueIn24Hours);
        dueIn24Hours.forEach(task ->
                log.info(
                        "[24-HOUR REMINDER] Task '{}' is due at {}",
                        task.getTitle(),
                        task.getDueDate()
                )
        );

        List<Task> dueIn1Hour =
                taskRepository.findTasksDueIn1Hour(start1, end1);

        for (Task t : dueIn1Hour) {
            send1HourReminder(t);
        }

        dueIn1Hour.forEach(task ->
                log.warn(
                        "[1-HOUR REMINDER] Task '{}' is due at {}",
                        task.getTitle(),
                        task.getDueDate()
                )
        );

    }

    private void NotifyEmail(List<Task> dueIn24Hours) {

    }

    private void send24HourReminder(Task task) {

        for (Users user : task.getUsers()) {

            notificationService.sendTaskEmail(
                    user,
                    "⏳ Task Due in 24 Hours",
                    buildTask24HourReminderMessage(task, user)
            );

            //web socket notifications
            webSocketNotificationService.send(
                    new TaskNotificationDto(
                            task.getId(),
                            task.getTitle(),
                            "Reminder: This task is due in 24 hours",
                            "REMINDER_24H",
                            LocalDateTime.now()
                    )
            );
        }
//
        Users creator = task.getCreatedBy();

        notificationService.sendTaskEmail(
                creator,
                "⏳ Task Due in 24 Hours",
                buildTask24HourReminderMessage(task, creator)
        );
        webSocketNotificationService.send(
                new TaskNotificationDto(
                        task.getId(),
                        task.getTitle(),
                        "Reminder: This task is due in 24 hours",
                        "REMINDER_24H",
                        LocalDateTime.now()
                )
        );

        log.info("[24-HOUR REMINDER EMAIL SENT] Task: {}", task.getTitle());
    }

    private void send1HourReminder(Task task) {

        for (Users user : task.getUsers()) {

            notificationService.sendTaskEmail(
                    user,
                    "⏳ Task Due in 1 Hours",
                    buildTask1HourReminderMessage(task, user)
            );

            webSocketNotificationService.send(
                    new TaskNotificationDto(
                            task.getId(),
                            task.getTitle(),
                            "Urgent reminder: This task is due in 1 hour",
                            "REMINDER_1H",
                            LocalDateTime.now()
                    )
            );

        }

//        // Task creator
        Users creator = task.getCreatedBy();
        notificationService.sendTaskEmail(
                creator,
                "⏳ Task Due in 1 Hours",
                buildTask1HourReminderMessage(task, creator)
        );

        webSocketNotificationService.send(
                new TaskNotificationDto(
                        task.getId(),
                        task.getTitle(),
                        "Urgent reminder: This task is due in 1 hour",
                        "REMINDER_1H",
                        LocalDateTime.now()
                )
        );


        log.info("[1-HOUR REMINDER EMAIL SENT] Task: {}", task.getTitle());
    }


    private String buildTask24HourReminderMessage(Task task, Users user) {
        return """
                👋 Hello %s,
                
                ⏳ Friendly Reminder! Your task is due in **24 hours**.
                
                📌 Task Details:
                ─────────────────────
                📝 Title     : %s
                ⏰ Due Date  : %s
                📊 Status   : %s
                ─────────────────────
                
                🛠 Please make sure all preparations are on track.
                ✅ Completing tasks on time helps keep everything running smoothly.
                
                💪 You’ve got this!
                
                Regards,
                ✅ Task Management System
                """.formatted(
                user.getName(),
                task.getTitle(),
                task.getDueDate(),
                task.getTaskStatus()
        );
    }

    private String buildTask1HourReminderMessage(Task task, Users user) {
        return """
                👋 Hello %s,
                
                🚨 Urgent Reminder! Your task is due in **1 hour**.
                
                📌 Task Details:
                ─────────────────────
                📝 Title     : %s
                ⏰ Due Date  : %s
                📊 Status   : %s
                ─────────────────────
                
                ⏱ Please prioritize this task to avoid missing the deadline.
                🔔 If already completed, kindly ignore this message.
                
                🚀 Stay focused and finish strong!
                
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
