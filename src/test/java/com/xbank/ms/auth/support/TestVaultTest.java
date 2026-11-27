package com.xbank.ms.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/** The test Vault is set up like the real one: AppRole ms-auth-local reads its own path. */
class TestVaultTest {

	@Test
	@SuppressWarnings("unchecked")
	void appRoleReadsItsSecret() {
		TestVault.start();
		RestClient vault = RestClient.create(TestVault.url() + "/v1");

		Map<String, Object> login = vault.post().uri("/auth/approle/login")
				.body(Map.of("role_id", System.getProperty("VAULT_ROLE_ID"),
						"secret_id", System.getProperty("VAULT_SECRET_ID")))
				.retrieve().body(new ParameterizedTypeReference<>() {});
		String token = (String) ((Map<String, Object>) login.get("auth")).get("client_token");

		Map<String, Object> secret = vault.get().uri("/secret/data/xbank/local/ms-auth")
				.header("X-Vault-Token", token)
				.retrieve().body(new ParameterizedTypeReference<>() {});
		Map<String, Object> data = (Map<String, Object>) ((Map<String, Object>) secret.get("data")).get("data");

		assertThat(data).containsEntry("keycloak-client-secret", TestVault.CLIENT_SECRET);
	}
}
