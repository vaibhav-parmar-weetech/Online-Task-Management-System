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
                        .requestMatchers("/", "/index.html").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // SWAGGER
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()

                        //USERS
                        .requestMatchers(
                                "/api/users/profile",
                                "/api/users/update",
                                "/api/users/change-password"
                        ).hasAnyAuthority("ROLE_Admin", "ROLE_Manager", "ROLE_Employee")

                        .requestMatchers(
                                "/api/users/all-users",
                                "/api/users/delete/**",
                                "/api/users/all-manager"
                        ).hasAuthority("ROLE_Admin")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/all-employee"
                        ).hasAnyAuthority("ROLE_Admin", "ROLE_Manager")



                        //  TASKS (ADMIN + MANAGER)
                        .requestMatchers(
                                "/api/tasks/add-task",
                                "/api/tasks/editTask/**",
                                "/api/tasks/deleteTask/**",
                                "/api/tasks/*/assign",
                                "/api/tasks/view-task/**",
                                "/api/tasks/v1/view-all-task/**",
                                "/api/tasks/v2/view-all-task/**"
                        ).hasAnyAuthority("ROLE_Admin", "ROLE_Manager")

                        // TASKS (EMPLOYEE)
                        .requestMatchers(
                                "/api/tasks/v1/EMP/view-all-task",
                                "/api/tasks/v2/EMP/view-all-task",
                                "/api/tasks/my-task/**",
                                "/api/tasks/update-task/**"
                        ).hasAuthority("ROLE_Employee")

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
