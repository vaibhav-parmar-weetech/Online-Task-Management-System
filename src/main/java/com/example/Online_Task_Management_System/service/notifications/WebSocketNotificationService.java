package com.example.Online_Task_Management_System.service.notifications;

import com.example.Online_Task_Management_System.dto.request.TaskNotificationDto;
import com.example.Online_Task_Management_System.service.Impl.TaskServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger log =
            LoggerFactory.getLogger(WebSocketNotificationService.class);

    public void send(String email, TaskNotificationDto dto) {

        log.info("🔔 Sending WS notification to user [{}]: {}",
                email, dto.getMessage());

        messagingTemplate.convertAndSendToUser(
                email,
                "/queue/notifications",
                dto
        );
    }
}


