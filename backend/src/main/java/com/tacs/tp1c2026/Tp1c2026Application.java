package com.tacs.tp1c2026;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableAsync
public class Tp1c2026Application {

	public static void main(String[] args) {
		SpringApplication.run(Tp1c2026Application.class, args);
	}
	@PostConstruct
	public void checkEnv() {
    	System.out.println("SPRING_MONGODB_URI=" + System.getenv("SPRING_MONGODB_URI"));
	}
}
