package com.pratik.urlshortener;

import com.pratik.urlshortener.service.RedisTestService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UrlshortenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlshortenerApplication.class, args);
	}

	@Bean
	CommandLineRunner run(RedisTestService redisTestService) {

		return args -> {

			redisTestService.testRedis();

		};

	}

}
