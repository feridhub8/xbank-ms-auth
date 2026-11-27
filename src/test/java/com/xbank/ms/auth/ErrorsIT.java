package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.xbank.ms.auth.support.TestVault;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** specs/api-errors in the real application, for errors that exist without any endpoint. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ErrorsIT {

	static {
		TestVault.start();
	}

	@LocalServerPort
	private int port;

	private RestClient client;

	@BeforeEach
	void setUp() {
		client = RestClient.builder()
				.baseUrl("http://localhost:" + port)
				.defaultStatusHandler(status -> true, (request, response) -> { })
				.build();
	}

	@Test
	void unknownPathIsAProblemWithEveryField() {
		ResponseEntity<Map<String, Object>> response = client.get().uri("/does-not-exist")
				.retrieve().toEntity(new ParameterizedTypeReference<>() {});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody())
				.containsEntry("code", "NOT_FOUND")
				.containsEntry("status", 404)
				.containsEntry("type", "https://xbank.local/errors/not-found")
				.containsEntry("instance", "/does-not-exist");
		assertThat(response.getBody().get("title")).asString().isNotBlank();
		assertThat(response.getBody().get("detail")).asString().isNotBlank();
	}

	@Test
	void wrongMethodIsAProblem() {
		ResponseEntity<Map<String, Object>> response = client.post().uri("/actuator/health")
				.retrieve().toEntity(new ParameterizedTypeReference<>() {});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody())
				.containsEntry("code", "METHOD_NOT_ALLOWED")
				.containsEntry("status", 405)
				.containsEntry("instance", "/actuator/health");
	}
}
