package com.example.Online_Task_Management_System;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableScheduling
@EnableAsync
public class OnlineTaskManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineTaskManagementSystemApplication.class, args);
	}

}
