package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.xbank.ms.auth.support.TestVault;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/**
 * The local profile reads the AppRole credentials from ./.env, as with ./gradlew bootRun.
 * Runs the built jar as its own process in a folder that holds only a .env file, with no
 * VAULT_ROLE_ID or VAULT_SECRET_ID in its environment or system properties.
 */
class LocalEnvFileIT {

	@TempDir
	Path workDir;

	@Test
	void localProfileStartsWithCredentialsFromEnvFile() throws Exception {
		TestVault.start();
		Files.write(workDir.resolve(".env"), List.of(
				"VAULT_ROLE_ID=" + TestVault.roleId("local"),
				"VAULT_SECRET_ID=" + TestVault.newSecretId("local")));
		int port = freePort();

		ProcessBuilder builder = new ProcessBuilder(javaBinary(), "-jar", bootJar(),
				"--spring.profiles.active=local", "--server.port=" + port)
				.directory(workDir.toFile())
				.redirectErrorStream(true)
				.redirectOutput(workDir.resolve("app.log").toFile());
		builder.environment().remove("VAULT_ROLE_ID");
		builder.environment().remove("VAULT_SECRET_ID");
		builder.environment().put("VAULT_URL", TestVault.url());

		Process app = builder.start();
		try {
			assertThat(waitForHealth(port, app)).as(Files.readString(workDir.resolve("app.log"))).isEqualTo("UP");
		}
		finally {
			app.destroy();
			app.waitFor();
		}
	}

	private static String waitForHealth(int port, Process app) throws InterruptedException {
		RestClient client = RestClient.create("http://localhost:" + port);
		Instant deadline = Instant.now().plus(Duration.ofSeconds(60));
		while (Instant.now().isBefore(deadline) && app.isAlive()) {
			try {
				Map<String, Object> health = client.get().uri("/actuator/health").retrieve()
						.body(new ParameterizedTypeReference<>() {});
				return String.valueOf(health.get("status"));
			}
			catch (RuntimeException notUpYet) {
				Thread.sleep(500);
			}
		}
		return app.isAlive() ? "timeout" : "exited";
	}

	private static String bootJar() throws IOException {
		try (var jars = Files.list(Path.of("build/libs"))) {
			return jars.map(Path::toAbsolutePath).map(Path::toString)
					.filter(name -> name.endsWith("-SNAPSHOT.jar") && !name.endsWith("-plain.jar"))
					.findFirst().orElseThrow(() -> new IllegalStateException("bootJar has not been built"));
		}
	}

	private static String javaBinary() {
		return Path.of(System.getProperty("java.home"), "bin", "java").toString();
	}

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}
}
