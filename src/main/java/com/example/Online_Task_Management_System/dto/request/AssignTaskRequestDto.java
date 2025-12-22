package com.example.Online_Task_Management_System.dto.request;

import java.util.Set;

public class AssignTaskRequestDto {

    private Set<Long> userIds;

    public Set<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(Set<Long> userIds) {
        this.userIds = userIds;
    }

    public AssignTaskRequestDto() {
    }

    public AssignTaskRequestDto(Set<Long> userIds) {
        this.userIds = userIds;
    }
}
