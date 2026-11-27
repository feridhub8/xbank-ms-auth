package com.xbank.ms.auth.auth.client.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Keycloak token endpoint response. */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakTokens {

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("expires_in")
	private long expiresIn;
}
