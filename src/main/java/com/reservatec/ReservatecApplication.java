package com.reservatec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling

@SpringBootApplication
public class ReservatecApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservatecApplication.class, args);
	}

}
