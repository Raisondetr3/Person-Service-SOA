package ru.itmo.person_service;

import io.github.cdimascio.dotenv.Dotenv;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@OpenAPIDefinition(
		servers = {
				@Server(url = "https://localhost:58123", description = "Local HTTPS")
		})
public class PersonServiceApplication {

	public static void main(String[] args) {
//		Dotenv dotenv = Dotenv.configure()
//				.filename(".env")
//				.load();
//		dotenv.entries().forEach(entry ->
//				System.setProperty(entry.getKey(), entry.getValue())
//		);
		SpringApplication.run(PersonServiceApplication.class, args);
	}

}
