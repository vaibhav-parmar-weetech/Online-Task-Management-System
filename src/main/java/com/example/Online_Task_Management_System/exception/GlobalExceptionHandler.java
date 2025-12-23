package com.example.Online_Task_Management_System.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "errors", errors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleInvalidEnum(HttpMessageNotReadableException ex) {

        if (ex.getCause() instanceof InvalidFormatException ife &&
                ife.getTargetType().isEnum()) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "error", "Bad Request",
                            "message", "Invalid role value. Allowed values: Admin, Manager, Employee"
                    )
            );
        }

        return ResponseEntity.badRequest().body(
                Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", "Invalid request payload"
                )
        );
    }

}
