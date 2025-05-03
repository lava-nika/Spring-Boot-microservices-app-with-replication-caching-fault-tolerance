package com.example.order;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class OrderServiceApplication {
	private static final Logger logger = LoggerFactory.getLogger(OrderServiceApplication.class);

	@Value("${replica.id}")
	private int replicaId;

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

	@PostConstruct
	public void logReplicaInfo() {
		logger.info("Order service Replica with ID: {} has started", replicaId);
	}
}
