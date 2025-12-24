package com.example.Online_Task_Management_System.service;

import com.example.Online_Task_Management_System.dto.request.ForgotPasswordDto;
import com.example.Online_Task_Management_System.dto.request.LoginRequestDto;
import com.example.Online_Task_Management_System.dto.request.RegisterRequestDto;
import com.example.Online_Task_Management_System.dto.request.UpdateProfileReqDto;
import com.example.Online_Task_Management_System.dto.response.PageResponse;
import com.example.Online_Task_Management_System.dto.response.ProfileResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    ResponseEntity<?> registerUser(RegisterRequestDto user);

    ResponseEntity<?> loginUser(LoginRequestDto userDetails);

    ResponseEntity<?> getProfile();

    ResponseEntity<?> updateProfile(UpdateProfileReqDto reqDto);

    ResponseEntity<PageResponse<ProfileResponseDto>> getAllUser(int page, int size, String sortBy, String direction);

    ResponseEntity<?> deleteUser(Long userId);

    void sendResetLink(String email);

    ResponseEntity<?> resetPassword(String token, String newPassword);

    ResponseEntity<?> forgotPass(ForgotPasswordDto forgotPasswordDto);

    ResponseEntity<?> getAllEmployee(int page, int size);

    ResponseEntity<?> getAllManager(int page, int size);

    ResponseEntity<?> verifyEmail(String token);

    ResponseEntity<?> resendVerificationEmail(String email);
}
