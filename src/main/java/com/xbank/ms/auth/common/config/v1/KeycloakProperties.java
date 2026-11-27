package com.xbank.ms.auth.common.config.v1;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Settings for calling Keycloak. The client secret comes from Vault (key keycloak-client-secret). */
@Getter
@Setter
@Validated
@ConfigurationProperties("xbank.keycloak")
public class KeycloakProperties {

	/** Base URL, for example http://localhost:8180. */
	@NotBlank
	private String url;

	/** Realm of the current environment: xbankDev (local, dev) or xbank (prod). */
	@NotBlank
	private String realm;

	@NotBlank
	private String clientId;

	@NotBlank
	private String clientSecret;

	private Duration connectTimeout = Duration.ofSeconds(3);

	private Duration readTimeout = Duration.ofSeconds(10);
}
