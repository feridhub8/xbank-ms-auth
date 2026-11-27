package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.xbank.ms.auth.support.TestVault;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** specs/api-docs "No docs in production". */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
class ApiDocsProdIT {

	/**
	 * The prod profile reads secret/xbank/prod/ms-auth, so it signs in with AppRole ms-auth-prod. System
	 * properties, not @DynamicPropertySource: the Vault import runs before dynamic properties exist.
	 * TestVault.start() resets them to ms-auth-local for every other test.
	 */
	static {
		TestVault.start();
		TestVault.useAppRole("prod");
	}

	@LocalServerPort
	private int port;

	@ParameterizedTest
	@ValueSource(strings = { "/v3/api-docs", "/swagger-ui.html" })
	void docsAreNotFoundInProd(String path) {
		ResponseEntity<Map<String, Object>> response = RestClient.builder()
				.baseUrl("http://localhost:" + port)
				.defaultStatusHandler(status -> true, (request, resp) -> { })
				.build()
				.get().uri(path)
				.retrieve().toEntity(new ParameterizedTypeReference<>() {});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).containsEntry("code", "NOT_FOUND");
	}
}
