package com.example.Online_Task_Management_System.filter;

import com.example.Online_Task_Management_System.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RateLimitFilter extends OncePerRequestFilter {
    /* ================= EXCLUDED PATHS ================= */
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars",
            "/actuator/health",
            "/error",  // ===== Static UI =====
            "/",
            "/index.html",
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico",
            "/index2.html",
            "/index3.html"
    );

    /* ================= BUCKET STORES ================= */
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> taskBuckets = new ConcurrentHashMap<>();

    /* ================= MAIN FILTER ================= */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr(); // IP based (JWT ready)

        Bucket bucket = resolveBucket(key, path, request.getMethod());

        if (bucket.tryConsume(1)) {
            response.setHeader("X-Rate-Limit-Remaining",
                    String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            response.setHeader("Retry-After", "60");

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");

            ErrorResponse error = new ErrorResponse(
                    429,
                    "Too many requests"
            );

            new ObjectMapper().writeValue(response.getWriter(), error);
        }
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private Bucket resolveBucket(String key, String path, String method) {

        // 🔐 AUTH APIs (STRICT)
        if (path.startsWith("/auth")) {
            return authBuckets.computeIfAbsent(
                    key, k -> authBucket(method)
            );
        }

        // 👤 USER APIs
        if (path.startsWith("/api/users")) {
            return userBuckets.computeIfAbsent(
                    key, k -> userBucket(method)
            );
        }

        // 📋 TASK APIs
        if (path.startsWith("/api/tasks")) {
            return taskBuckets.computeIfAbsent(
                    key, k -> taskBucket(method)
            );
        }
        return taskBuckets.computeIfAbsent(key, k -> taskBucket(method));
    }


    // AUTH → POST stricter
    private Bucket authBucket(String method) {
        if ("POST".equals(method)) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(5,
                            Refill.intervally(5, Duration.ofMinutes(1))))
                    .build();
        }
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10,
                        Refill.intervally(10, Duration.ofMinutes(1))))
                .build();
    }

    // USERS
    private Bucket userBucket(String method) {
        if ("POST".equals(method)) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(20,
                            Refill.intervally(20, Duration.ofHours(1))))
                    .build();
        }
        return Bucket.builder()
                .addLimit(Bandwidth.classic(30,
                        Refill.intervally(30, Duration.ofHours(1))))
                .build();
    }

    // TASKS
    private Bucket taskBucket(String method) {
        if ("POST".equals(method)) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(2,
                            Refill.intervally(2, Duration.ofSeconds(1))))
                    .build();
        }
        return Bucket.builder()
                .addLimit(Bandwidth.classic(3,
                        Refill.intervally(3, Duration.ofSeconds(1))))
                .build();
    }
}



