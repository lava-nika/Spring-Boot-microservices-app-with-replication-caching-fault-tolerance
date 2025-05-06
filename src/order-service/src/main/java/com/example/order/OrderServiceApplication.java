package com.example.order;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// main entry point for OrderService replica
// for replication, can run this multiple times with different replica IDs
@SpringBootApplication
public class OrderServiceApplication {
	// logger to track startup and anything related to replica
	private static final Logger logger = LoggerFactory.getLogger(OrderServiceApplication.class);

	// unique ID of this replica, get from application.properties
	@Value("${replica.id}")
	private int replicaId;

	// Spring Boot main method to launch the application
	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

	// logs replica ID to identify which replica is running
	@PostConstruct
	public void logReplicaInfo() {
		logger.info("Order service Replica with ID: {} has started", replicaId);
	}
}
