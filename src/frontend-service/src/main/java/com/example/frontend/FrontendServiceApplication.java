package com.example.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// main entry point for the Frontend microservice
@SpringBootApplication
public class FrontendServiceApplication {

	// launches the Spring Boot application, starts embedded Tomcat web server
	public static void main(String[] args) {
		SpringApplication.run(FrontendServiceApplication.class, args);
	}

}

