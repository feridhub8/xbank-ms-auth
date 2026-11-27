package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.xbank.ms.auth.support.TestVault;
import java.net.http.HttpClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** specs/api-docs in the local profile. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ApiDocsIT {

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
				.requestFactory(new JdkClientHttpRequestFactory(
						HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()))
				.defaultStatusHandler(status -> true, (request, response) -> { })
				.build();
	}

	@Test
	void openApiJsonIsServed() {
		ResponseEntity<Map<String, Object>> response = client.get().uri("/v3/api-docs")
				.retrieve().toEntity(new ParameterizedTypeReference<>() {});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("openapi")).asString().startsWith("3.");
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerDocumentsEveryResponse() {
		Map<String, Object> docs = client.get().uri("/v3/api-docs").retrieve().body(new ParameterizedTypeReference<>() {});
		Map<String, Object> paths = (Map<String, Object>) docs.get("paths");
		Map<String, Object> register = (Map<String, Object>) ((Map<String, Object>) paths.get("/api/v1/auth/register")).get("post");
		Map<String, Map<String, Object>> responses = (Map<String, Map<String, Object>>) register.get("responses");

		assertThat(register.get("summary")).asString().isNotBlank();
		assertThat(responses).containsOnlyKeys("201", "400", "409", "503");
		for (String error : new String[] { "400", "409", "503" }) {
			assertThat((Map<String, Object>) responses.get(error).get("content")).as(error).containsOnlyKeys("application/problem+json");
		}
	}

	@Test
	void swaggerUiIsServed() {
		ResponseEntity<String> response = client.get().uri("/swagger-ui.html").retrieve().toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("Swagger UI");
	}
}
