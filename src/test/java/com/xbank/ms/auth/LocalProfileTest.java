package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/** specs/service-health: in the local profile ms-auth listens on 9001. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class LocalProfileTest {

	@Autowired
	private Environment environment;

	@Test
	void localProfileUsesPort9001() {
		assertThat(environment.getProperty("server.port")).isEqualTo("9001");
	}
}
