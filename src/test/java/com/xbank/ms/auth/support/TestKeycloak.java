package com.xbank.ms.auth.support;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * One Keycloak container per test run, importing the real xbankDev realm from xbank-infrastructure.
 * The xbank-ms-auth client secret is set to {@link TestVault#CLIENT_SECRET}, so Vault and Keycloak agree.
 */
public final class TestKeycloak {

	public static final String REALM = "xbankDev";
	public static final String CLIENT_ID = "xbank-ms-auth";

	private static final KeycloakContainer KEYCLOAK = new KeycloakContainer("keycloak/keycloak:26.8.2")
			.withRealmImportFile("infrastructure/realms/" + REALM + ".json");

	private static boolean started;

	private TestKeycloak() {
	}

	/**
	 * Starts Keycloak once and sets the client secret. Tests that need it point the app at it with
	 * {@code @DynamicPropertySource} ({@code xbank.keycloak.url} = {@link #url()}).
	 */
	public static synchronized void start() {
		if (!started) {
			KEYCLOAK.start();
			setClientSecret(TestVault.CLIENT_SECRET);
			started = true;
		}
	}

	public static String url() {
		String url = KEYCLOAK.getAuthServerUrl();
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	/** Admin token of the master realm, for tests that inspect or prepare Keycloak directly. */
	public static String adminToken() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "password");
		form.add("client_id", "admin-cli");
		form.add("username", KEYCLOAK.getAdminUsername());
		form.add("password", KEYCLOAK.getAdminPassword());
		Map<String, Object> token = RestClient.create(url()).post()
				.uri("/realms/master/protocol/openid-connect/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});
		return String.valueOf(token.get("access_token"));
	}

	private static void setClientSecret(String secret) {
		String admin = adminToken();
		RestClient client = RestClient.create(url());
		List<Map<String, Object>> clients = client.get()
				.uri("/admin/realms/{realm}/clients?clientId={clientId}", REALM, CLIENT_ID)
				.headers(h -> h.setBearerAuth(admin))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});
		Map<String, Object> representation = clients.getFirst();
		representation.put("secret", secret);
		client.put()
				.uri("/admin/realms/{realm}/clients/{id}", REALM, representation.get("id"))
				.headers(h -> h.setBearerAuth(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.body(representation)
				.retrieve()
				.toBodilessEntity();
	}
}
