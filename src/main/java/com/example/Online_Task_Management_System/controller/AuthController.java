package com.example.Online_Task_Management_System.controller;

import com.example.Online_Task_Management_System.dto.request.ForgotPasswordDto;
import com.example.Online_Task_Management_System.dto.request.LoginRequestDto;
import com.example.Online_Task_Management_System.dto.request.RegisterRequestDto;

import com.example.Online_Task_Management_System.repository.UserRepository;
import com.example.Online_Task_Management_System.service.JwtService;
import com.example.Online_Task_Management_System.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("auth")
@Tag(name = "Auth APIs", description = "Register & Login")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;

    @Autowired
    UserService userService;

    public AuthController(AuthenticationManager authManager,
                          UserRepository userRepository,
                          JwtService jwtService,
                          PasswordEncoder encoder) {
        this.authManager = authManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDto request) {
       return userService.registerUser(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequestDto request) {
       return userService.loginUser(request);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {

        return userService.resetPassword(token, newPassword);
//        return ResponseEntity.ok("Password updated successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        userService.sendResetLink(email);
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("message", "Password Reset Link Sented.."));
    }

    @PostMapping("/resend-verify-email")
    public ResponseEntity<?> resendVerificationEmail(@RequestParam String email) {
        return userService.resendVerificationEmail(email);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        return userService.verifyEmail(token);
    }




}
