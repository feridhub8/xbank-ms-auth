package com.xbank.ms.auth.common.config.v1;

import jakarta.validation.constraints.NotBlank;
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

	@NotBlank
	private String clientSecret;
}
