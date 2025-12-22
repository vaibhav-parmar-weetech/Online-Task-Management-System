package com.example.Online_Task_Management_System.controller;

import com.example.Online_Task_Management_System.dto.request.ForgotPasswordDto;
import com.example.Online_Task_Management_System.dto.request.UpdateProfileReqDto;
import com.example.Online_Task_Management_System.dto.response.PageResponse;
import com.example.Online_Task_Management_System.dto.response.ProfileResponseDto;
import com.example.Online_Task_Management_System.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/users")
@SecurityRequirement(name = "bearerAuth")
public class UsersController {

    @Autowired
    UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        return userService.getProfile();
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileReqDto reqDto)  {
        return userService.updateProfile(reqDto);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> forget_pass(@RequestBody ForgotPasswordDto forgotPasswordDto) {
        return userService.forgotPass(forgotPasswordDto);
    }

    @GetMapping("/all-users")
    public ResponseEntity<PageResponse<ProfileResponseDto>> getAllUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return userService.getAllUser(page, size, sortBy, direction);
    }

    @GetMapping("/all-employee")
    public ResponseEntity<?> getAllEmployee( @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size){
        return userService.getAllEmployee(page,size);
    }

    @GetMapping("/all-manager")
    public ResponseEntity<?> getAllmanager( @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size){
        return userService.getAllManager(page,size);
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        return userService.deleteUser(userId);
    }

}
