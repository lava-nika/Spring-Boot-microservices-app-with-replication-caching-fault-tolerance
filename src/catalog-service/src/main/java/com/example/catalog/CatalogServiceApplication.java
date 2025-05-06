package com.example.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// this is the main entry point for the catalog service Spring Boot application
@SpringBootApplication
public class CatalogServiceApplication {
	// main method to start the Catalog microservice
	public static void main(String[] args) {
		SpringApplication.run(CatalogServiceApplication.class, args);
	}

}
