package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xbank.ms.auth.common.config.v1.KeycloakProperties;
import com.xbank.ms.auth.support.TestVault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** specs/secrets-store: the secret is loaded at startup, and the service does not start without it. */
class VaultSecretsIT {

	@BeforeEach
	void setUp() {
		TestVault.start();
	}

	@AfterEach
	void restore() {
		TestVault.vault("vault kv put secret/xbank/local/ms-auth keycloak-client-secret=" + TestVault.CLIENT_SECRET);
		TestVault.useAppRole("local");
	}

	@Test
	void secretIsLoadedIntoKeycloakProperties() {
		try (ConfigurableApplicationContext context = startLocal()) {
			assertThat(context.getBean(KeycloakProperties.class).getClientSecret()).isEqualTo(TestVault.CLIENT_SECRET);
		}
	}

	@Test
	void wrongSecretIdStopsStartup() {
		System.setProperty("VAULT_SECRET_ID", "not-a-valid-secret-id");

		assertThatThrownBy(this::startLocal).isInstanceOf(Exception.class);
	}

	@Test
	void missingSecretStopsStartup() {
		TestVault.vault("vault kv put secret/xbank/local/ms-auth some-other-key=value");

		assertThatThrownBy(this::startLocal).isInstanceOf(Exception.class);
	}

	private ConfigurableApplicationContext startLocal() {
		return new SpringApplicationBuilder(AuthApplication.class)
				.web(WebApplicationType.NONE)
				.profiles("local")
				.run();
	}
}
