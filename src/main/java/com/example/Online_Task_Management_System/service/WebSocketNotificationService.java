package com.example.Online_Task_Management_System.service;

import com.example.Online_Task_Management_System.dto.request.TaskNotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);


    public void send(TaskNotificationDto dto) {
        log.info("🔔 Broadcasting WS notification: {}", dto.getMessage());
        messagingTemplate.convertAndSend("/topic/notifications", dto);
    }

}

