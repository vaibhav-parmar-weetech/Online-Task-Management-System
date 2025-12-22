package com.example.Online_Task_Management_System.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();
    String entity();
    String entityIdParam() default ""; // parameter name
    boolean useLoggedInUser() default false;
    boolean extractFromResult() default false; // 👈 ADD THIS

}
