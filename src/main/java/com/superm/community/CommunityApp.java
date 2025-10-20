package com.superm.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import me.paulschwarz.springdotenv.annotation.EnableDotenv;

@SpringBootApplication
@EnableDotenv
public class CommunityApp {
	public static void main(String[] args) {
		SpringApplication.run(CommunityApp.class, args);
	}
}
