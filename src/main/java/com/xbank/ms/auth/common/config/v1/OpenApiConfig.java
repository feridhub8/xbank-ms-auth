package com.xbank.ms.auth.common.config.v1;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI description served at /v3/api-docs and /swagger-ui.html (off in prod, see application-prod.yml). */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI().info(new Info()
				.title("XBank Auth API")
				.version("v1")
				.description("Registration and sign-in for XBank. Errors are ProblemDetail (application/problem+json) "
						+ "with a stable code field."));
	}
}
