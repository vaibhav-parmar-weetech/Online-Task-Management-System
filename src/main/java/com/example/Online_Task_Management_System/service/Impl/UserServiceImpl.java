package com.example.Online_Task_Management_System.service.Impl;


import com.example.Online_Task_Management_System.dto.request.ForgotPasswordDto;
import com.example.Online_Task_Management_System.dto.request.LoginRequestDto;
import com.example.Online_Task_Management_System.dto.request.RegisterRequestDto;
import com.example.Online_Task_Management_System.dto.request.UpdateProfileReqDto;
import com.example.Online_Task_Management_System.dto.response.*;
import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.enums.Roles;
import com.example.Online_Task_Management_System.repository.TaskRepository;
import com.example.Online_Task_Management_System.repository.UserRepository;
import com.example.Online_Task_Management_System.service.AuditLogService;
import com.example.Online_Task_Management_System.service.notifications.EmailService;
import com.example.Online_Task_Management_System.service.JwtService;
import com.example.Online_Task_Management_System.service.UserService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private AuditLogService auditLogService;

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    EmailService emailService;


    @Override
    public ResponseEntity<?> registerUser(RegisterRequestDto user) {
        try {
            log.info("User registration request received | email={}", user.getEmail());

            if (userRepository.existsByEmail(user.getEmail())) {
                log.warn("User creation failed | email already exists={}", user.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", 400,"message", "email already exists"));
            }

            if(user.getPassword().length() < 6){
                log.warn("User creation failed | Password length Must be 6 | email={}", user.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", 400,"message", "Password length Must be 6 characters.."));
            }
            Users nwUser = new Users();
            nwUser.setName(user.getName());
            nwUser.setEmail(user.getEmail());
            nwUser.setRoles(user.getRoles());
            nwUser.setPassword(passwordEncoder.encode(user.getPassword()));

            Users saved = userRepository.save(nwUser);

            auditLogService.log(
                    "CREATE_USER",
                    "User registered: " + user.getEmail(),
                    "Users",
                    nwUser.getId()
            );
            log.info(
                    "User created successfully | userId={} | email={} | role={}",
                    saved.getId(),
                    saved.getEmail(),
                    saved.getRoles()
            );

            RegisterResponseDto response = new RegisterResponseDto(
                    saved.getId(), saved.getName(), saved.getEmail(), saved.getRoles());

            return ResponseEntity.ok(
                    Map.of("status", 201, "message", "User Added successfully")
            );

        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500,"message", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> loginUser(LoginRequestDto request) {
        try {
            log.info("User login request received | email={}", request.getEmail());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()
                    )
            );
            var user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow();

            String token = jwtService.generateToken(user);
            auditLogService.log(
                    "LOGIN_SUCCESS",
                    "User logged in successfully",
                    "Users",
                    user.getId()
            );
            log.info(
                    "User Login successfully | userId={} | email={} | role={}",
                    user.getId(),
                    user.getEmail(),
                    user.getRoles()
            );


            return ResponseEntity.status(HttpStatus.OK)
                    .body(new LoginResponseDto(token, "Login Successful"));

        } catch (BadCredentialsException ex) {
            log.warn(
                    "User Login Failed (Invalid email or password) | email={} ",
                    request.getEmail()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", 401,"message", "Invalid email or password"));

        } catch (AuthenticationException ex) {
            auditLogService.log(
                    "LOGIN_FAILED",
                    "Authentication failed",
                    "Users",
                    Long.valueOf(-1)
            );
            log.warn(
                    "User Login Failed (Authentication failed) | email={} ",
                    request.getEmail()
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", 401,"message", "Authentication failed"));
        }
    }

    @Override
    public ResponseEntity<?> getProfile() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();
        Optional<Users> byEmail = userRepository.findByEmail(email);
        if (byEmail.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404,"message", "User Not Found"));
        Users users = byEmail.get();
        ProfileResponseDto response = new ProfileResponseDto();
        response.setId(users.getId());
        response.setEmail(users.getEmail());
        response.setRoles(users.getRoles().toString());
        response.setCreatedAt(users.getCreatedAt());
        response.setName(users.getName());
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateProfile(UpdateProfileReqDto reqDto) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        Optional<Users> optionalUsers = userRepository.findByEmail(email);
        if (optionalUsers.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404,"message", "User Not Found"));

        Users users = optionalUsers.get();

        if(reqDto.getPassword().length() < 6){
            log.warn("User creation failed | Password length Must be 6 | email={}", reqDto.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400,"message", "Password length Must be 6 characters.."));
        }


        StringBuilder changes = new StringBuilder("Updated fields: ");

        if (reqDto.getEmail() != null && !reqDto.getEmail().isEmpty()) {
            users.setEmail(reqDto.getEmail());
            changes.append("email, ");
        }

        if (reqDto.getName() != null && !reqDto.getName().isEmpty()) {
            users.setName(reqDto.getName());
            changes.append("name, ");
        }

        if (reqDto.getPassword() != null && !reqDto.getPassword().isEmpty()) {
            users.setPassword(passwordEncoder.encode(reqDto.getPassword()));
            changes.append("password, ");
        }

        userRepository.save(users);

        auditLogService.log(
                "UPDATE_PROFILE",
                changes.toString(),
                "Users",
                users.getId()
        );
        log.info("User Profile Updated | UserId={} | Changes = {}",users.getId(),changes.toString());

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("status", 200,"message", "Updated Successfully.."));
    }


    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<ProfileResponseDto>> getAllUser(
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        // default safety
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Users> userPage = userRepository.findAll(pageable);

        List<ProfileResponseDto> content = userPage.getContent()
                .stream()
                .map(user -> {
                    ProfileResponseDto dto = new ProfileResponseDto();
                    dto.setId(user.getId());
                    dto.setEmail(user.getEmail());
                    dto.setName(user.getName());
                    dto.setCreatedAt(user.getCreatedAt());
                    dto.setRoles(user.getRoles().name());
                    return dto;
                })
                .toList();

        PageResponse<ProfileResponseDto> response = new PageResponse<>();
        response.setContent(content);
        response.setCurrentPage(userPage.getNumber());
        response.setTotalPages(userPage.getTotalPages());
        response.setTotalElements(userPage.getTotalElements());

        return ResponseEntity.ok(response);
    }


    @Override
    @Transactional
    public ResponseEntity<?> deleteUser(Long userId) {

        Optional<Users> optionalUsers = userRepository.findById(userId);
        if (optionalUsers.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404,"message", "User Not Found"));

        Users user = optionalUsers.get();
        for (Task task : user.getTasks()) {
            task.getUsers().remove(user);
        }
        user.getTasks().clear();

        List<Task> createdTasks = taskRepository.findByCreatedBy(user);
        for (Task task : createdTasks) {
            task.setCreatedBy(null); // OR assign to admin/system user
        }

        userRepository.delete(user);

        auditLogService.log(
                "USER_DELETED",
                "User DELETED: " + user.getEmail(),
                "Users",
                user.getId()
        );

        log.info(
                "User Deleted successfully | userId={} | email={} | role={}",
                user.getId(),
                user.getEmail(),
                user.getRoles()
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("status", 200,"message", "User Deleted Successfully"));
    }


    @Override
    public void sendResetLink(String email) {

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generatePasswordResetToken(email);

        String resetLink =
                "http://localhost:8080/reset-password?token=" + token;

//        emailService.sendEmail(
//                "vaibhav@weetechsolution.com",
//                "Reset Your Password",
//                buildResetEmail(user.getName(), token)
//        );
    }

    public void resetPassword(String token, String newPassword) {

        Claims claims = jwtService.extractAllClaims(token);

        if (!"PASSWORD_RESET".equals(claims.get("type"))) {
            throw new RuntimeException("Invalid token");
        }
        if(newPassword.length() < 6){
            log.warn("User creation failed | Password length Must be 6");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(Map.of("status", 400,"message", "Password length Must be 6 characters.."));
        }

        String email = claims.getSubject();

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public ResponseEntity<?> forgotPass(ForgotPasswordDto forgotPasswordDto) {
        try{
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            String email = authentication.getName();

            Users users = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "User not found"));

            if (!passwordEncoder.matches(forgotPasswordDto.getOldPassword(), users.getPassword())) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", 400,"message", "Password Incorrect.."));
            }

            if(forgotPasswordDto.getNewPassword().equals(forgotPasswordDto.getOldPassword())){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status",400,"message", "Please Enter Diffrent Password.."));
            }

            if(forgotPasswordDto.getNewPassword().length() < 6){
                log.warn("User creation failed | Password length Must be 6 | email={}", users.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", 400,"message", "Password length Must be 6 characters.."));
            }

            users.setPassword(passwordEncoder.encode(forgotPasswordDto.getNewPassword()));

            Users save = userRepository.save(users);
            auditLogService.log(
                    "USER_PASSWORD_CHANGED",
                    "User : " + save.getEmail(),
                    "Users",
                    save.getId()
            );

            log.info(
                    "User Password Changed successfully | userId={} | email={} | role={}",
                    save.getId(),
                    save.getEmail(),
                    save.getRoles()
            );

            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("status",200,"message", "Password Updated Successfully.."));

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status",500,"message", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> getAllEmployee(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Users> userPage = userRepository.findByRoleNative(Roles.Employee.name(), pageable);
        List<ProfileResponseDto> content = userPage.getContent()
                .stream()
                .map(user -> {
                    ProfileResponseDto dto = new ProfileResponseDto();
                    dto.setId(user.getId());
                    dto.setName(user.getName());
                    dto.setEmail(user.getEmail());
                    dto.setRoles(user.getRoles().name());
                    dto.setCreatedAt(user.getCreatedAt());
                    return dto;
                })
                .toList();

        PageResponse<ProfileResponseDto> response = new PageResponse<>();
        response.setContent(content);
        response.setCurrentPage(userPage.getNumber());
        response.setTotalPages(userPage.getTotalPages());
        response.setTotalElements(userPage.getTotalElements());
        return ResponseEntity.ok(response);

    }

    @Override
    public ResponseEntity<?> getAllManager(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Users> userPage = userRepository.findByRoleNative(Roles.Manager.name(), pageable);
        List<ProfileResponseDto> content = userPage.getContent()
                .stream()
                .map(user -> {
                    ProfileResponseDto dto = new ProfileResponseDto();
                    dto.setId(user.getId());
                    dto.setName(user.getName());
                    dto.setEmail(user.getEmail());
                    dto.setRoles(user.getRoles().name());
                    dto.setCreatedAt(user.getCreatedAt());
                    return dto;
                })
                .toList();

        PageResponse<ProfileResponseDto> response = new PageResponse<>();
        response.setContent(content);
        response.setCurrentPage(userPage.getNumber());
        response.setTotalPages(userPage.getTotalPages());
        response.setTotalElements(userPage.getTotalElements());
        return ResponseEntity.ok(response);
    }


    private String buildResetEmail(String name, String resetToken) {

        String resetLink =
                "http://localhost:8080/reset-password?token=" + resetToken;

        return """
    Hello %s,

    We received a request to reset your account password.

    ===============================
    PASSWORD RESET LINK
    ===============================
    %s

    ===============================
    PASSWORD RESET TOKEN
    ===============================
    %s

    This token is valid for 15 minutes.

    If you did not request a password reset, please ignore this email.
    Your account will remain secure.

    ---
    Online Task Management System
    This is an automated message. Please do not reply.
    """.formatted(name, resetLink, resetToken);
    }



}
