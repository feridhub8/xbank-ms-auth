package com.xbank.ms.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** The test Keycloak's xbank-ms-auth client accepts the same secret the test Vault holds. */
class TestKeycloakTest {

	@Test
	void serviceAccountGetsTokenWithVaultSecret() {
		TestKeycloak.start();
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "client_credentials");
		form.add("client_id", TestKeycloak.CLIENT_ID);
		form.add("client_secret", TestVault.CLIENT_SECRET);

		Map<String, Object> token = RestClient.create(TestKeycloak.url()).post()
				.uri("/realms/{realm}/protocol/openid-connect/token", TestKeycloak.REALM)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});

		assertThat(token).containsKey("access_token");
	}
}
