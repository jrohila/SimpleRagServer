package io.github.jrohila.simpleragserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.jrohila.simpleragserver.config.OpenNlpRuntimeHints;

@SpringBootApplication(scanBasePackages = {"io.github.jrohila.simpleragserver"})
@EnableScheduling
@ImportRuntimeHints(OpenNlpRuntimeHints.class)
public class SimpleRagServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleRagServerApplication.class, args);
	}

}