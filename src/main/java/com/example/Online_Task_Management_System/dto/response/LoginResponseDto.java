package com.example.Online_Task_Management_System.dto.response;


public class LoginResponseDto {
    String token;
    String msg;

    public LoginResponseDto(String token, String msg) {
        this.token = token;
        this.msg = msg;
    }

    public LoginResponseDto() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
