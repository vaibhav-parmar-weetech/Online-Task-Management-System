package com.example.Online_Task_Management_System.exception;

import java.time.LocalDateTime;

public record ApiError(
        int status,
        String error,
        String message
) {}
