package com.xbank.ms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** specs/secrets-store: .env.example lists the AppRole keys with empty values. */
class EnvExampleTest {

	@Test
	void exampleListsAppRoleKeysWithoutValues() throws IOException {
		assertThat(Files.readAllLines(Path.of(".env.example"))).containsExactly("VAULT_ROLE_ID=", "VAULT_SECRET_ID=");
	}
}
