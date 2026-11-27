package com.xbank.ms.auth.auth.service.v1;

import com.xbank.ms.auth.auth.client.v1.KeycloakClient;
import com.xbank.ms.auth.auth.client.v1.KeycloakUser;
import com.xbank.ms.auth.auth.dto.v1.RegisterRequest;
import com.xbank.ms.auth.auth.dto.v1.UserResponse;
import com.xbank.ms.auth.auth.exception.v1.PhoneAlreadyExistsException;
import com.xbank.ms.auth.auth.mapper.v1.UserMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	/** Only XBank's own roles are returned; Keycloak's built-in ones are not. */
	static final Set<String> XBANK_ROLES = Set.of("USER", "ADMIN");

	private static final Comparator<KeycloakUser> OLDEST_FIRST = Comparator
			.comparing((KeycloakUser user) -> user.getCreatedTimestamp() == null ? Long.MAX_VALUE : user.getCreatedTimestamp())
			.thenComparing(KeycloakUser::getId);

	private final KeycloakClient keycloakClient;
	private final UserMapper userMapper;

	/**
	 * FIN (username) and email are unique in Keycloak itself. Phone is a plain attribute, so it is checked
	 * before creating and again after: if two users ended up with the same phone, only the oldest one stays.
	 */
	public UserResponse register(RegisterRequest request) {
		String phone = request.getPhoneNumber();
		if (!keycloakClient.findByPhone(phone).isEmpty()) {
			throw new PhoneAlreadyExistsException();
		}

		String id = keycloakClient.createUser(userMapper.toKeycloakUser(request));

		List<KeycloakUser> samePhone = keycloakClient.findByPhone(phone);
		if (samePhone.size() > 1) {
			KeycloakUser keeper = samePhone.stream().min(OLDEST_FIRST).orElseThrow();
			if (!keeper.getId().equals(id)) {
				keycloakClient.deleteUser(id);
				throw new PhoneAlreadyExistsException();
			}
		}

		List<String> roles = keycloakClient.realmRoles(id).stream().filter(XBANK_ROLES::contains).sorted().toList();
		return userMapper.toResponse(keycloakClient.getUser(id), roles);
	}
}
