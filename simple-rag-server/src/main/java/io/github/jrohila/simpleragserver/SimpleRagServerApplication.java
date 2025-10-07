package io.github.jrohila.simpleragserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"io.github.jrohila.simpleragserver"})
@EnableScheduling
public class SimpleRagServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleRagServerApplication.class, args);
	}

}