package com.reservatec;

import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class ReservatecApplication {

	public static void main(String[] args) {
		// Forzar la zona horaria a America/Lima para toda la aplicación
		TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
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
