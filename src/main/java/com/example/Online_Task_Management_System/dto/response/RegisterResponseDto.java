package com.example.Online_Task_Management_System.dto.response;

import com.example.Online_Task_Management_System.enums.Roles;

public class RegisterResponseDto {

    private Long id;
    private String name;
    private String email;
    private Roles roles;

    public RegisterResponseDto(Long id, String name, String email, Roles roles) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.roles = roles;
    }

    public RegisterResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Roles getRoles() {
        return roles;
    }

    public void setRoles(Roles roles) {
        this.roles = roles;
    }
}
