package com.reservatec;

import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.TimeUnit;

@EnableScheduling

@SpringBootApplication
public class ReservatecApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservatecApplication.class, args);
	}

	@Bean
	CommandLineRunner testRedis(RedissonClient redissonClient) {
		return args -> {
			redissonClient.getBucket("prueba").set("¡Hola Redis!");
			System.out.println("✔ Conexión a Redis exitosa");
		};
	}



}
