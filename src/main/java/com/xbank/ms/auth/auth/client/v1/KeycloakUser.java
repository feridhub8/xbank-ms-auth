package com.xbank.ms.auth.auth.client.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The part of Keycloak's UserRepresentation that ms-auth reads and writes. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakUser {

	public static final String PHONE_ATTRIBUTE = "phoneNumber";

	private String id;
	private String username;
	private String email;
	private String firstName;
	private String lastName;
	private Boolean enabled;
	private Long createdTimestamp;
	private Map<String, List<String>> attributes;
	private List<Credential> credentials;

	/** First value of the phoneNumber attribute, or null. */
	public String phoneNumber() {
		if (attributes == null) {
			return null;
		}
		List<String> values = attributes.get(PHONE_ATTRIBUTE);
		return values == null || values.isEmpty() ? null : values.getFirst();
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Credential {

		private String type;
		private String value;
		private Boolean temporary;
	}
}
