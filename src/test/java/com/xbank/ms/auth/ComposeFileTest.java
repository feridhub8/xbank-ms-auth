package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.yaml.snakeyaml.Yaml;

/** specs/service-health: dev and prod compose files (profile, host port, project name, network). */
class ComposeFileTest {

	@ParameterizedTest
	@CsvSource({ "docker-compose.dev.yml, dev, 9101, .env.dev", "docker-compose.yml, prod, 9201, .env.prod" })
	@SuppressWarnings("unchecked")
	void composeFileRunsProfileOnItsPort(String file, String profile, String hostPort, String envFile) throws IOException {
		Map<String, Object> compose = read(file);
		Map<String, Map<String, Object>> services = (Map<String, Map<String, Object>>) compose.get("services");
		Map<String, Object> networks = (Map<String, Object>) compose.get("networks");

		assertThat(compose).containsEntry("name", "xbank");
		assertThat(services).hasSize(1);
		Map<String, Object> service = services.values().iterator().next();

		assertThat(service).doesNotContainKey("container_name");
		assertThat(service).containsEntry("env_file", envFile);
		assertThat((Map<String, Object>) service.get("environment")).containsEntry("SPRING_PROFILES_ACTIVE", profile);
		assertThat((List<String>) service.get("ports")).containsExactly(hostPort + ":8080");
		assertThat((List<String>) service.get("networks")).containsExactly("xbank_network");
		assertThat((Map<String, Object>) networks.get("xbank_network")).containsEntry("external", true);
	}

	private static Map<String, Object> read(String file) throws IOException {
		try (Reader reader = Files.newBufferedReader(Path.of(file))) {
			return new Yaml().load(reader);
		}
	}
}
