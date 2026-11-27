package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/** specs/service-health: port per profile (local on the host, dev and prod inside the container). */
class ProfilesTest {

	@ParameterizedTest
	@CsvSource({ "local, 9001", "dev, 8080", "prod, 8080" })
	void profileSetsPort(String profile, String port) {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AuthApplication.class)
				.web(WebApplicationType.NONE)
				.profiles(profile)
				.run()) {
			assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo(port);
		}
	}
}
