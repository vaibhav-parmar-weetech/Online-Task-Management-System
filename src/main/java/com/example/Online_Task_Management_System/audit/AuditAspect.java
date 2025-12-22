package com.example.Online_Task_Management_System.audit;

import com.example.Online_Task_Management_System.dto.response.RegisterResponseDto;
import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.service.AuditLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogService auditLogService;

    @AfterReturning(
            pointcut = "@annotation(auditable)",
            returning = "result"
    )
    public void logAudit(JoinPoint joinPoint, Auditable auditable, Object result) {

        Long entityId = null;

        // 1️⃣ From method param
        if (!auditable.entityIdParam().isEmpty()) {
            entityId = extractEntityIdFromArgs(joinPoint, auditable.entityIdParam());
        }

        // 2️⃣ From return object (CREATE)
        if (entityId == null && auditable.extractFromResult()) {
            entityId = extractEntityIdFromResult(result);
        }

        // 3️⃣ From logged-in user
        if (entityId == null && auditable.useLoggedInUser()) {
            entityId = getLoggedInUserId();
        }

        auditLogService.log(
                auditable.action(),
                "Action executed successfully",
                auditable.entity(),
                entityId
        );
    }

    private Long extractEntityIdFromResult(Object result) {

        if (result instanceof ResponseEntity<?> response) {
            Object body = response.getBody();


            if (body instanceof RegisterResponseDto dto) {
                return dto.getId();
            }
        }
        return null;
    }

    /**
     * Extract entityId from method arguments by parameter name
     */
    private Long extractEntityIdFromArgs(JoinPoint joinPoint, String paramName) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }
        return null;
    }

    /**
     * Extract logged-in user id from SecurityContext
     */
    private Long getLoggedInUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Users users) {
            return users.getId();
        }

        return null;
    }
}
