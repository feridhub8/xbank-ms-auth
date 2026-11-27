package com.xbank.ms.auth.auth.mapper.v1;

import com.xbank.ms.auth.auth.client.v1.KeycloakUser;
import com.xbank.ms.auth.auth.dto.v1.RegisterRequest;
import com.xbank.ms.auth.auth.dto.v1.UserResponse;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = { List.class, Map.class, KeycloakUser.class })
public interface UserMapper {

	/** The new Keycloak user: FIN as username, phone as attribute, password as a non-temporary credential. */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdTimestamp", ignore = true)
	@Mapping(target = "username", source = "fin")
	@Mapping(target = "enabled", constant = "true")
	@Mapping(target = "attributes", expression = "java(Map.of(KeycloakUser.PHONE_ATTRIBUTE, List.of(request.getPhoneNumber())))")
	@Mapping(target = "credentials", expression = "java(List.of(new KeycloakUser.Credential(\"password\", request.getPassword(), false)))")
	KeycloakUser toKeycloakUser(RegisterRequest request);

	/** Keycloak stores usernames in lowercase; the FIN is always returned in uppercase. */
	@Mapping(target = "fin", expression = "java(user.getUsername() == null ? null : user.getUsername().toUpperCase(java.util.Locale.ROOT))")
	@Mapping(target = "phoneNumber", expression = "java(user.phoneNumber())")
	UserResponse toResponse(KeycloakUser user, List<String> roles);
}
