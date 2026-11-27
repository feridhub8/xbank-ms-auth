package com.xbank.ms.auth.auth.client.v1;

import com.xbank.ms.auth.auth.exception.v1.EmailAlreadyExistsException;
import com.xbank.ms.auth.auth.exception.v1.FinAlreadyExistsException;
import com.xbank.ms.auth.auth.exception.v1.PasswordRejectedException;
import com.xbank.ms.auth.common.config.v1.KeycloakProperties;
import com.xbank.ms.auth.common.exception.v1.UpstreamUnavailableException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Calls Keycloak's admin users API for the current realm, with the client's service account. */
@Component
public class KeycloakClient {

	private static final String UPSTREAM = "Identity provider";
	private static final ParameterizedTypeReference<List<KeycloakUser>> USERS = new ParameterizedTypeReference<>() {};
	private static final ParameterizedTypeReference<List<Map<String, Object>>> ROLES = new ParameterizedTypeReference<>() {};

	private final RestClient restClient;
	private final KeycloakProperties properties;

	private String serviceToken;
	private Instant serviceTokenExpiresAt = Instant.EPOCH;

	public KeycloakClient(@Qualifier("keycloakRestClient") RestClient restClient, KeycloakProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	/** Creates the user and returns its id. Duplicate FIN or email, and weak passwords, become domain errors. */
	public String createUser(KeycloakUser user) {
		URI location = call(() -> restClient.post()
				.uri("/admin/realms/{realm}/users", properties.getRealm())
				.headers(h -> h.setBearerAuth(serviceToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(user)
				.retrieve()
				.toBodilessEntity()
				.getHeaders()
				.getLocation(), KeycloakClient::createUserError);
		String path = location.getPath();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	/** Users whose phoneNumber attribute is exactly this value. */
	public List<KeycloakUser> findByPhone(String phoneNumber) {
		return call(() -> restClient.get()
				.uri(uri -> uri.path("/admin/realms/{realm}/users")
						.queryParam("q", "{q}")
						.queryParam("exact", true)
						.queryParam("briefRepresentation", false)
						.build(properties.getRealm(), KeycloakUser.PHONE_ATTRIBUTE + ":" + phoneNumber))
				.headers(h -> h.setBearerAuth(serviceToken()))
				.retrieve()
				.body(USERS), null);
	}

	public KeycloakUser getUser(String id) {
		return call(() -> restClient.get()
				.uri("/admin/realms/{realm}/users/{id}", properties.getRealm(), id)
				.headers(h -> h.setBearerAuth(serviceToken()))
				.retrieve()
				.body(KeycloakUser.class), null);
	}

	public void deleteUser(String id) {
		call(() -> restClient.delete()
				.uri("/admin/realms/{realm}/users/{id}", properties.getRealm(), id)
				.headers(h -> h.setBearerAuth(serviceToken()))
				.retrieve()
				.toBodilessEntity(), null);
	}

	/** Effective realm role names of the user, including those from composite (default) roles. */
	public List<String> realmRoles(String id) {
		List<Map<String, Object>> roles = call(() -> restClient.get()
				.uri("/admin/realms/{realm}/users/{id}/role-mappings/realm/composite", properties.getRealm(), id)
				.headers(h -> h.setBearerAuth(serviceToken()))
				.retrieve()
				.body(ROLES), null);
		return roles == null ? List.of() : roles.stream().map(role -> String.valueOf(role.get("name"))).toList();
	}

	/** Service-account token (client credentials), cached until 30 seconds before it expires. */
	private synchronized String serviceToken() {
		if (serviceToken == null || Instant.now().isAfter(serviceTokenExpiresAt)) {
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("grant_type", "client_credentials");
			form.add("client_id", properties.getClientId());
			form.add("client_secret", properties.getClientSecret());
			KeycloakTokens tokens = call(() -> restClient.post()
					.uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(KeycloakTokens.class), null);
			serviceToken = tokens.getAccessToken();
			serviceTokenExpiresAt = Instant.now().plusSeconds(Math.max(tokens.getExpiresIn() - 30, 0));
		}
		return serviceToken;
	}

	private static RuntimeException createUserError(HttpClientErrorException ex) {
		String body = ex.getResponseBodyAsString().toLowerCase(Locale.ROOT);
		if (ex.getStatusCode() == HttpStatus.CONFLICT) {
			return body.contains("email") ? new EmailAlreadyExistsException() : new FinAlreadyExistsException();
		}
		if (ex.getStatusCode() == HttpStatus.BAD_REQUEST && body.contains("password")) {
			return new PasswordRejectedException();
		}
		return null;
	}

	/**
	 * Runs one Keycloak call. Unreachable Keycloak or a 5xx becomes UPSTREAM_UNAVAILABLE. A 4xx goes through
	 * {@code onClientError}; anything it does not map is unexpected and ends up as INTERNAL_ERROR.
	 */
	private static <T> T call(Supplier<T> request, ClientErrorMapper onClientError) {
		try {
			return request.get();
		}
		catch (ResourceAccessException | HttpServerErrorException ex) {
			throw new UpstreamUnavailableException(UPSTREAM);
		}
		catch (HttpClientErrorException ex) {
			RuntimeException mapped = onClientError == null ? null : onClientError.map(ex);
			if (mapped != null) {
				throw mapped;
			}
			throw new IllegalStateException("Unexpected Keycloak response " + ex.getStatusCode(), ex);
		}
	}

	@FunctionalInterface
	private interface ClientErrorMapper {
		RuntimeException map(HttpClientErrorException ex);
	}
}
