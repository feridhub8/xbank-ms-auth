package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.xbank.ms.auth.support.TestVault;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * specs/service-health: the image runs dev and prod on container port 8080 as a non-root user.
 * Vault shares a network with the image under the alias "vault", as in compose.
 */
class ContainerIT {

	/** Built once from the project's Dockerfile and reused by every run. */
	private static final ImageFromDockerfile IMAGE = new ImageFromDockerfile("xbank-ms-auth-test", false)
			.withFileFromPath(".", Path.of("."));

	@ParameterizedTest
	@ValueSource(strings = { "dev", "prod" })
	void imageRunsProfileOnPort8080AsNonRoot(String profile) throws Exception {
		TestVault.start();
		try (GenericContainer<?> container = new GenericContainer<>(IMAGE)
				.withNetwork(TestVault.NETWORK)
				.withEnv("SPRING_PROFILES_ACTIVE", profile)
				.withEnv("VAULT_ROLE_ID", TestVault.roleId(profile))
				.withEnv("VAULT_SECRET_ID", TestVault.newSecretId(profile))
				.withExposedPorts(8080)
				.waitingFor(Wait.forHttp("/actuator/health").forPort(8080).forStatusCode(200))) {
			container.start();

			Map<String, Object> health = RestClient.create("http://" + container.getHost() + ":" + container.getMappedPort(8080))
					.get().uri("/actuator/health")
					.retrieve()
					.body(new ParameterizedTypeReference<>() {});
			Container.ExecResult user = container.execInContainer("id", "-un");

			assertThat(health).containsEntry("status", "UP");
			assertThat(user.getStdout().trim()).isNotEqualTo("root");
		}
	}
}
