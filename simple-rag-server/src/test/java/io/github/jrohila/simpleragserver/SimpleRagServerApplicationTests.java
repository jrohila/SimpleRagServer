package io.github.jrohila.simpleragserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.vectorstore.VectorStore;
import org.mockito.Mockito;

@SpringBootTest
class SimpleRagServerApplicationTests {

	@TestConfiguration
	static class TestConfig {
		@Bean
		VectorStore vectorStore() {
			return Mockito.mock(VectorStore.class);
		}
	}

	@Test
	void contextLoads() {
	}

}
