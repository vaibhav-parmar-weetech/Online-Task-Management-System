package com.example.Online_Task_Management_System.service;

import com.example.Online_Task_Management_System.entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private EmailService emailService;

    public void sendTaskEmail(
            Users user,
            String subject,
            String message
    ) {
        emailService.sendEmail(
                "vaibhav@weetechsolution.com", // user.getEmail();
                subject,
                message
        );
    }

}
