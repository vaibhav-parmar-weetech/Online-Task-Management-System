package com.example.Online_Task_Management_System.dto.request;


import com.example.Online_Task_Management_System.enums.TaskStatus;

public class TaskUpdateDto {
    private TaskStatus taskStatus;

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public TaskUpdateDto() {
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public TaskUpdateDto(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }
}
