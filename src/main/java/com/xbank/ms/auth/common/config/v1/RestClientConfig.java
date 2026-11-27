package com.xbank.ms.auth.common.config.v1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	/** RestClient for Keycloak: base URL and timeouts from {@link KeycloakProperties}. */
	@Bean
	public RestClient keycloakRestClient(RestClient.Builder builder, KeycloakProperties properties) {
		return keycloakRestClient(builder.clone(), properties.getUrl(), properties);
	}

	static RestClient keycloakRestClient(RestClient.Builder builder, String baseUrl, KeycloakProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.getConnectTimeout());
		requestFactory.setReadTimeout(properties.getReadTimeout());
		return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
	}

	/** Same as the bean, for code and tests that build a client without a Spring context. */
	public static RestClient keycloakRestClient(KeycloakProperties properties) {
		return keycloakRestClient(RestClient.builder(), properties.getUrl(), properties);
	}
}
