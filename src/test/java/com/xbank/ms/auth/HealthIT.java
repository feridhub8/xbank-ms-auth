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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** specs/service-health: health endpoint is up, other actuator endpoints are hidden. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class HealthIT {

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
	void healthIsUp() {
		ResponseEntity<Map<String, Object>> response = client.get().uri("/actuator/health")
				.retrieve().toEntity(new ParameterizedTypeReference<>() {});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("status", "UP");
	}

	@Test
	void otherActuatorEndpointsAreHidden() {
		ResponseEntity<Void> response = client.get().uri("/actuator/env").retrieve().toBodilessEntity();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
