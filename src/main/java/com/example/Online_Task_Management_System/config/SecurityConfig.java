package com.example.Online_Task_Management_System.config;

import com.example.Online_Task_Management_System.filter.JwtAccessDeniedHandler;
import com.example.Online_Task_Management_System.filter.JwtAuthEntryPoint;
import com.example.Online_Task_Management_System.filter.JwtAuthenticationFilter;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, JwtAuthEntryPoint jwtAuthEntryPoint, JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Custom 401 / 403 handling
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint)   // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler)   // 403
                )

                .authorizeHttpRequests(auth -> auth

                                //  PUBLIC
                                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                                .requestMatchers("/", "/index.html","/index2.html","/index3.html").permitAll()
                                .requestMatchers("/auth/**").permitAll()
                                .requestMatchers("/ws/**").permitAll()
                                .requestMatchers("/favicon.ico").permitAll()
                                .requestMatchers("/api/monitor/**").permitAll()
                                .requestMatchers("/actuator/**").permitAll()


                                // ---------- SWAGGER ----------
                                .requestMatchers(
                                        "/v3/api-docs/**",
                                        "/swagger-resources/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/webjars/**"
                                ).permitAll()

                                // ---------- USER PROFILE (ALL ROLES) ----------
                                .requestMatchers(
                                        "/api/users/profile",
                                        "/api/users/update",
                                        "/api/users/change-password"
                                ).hasAnyAuthority("ROLE_Admin", "ROLE_Manager", "ROLE_Employee")

                                // ---------- USER MANAGEMENT (ADMIN) ----------
                                .requestMatchers(
                                        "/api/users/all-users",
                                        "/api/users/delete/**",
                                        "/api/users/all-manager",
                                        "/api/users/view-logs/**"
                                ).hasAuthority("ROLE_Admin")

                                // ---------- USER MANAGEMENT (ADMIN + MANAGER) ----------
                                .requestMatchers(
                                        "/api/users/all-employee"
                                ).hasAnyAuthority("ROLE_Admin", "ROLE_Manager")


                                // ---------- TASK FILES ----------

                                // Download → All roles
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/tasks/files/*/download"
                                ).hasAnyAuthority("ROLE_Employee", "ROLE_Manager", "ROLE_Admin")

                                // List files (THIS ONE) → All roles
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/tasks/*/files"
                                ).hasAnyAuthority("ROLE_Employee", "ROLE_Manager", "ROLE_Admin")

                                // Upload files → Admin & Manager only
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/tasks/*/files"
                                ).hasAnyAuthority("ROLE_Admin", "ROLE_Manager")

                                // Delete file → Admin only
                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/tasks/delete-file/**"
                                ).hasAuthority("ROLE_Admin")


                                // ---------- TASK COMMENTS ----------
                                // Add comment → All roles
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/tasks/comment/**"
                                ).hasAnyAuthority("ROLE_Employee", "ROLE_Manager", "ROLE_Admin")

                                // View comments → Admin & Manager
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/tasks/comment/**"
                                ).hasAnyAuthority("ROLE_Admin", "ROLE_Manager")


                                // ---------- TASK MANAGEMENT (ADMIN + MANAGER) ----------
                                .requestMatchers(
                                        "/api/tasks/add-task",
                                        "/api/tasks/editTask/**",
                                        "/api/tasks/deleteTask/**",
                                        "/api/tasks/*/assign",
                                        "/api/tasks/view-task/**",
                                        "/api/tasks/v1/view-all-task/**",
                                        "/api/tasks/v2/view-all-task/**"
                                ).hasAnyAuthority("ROLE_Admin", "ROLE_Manager")

                                // ---------- TASK MANAGEMENT (EMPLOYEE) ----------
                                .requestMatchers(
                                        "/api/tasks/v1/EMP/view-all-task",
                                        "/api/tasks/v2/EMP/view-all-task",
                                        "/api/tasks/my-task/**",
                                        "/api/tasks/update-task/**"
                                ).hasAuthority("ROLE_Employee")

                                // ---------- FALLBACK ----------
                                .anyRequest().authenticated()
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
