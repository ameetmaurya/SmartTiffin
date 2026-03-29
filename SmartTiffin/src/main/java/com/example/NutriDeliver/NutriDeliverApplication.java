package com.example.NutriDeliver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling // <--- ADD THIS
public class NutriDeliverApplication {

	public static void main(String[] args) {
		SpringApplication.run(NutriDeliverApplication.class, args);
	}

}
