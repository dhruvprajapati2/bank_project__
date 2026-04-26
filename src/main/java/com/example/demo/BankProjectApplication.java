package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.demo.forentity.Admin;
import com.example.demo.repo.Adminrepo;

@SpringBootApplication
public class BankProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankProjectApplication.class, args);
	}

	@Bean
	CommandLineRunner seedAdminUser(
			Adminrepo adminRepo,
			@Value("${app.admin.email}") String adminEmail,
			@Value("${app.admin.password}") String adminPassword) {
		return args -> {
			Admin admin = adminRepo.findByEmail(adminEmail)
					.orElseGet(Admin::new);

			admin.setEmail(adminEmail);
			admin.setPassword(adminPassword);
			adminRepo.save(admin);
		};
	}

}
