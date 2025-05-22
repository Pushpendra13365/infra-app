package com.infra_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class InfraAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(InfraAppApplication.class, args);
	}

}
