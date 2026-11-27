package com.xbank.ms.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.xbank.ms.auth.AuthApplication;
import com.xbank.ms.auth.common.config.v1.KeycloakProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Tests never talk to the real local Keycloak (http://localhost:8180) by accident. */
class RealKeycloakGuardTest {

	@Test
	void appStartedByTestsPointsKeycloakNowhere() {
		TestVault.start();
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AuthApplication.class)
				.web(WebApplicationType.NONE)
				.profiles("local")
				.run()) {
			assertThat(context.getBean(KeycloakProperties.class).getUrl()).isEqualTo(TestVault.NO_KEYCLOAK);
		}
	}
}
