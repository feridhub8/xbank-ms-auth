package com.xbank.ms.auth.auth.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.xbank.ms.auth.support.TestVault;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** specs/user-auth "Identity provider unavailable": Keycloak points at a port where nothing listens. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "xbank.keycloak.url=http://127.0.0.1:1", "xbank.keycloak.connect-timeout=500ms" })
@ActiveProfiles("local")
class RegisterKeycloakDownIT {

	static {
		TestVault.start();
	}

	@LocalServerPort
	private int port;

	@Test
	void registrationIs503WhenKeycloakIsUnreachable() {
		ResponseEntity<Map<String, Object>> response = RestClient.builder()
				.baseUrl("http://localhost:" + port)
				.defaultStatusHandler(status -> true, (request, resp) -> { })
				.build()
				.post().uri("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("fin", "AB12C3D", "email", "down@xbank.test", "password", "Passw0rd",
						"firstName", "Test", "lastName", "User", "phoneNumber", "+994501234567"))
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getBody()).containsEntry("code", "UPSTREAM_UNAVAILABLE");
	}
}
