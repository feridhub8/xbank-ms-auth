package com.xbank.ms.auth.auth.client.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xbank.ms.auth.auth.exception.v1.EmailAlreadyExistsException;
import com.xbank.ms.auth.auth.exception.v1.FinAlreadyExistsException;
import com.xbank.ms.auth.auth.exception.v1.PasswordRejectedException;
import com.xbank.ms.auth.common.config.v1.KeycloakProperties;
import com.xbank.ms.auth.common.config.v1.RestClientConfig;
import com.xbank.ms.auth.support.TestKeycloak;
import com.xbank.ms.auth.support.TestUsers;
import com.xbank.ms.auth.support.TestVault;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** KeycloakClient against the test Keycloak with the real xbankDev realm. */
class KeycloakClientIT {

	private static KeycloakClient client;

	@BeforeAll
	static void setUp() {
		TestKeycloak.start();
		KeycloakProperties properties = new KeycloakProperties();
		properties.setUrl(TestKeycloak.url());
		properties.setRealm(TestKeycloak.REALM);
		properties.setClientId(TestKeycloak.CLIENT_ID);
		properties.setClientSecret(TestVault.CLIENT_SECRET);
		client = new KeycloakClient(RestClientConfig.keycloakRestClient(properties), properties);
	}

	@Test
	void createReadFindAndDeleteUser() {
		String fin = TestUsers.fin();
		String phone = TestUsers.phone();
		String id = client.createUser(user(fin, fin.toLowerCase() + "@xbank.test", phone, "Passw0rd"));

		KeycloakUser stored = client.getUser(id);
		assertThat(stored.getUsername()).isEqualTo(fin.toLowerCase());
		assertThat(stored.phoneNumber()).isEqualTo(phone);
		assertThat(stored.getCreatedTimestamp()).isPositive();
		assertThat(client.findByPhone(phone)).extracting(KeycloakUser::getId).containsExactly(id);
		assertThat(client.realmRoles(id)).contains("USER").doesNotContain("ADMIN");

		client.deleteUser(id);
		assertThat(client.findByPhone(phone)).isEmpty();
	}

	@Test
	void phoneSearchIsExact() {
		String phone = TestUsers.phone();
		String fin = TestUsers.fin();
		String id = client.createUser(user(fin, fin.toLowerCase() + "@xbank.test", phone, "Passw0rd"));

		assertThat(client.findByPhone(phone.substring(0, phone.length() - 1))).isEmpty();
		assertThat(client.findByPhone(phone)).extracting(KeycloakUser::getId).containsExactly(id);
	}

	@Test
	void duplicateFinAndEmailAreMapped() {
		String fin = TestUsers.fin();
		String email = fin.toLowerCase() + "@xbank.test";
		client.createUser(user(fin, email, TestUsers.phone(), "Passw0rd"));

		assertThatThrownBy(() -> client.createUser(user(fin, "other-" + email, TestUsers.phone(), "Passw0rd")))
				.isInstanceOf(FinAlreadyExistsException.class);
		assertThatThrownBy(() -> client.createUser(user(TestUsers.fin(), email, TestUsers.phone(), "Passw0rd")))
				.isInstanceOf(EmailAlreadyExistsException.class);
	}

	@Test
	void weakPasswordIsRejected() {
		String fin = TestUsers.fin();
		assertThatThrownBy(() -> client.createUser(user(fin, fin.toLowerCase() + "@xbank.test", TestUsers.phone(), "password")))
				.isInstanceOf(PasswordRejectedException.class);
	}

	private static KeycloakUser user(String fin, String email, String phone, String password) {
		return KeycloakUser.builder()
				.username(fin)
				.email(email)
				.firstName("Test")
				.lastName("User")
				.enabled(true)
				.attributes(Map.of(KeycloakUser.PHONE_ATTRIBUTE, List.of(phone)))
				.credentials(List.of(new KeycloakUser.Credential("password", password, false)))
				.build();
	}
}
