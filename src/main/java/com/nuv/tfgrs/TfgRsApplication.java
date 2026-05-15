package com.nuv.tfgrs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TfgRsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TfgRsApplication.class, args);
	}

}
