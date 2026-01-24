package com.example.Online_Task_Management_System.service.Impl;


import com.example.Online_Task_Management_System.dto.request.ForgotPasswordDto;
import com.example.Online_Task_Management_System.dto.request.LoginRequestDto;
import com.example.Online_Task_Management_System.dto.request.RegisterRequestDto;
import com.example.Online_Task_Management_System.dto.request.UpdateProfileReqDto;
import com.example.Online_Task_Management_System.dto.response.*;
import com.example.Online_Task_Management_System.entity.Task;
import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.entity.VerificationToken;
import com.example.Online_Task_Management_System.enums.Roles;
import com.example.Online_Task_Management_System.exception.custom.BadRequestException;
import com.example.Online_Task_Management_System.exception.custom.ResourceNotFoundException;
import com.example.Online_Task_Management_System.exception.custom.UnauthorizedException;
import com.example.Online_Task_Management_System.repository.TaskRepository;
import com.example.Online_Task_Management_System.repository.UserRepository;
import com.example.Online_Task_Management_System.repository.VerificationTokenRepository;
import com.example.Online_Task_Management_System.service.AuditLogService;
import com.example.Online_Task_Management_System.service.notifications.EmailService;
import com.example.Online_Task_Management_System.security.JwtService;
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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    VerificationTokenRepository verificationTokenRepository;

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

    @Autowired
    VerificationTokenRepository tokenRepository;


    @Override
    @Transactional
    public ResponseEntity<?> registerUser(RegisterRequestDto user) {
        try {
            log.info("User registration request received | email={}", user.getEmail());

            if (userRepository.existsByEmail(user.getEmail())) {
                log.warn("User creation failed | email already exists={}", user.getEmail());
                throw new BadRequestException("Email already exists");
            }

            if (user.getPassword().length() < 6) {
                log.warn("User creation failed | Password length < 6 | email={}", user.getEmail());
                throw new BadRequestException("Password length must be at least 6 characters");
            }

            Users newUser = new Users();
            newUser.setName(user.getName());
            newUser.setEmail(user.getEmail());
            newUser.setRoles(user.getRoles());
            newUser.setPassword(passwordEncoder.encode(user.getPassword()));

            Users saved = userRepository.save(newUser);

            createVerificationToken(saved);

            auditLogService.log(
                    "CREATE_USER",
                    "User registered: " + user.getEmail(),
                    "Users",
                    saved.getId()
            );

            log.info("User created successfully | userId={} | email={} | role={}",
                    saved.getId(), saved.getEmail(), saved.getRoles());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", 201, "message", "User added successfully"));

        } catch (BadRequestException ex) {
            throw ex;

        } catch (Exception ex) {
            log.error("Unexpected error during user registration | email={}", user.getEmail(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }


    @Transactional
    public void createVerificationToken(Users user) {

        // delete old token if exists

        Optional<VerificationToken> vf = verificationTokenRepository.findByUser(user);
        if (vf.isPresent()) tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken(
                token,
                user,
                LocalDateTime.now().plusHours(24)
        );

        tokenRepository.save(verificationToken);

        String link = "http://localhost:8080/auth/verify?token=" + token;

//        emailService.sendHtmlEmail(
//                "vaibhav@weetechsolution.com",  // user.getEmail(),
//                "Verify your account",
//                buildVerificationEmail(user, link)
//        );
        System.out.println(token);
    }


    @Override
    public ResponseEntity<?> loginUser(LoginRequestDto request) {
        try {
            log.info("User login request | email={}", request.getEmail());

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()
                    )
            );

            Users user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

            String token = jwtService.generateToken(user);

            auditLogService.log(
                    "LOGIN_SUCCESS",
                    "User logged in successfully",
                    "Users",
                    user.getId()
            );

            return ResponseEntity.ok(new LoginResponseDto(token, "Login Successful"));

        } catch (DisabledException ex) {
            log.warn("Login failed - email not verified | email={}", request.getEmail());
            throw new UnauthorizedException("Please verify your email first");

        } catch (BadCredentialsException ex) {
            log.warn("Login failed - invalid credentials | email={}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");

        } catch (AuthenticationException ex) {
            log.error("Authentication failed | email={}", request.getEmail(), ex);
            throw new UnauthorizedException("Authentication failed");
        }
    }



    @Override
    public ResponseEntity<?> getProfile() {
        try{
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            String email = authentication.getName();
            Optional<Users> byEmail = userRepository.findByEmail(email);
            if (byEmail.isEmpty()) throw new ResourceNotFoundException("User Not Found");
            Users users = byEmail.get();
            ProfileResponseDto response = new ProfileResponseDto();
            response.setId(users.getId());
            response.setEmail(users.getEmail());
            response.setRoles(users.getRoles().toString());
            response.setCreatedAt(users.getCreatedAt());
            response.setName(users.getName());
            return ResponseEntity.ok(response);
        }catch (ResourceNotFoundException e){
            throw e;
        }catch (Exception e){
            throw new RuntimeException("");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateProfile(UpdateProfileReqDto reqDto) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        Optional<Users> optionalUsers = userRepository.findByEmail(email);
        if (optionalUsers.isEmpty()) throw new ResourceNotFoundException("User Not Found");

        Users users = optionalUsers.get();

        if (reqDto.getPassword().length() < 6) {
            log.warn("User creation failed | Password length Must be 6 | email={}", reqDto.getEmail());
            throw new BadRequestException("Password length Must be 6 characters..");
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
        log.info("User Profile Updated | UserId={} | Changes = {}", users.getId(), changes.toString());

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("status", 200, "message", "Updated Successfully.."));
    }


    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<ProfileResponseDto>> getAllUser(
            int page, int size, String sortBy, String direction ){
        try {
            // Validate pagination parameters
            if (page < 0) {
                throw new BadRequestException("Page index must be >= 0");
            }
            if (size <= 0) {
                throw new BadRequestException("Page size must be > 0");
            }

            // Default sorting
            if (sortBy == null || sortBy.isBlank()) {
                sortBy = "createdAt";
            }

            Sort sort = "asc".equalsIgnoreCase(direction)
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Users> userPage = userRepository.findAll(pageable);

            // Optional: Throw if no users found
            // if (userPage.isEmpty()) {
            //     throw new ResourceNotFoundException("No users found");
            // }

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

        } catch (BadRequestException ex) {
            // Pass through global handler
            throw ex;

        } catch (Exception ex) {
            log.error("Failed to fetch users: {}", ex.getMessage(), ex);
            throw new RuntimeException("Internal server error while fetching users");
        }
    }



    @Override
    @Transactional
    public ResponseEntity<?> deleteUser(Long userId) {
        try {
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

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

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "User Deleted Successfully"
            ));

        } catch (ResourceNotFoundException ex) {
            // Let the global handler handle it
            throw ex;

        } catch (Exception ex) {
            log.error("Failed to delete user with id {}: {}", userId, ex.getMessage(), ex);
            throw new RuntimeException("Internal server error while deleting user");
        }
    }



    @Override
    public void sendResetLink(String email) {

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generatePasswordResetToken(email);
        System.out.println(token);
        String resetLink =
                "http://localhost:8080/reset-password?token=" + token;

//        emailService.sendEmail(
//                "vaibhav@weetechsolution.com",
//                "Reset Your Password",
//                buildResetEmail(user.getName(), token)
//        );
        log.info("Password reset link prepared for email={}: {}", email, resetLink);

    }

    @Override
    @Transactional
    public ResponseEntity<?> resetPassword(String token, String newPassword) {
        try {
            log.info("Password reset request received");

            Claims claims = jwtService.extractAllClaims(token);

            if (!"PASSWORD_RESET".equals(claims.get("type"))) {
                log.warn("Invalid password reset token used");
                throw new BadRequestException("Invalid token");
            }

            if (newPassword.length() < 6) {
                log.warn("Password reset failed | Password length < 6");
                throw new BadRequestException("Password length must be at least 6 characters");
            }

            String email = claims.getSubject();

            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            log.info("Password updated successfully | email={}", email);

            auditLogService.log(
                    "PASSWORD_RESET",
                    "Password updated successfully",
                    "Users",
                    user.getId()
            );

            return ResponseEntity.ok(Map.of("status", 200, "message", "Password updated successfully"));

        } catch (Exception e) {
            log.error("Unexpected error during password reset", e);
            throw e; // Let GlobalExceptionHandler handle unexpected errors
        }
    }


    @Override
    public ResponseEntity<?> forgotPass(ForgotPasswordDto forgotPasswordDto) {
        try {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            String email = authentication.getName();

            Optional<Users> byEmail = userRepository.findByEmail(email);
            if(byEmail.isEmpty()) return new ResponseEntity<>(Map.of("status",404,"message","User not found"),HttpStatus.NOT_FOUND);

            Users users = byEmail.get();

            if (!passwordEncoder.matches(forgotPasswordDto.getOldPassword(), users.getPassword())) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", 400, "message", "Password Incorrect.."));
            }

            if (forgotPasswordDto.getNewPassword().equals(forgotPasswordDto.getOldPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", 400, "message", "Please Enter Diffrent Password.."));
            }

            if (forgotPasswordDto.getNewPassword().length() < 6) {
                log.warn("User creation failed | Password length Must be 6 | email={}", users.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", 400, "message", "Password length Must be 6 characters.."));
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
                    .body(Map.of("status", 200, "message", "Password Updated Successfully.."));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> getAllEmployee(int page, int size) {
        try {
            if (page < 0) {
                throw new BadRequestException("Page index must be >= 0");
            }
            if (size <= 0) {
                throw new BadRequestException("Page size must be > 0");
            }

            Pageable pageable = PageRequest.of(page, size);

            Page<Users> userPage = userRepository.findByRoleNative(Roles.ROLE_Employee.name(), pageable);


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

        } catch (BadRequestException ex) {
            // Pass through to global handler
            throw ex;

        } catch (Exception ex) {
            log.error("Failed to fetch employees: {}", ex.getMessage(), ex);
            throw new RuntimeException("Internal server error while fetching employees");
        }

    }

    @Override
    public ResponseEntity<?> getAllManager(int page, int size) {
        try {
            if (page < 0) {
                throw new BadRequestException("Page index must be >= 0");
            }
            if (size <= 0) {
                throw new BadRequestException("Page size must be > 0");
            }

            Pageable pageable = PageRequest.of(page, size);

            Page<Users> userPage = userRepository.findByRoleNative(Roles.ROLE_Manager.name(), pageable);

            // Optional: throw if no managers found
            // if (userPage.isEmpty()) {
            //     throw new ResourceNotFoundException("No managers found");
            // }

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

        } catch (BadRequestException ex) {
            // Let the global handler handle it
            throw ex;

        } catch (Exception ex) {
            log.error("Failed to fetch managers: {}", ex.getMessage(), ex);
            throw new RuntimeException("Internal server error while fetching managers");
        }
    }


    @Override
    @Transactional
    public ResponseEntity<?> verifyEmail(String token) {
        try {
            log.info("Email verification request received | token={}", token);

            VerificationToken verificationToken = tokenRepository.findByToken(token)
                    .orElseThrow(() -> new BadRequestException("Invalid token"));

            if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                tokenRepository.delete(verificationToken);
                log.warn("Email verification failed | Token expired | token={}", token);
                throw new BadRequestException("Token expired");
            }

            Users user = verificationToken.getUser();
            user.setEnabled(true);
            userRepository.save(user);

            tokenRepository.delete(verificationToken);
            log.info("Email verified successfully | userId={} | email={}", user.getId(), user.getEmail());

            // Audit log
            auditLogService.log(
                    "EMAIL_VERIFIED",
                    "Email verified successfully",
                    "User",
                    user.getId()
            );

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Email verified successfully"
            ));

        } catch (Exception e) {
            log.error("Unexpected error during email verification | token={}", token, e);
            throw e; // Let GlobalExceptionHandler handle unexpected exceptions
        }
    }


    @Override
    @Transactional
    public ResponseEntity<?> resendVerificationEmail(String email) {
        try {
            log.info("Resend verification email request received | email={}", email);

            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            if (user.isEnabled()) {
                log.warn("Attempted to resend verification email but email already verified | email={}", email);
                throw new BadRequestException("Email already verified");
            }

            // Generate new verification token
            createVerificationToken(user);
            log.info("Verification token regenerated for email={}", email);

            // Audit log
            auditLogService.log(
                    "EMAIL_VERIFICATION_RESENT",
                    "Verification email resent to " + user.getEmail(),
                    "User",
                    user.getId()
            );

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Verification email sent successfully"
            ));

        } catch (Exception e) {
            log.error("Unexpected error while resending verification email for email={}", email, e);
            throw e; // Let GlobalExceptionHandler handle unexpected exceptions
        }
    }


    public String buildVerificationEmail(Users user, String verificationLink) {

        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;
                            border: 1px solid #e0e0e0; padding: 24px; border-radius: 10px;
                            background-color: #ffffff;">
                
                    <h2 style="color: #2c3e50; text-align: center;">
                        Email Verification
                    </h2>
                
                    <p style="font-size: 15px; color: #333;">
                        Hello <strong>%s</strong>,
                    </p>
                
                    <p style="font-size: 14px; color: #555;">
                        Thank you for registering with <strong>Online Task Management System</strong>.
                        To complete your registration, please verify your email address by clicking the button below.
                    </p>
                
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s"
                           style="background-color: #1abc9c; color: white; padding: 12px 24px;
                                  text-decoration: none; border-radius: 6px; font-size: 15px;">
                            Verify Email
                        </a>
                    </div>
                
                    <p style="font-size: 13px; color: #777;">
                        This verification link will expire in <strong>24 hours</strong>.
                    </p>
                
                    <p style="font-size: 13px; color: #777;">
                        If you did not create an account, please ignore this email.
                    </p>
                
                    <hr style="margin: 25px 0;">
                
                    <p style="font-size: 12px; color: #999; text-align: center;">
                        © %d Online Task Management System<br/>
                        This is an automated email. Please do not reply.
                    </p>
                </div>
                """.formatted(
                user.getName(),
                verificationLink,
                LocalDate.now().getYear()
        );
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
