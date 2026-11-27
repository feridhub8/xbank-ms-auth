package com.xbank.ms.auth.auth.client.v1;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xbank.ms.auth.common.config.v1.KeycloakProperties;
import com.xbank.ms.auth.common.config.v1.RestClientConfig;
import com.xbank.ms.auth.common.exception.v1.UpstreamUnavailableException;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** specs/user-auth "Identity provider unavailable": nothing listens on the Keycloak port. */
class KeycloakUnavailableTest {

	@Test
	void unreachableKeycloakIsUpstreamUnavailable() throws IOException {
		KeycloakProperties properties = new KeycloakProperties();
		properties.setUrl("http://127.0.0.1:" + freePort());
		properties.setRealm("xbankDev");
		properties.setClientId("xbank-ms-auth");
		properties.setClientSecret("unused");
		properties.setConnectTimeout(Duration.ofMillis(500));
		KeycloakClient client = new KeycloakClient(RestClientConfig.keycloakRestClient(properties), properties);

		assertThatThrownBy(() -> client.findByPhone("+994501234567"))
				.isInstanceOf(UpstreamUnavailableException.class);
		assertThatThrownBy(() -> client.createUser(KeycloakUser.builder().username("AB12C3D").credentials(List.of()).build()))
				.isInstanceOf(UpstreamUnavailableException.class);
	}

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}
}
