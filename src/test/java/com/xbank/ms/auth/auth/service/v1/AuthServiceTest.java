package com.xbank.ms.auth.auth.service.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xbank.ms.auth.auth.client.v1.KeycloakClient;
import com.xbank.ms.auth.auth.client.v1.KeycloakUser;
import com.xbank.ms.auth.auth.dto.v1.RegisterRequest;
import com.xbank.ms.auth.auth.dto.v1.UserResponse;
import com.xbank.ms.auth.auth.exception.v1.PhoneAlreadyExistsException;
import com.xbank.ms.auth.auth.mapper.v1.UserMapperImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The registration flow with a mocked Keycloak client. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String PHONE = "+994501234567";

	@Mock
	private KeycloakClient keycloakClient;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(keycloakClient, new UserMapperImpl());
	}

	@Test
	void knownPhoneIsRejectedBeforeCreating() {
		when(keycloakClient.findByPhone(PHONE)).thenReturn(List.of(user("existing", 1L)));

		assertThatThrownBy(() -> authService.register(request())).isInstanceOf(PhoneAlreadyExistsException.class);
		verify(keycloakClient, never()).createUser(any());
	}

	@Test
	void newUserIsDeletedWhenAnOlderUserGotTheSamePhoneMeanwhile() {
		when(keycloakClient.findByPhone(PHONE))
				.thenReturn(List.of())
				.thenReturn(List.of(user("older", 1L), user("new", 2L)));
		when(keycloakClient.createUser(any())).thenReturn("new");

		assertThatThrownBy(() -> authService.register(request())).isInstanceOf(PhoneAlreadyExistsException.class);
		verify(keycloakClient).deleteUser("new");
	}

	@Test
	void newUserStaysWhenItIsTheOldestWithThatPhone() {
		when(keycloakClient.findByPhone(PHONE))
				.thenReturn(List.of())
				.thenReturn(List.of(user("new", 1L), user("later", 2L)));
		when(keycloakClient.createUser(any())).thenReturn("new");
		when(keycloakClient.getUser("new")).thenReturn(user("new", 1L));
		when(keycloakClient.realmRoles("new")).thenReturn(List.of("default-roles-xbankdev", "offline_access", "USER"));

		UserResponse response = authService.register(request());

		verify(keycloakClient, never()).deleteUser(any());
		assertThat(response.getFin()).isEqualTo("AB12C3D");
		assertThat(response.getEmail()).isEqualTo("user@xbank.test");
		assertThat(response.getPhoneNumber()).isEqualTo(PHONE);
		assertThat(response.getRoles()).containsExactly("USER");
	}

	@Test
	void keycloakUserHasFinAsUsernamePhoneAttributeAndPassword() {
		when(keycloakClient.findByPhone(PHONE)).thenReturn(List.of()).thenReturn(List.of(user("new", 1L)));
		when(keycloakClient.createUser(any())).thenReturn("new");
		when(keycloakClient.getUser("new")).thenReturn(user("new", 1L));
		when(keycloakClient.realmRoles("new")).thenReturn(List.of("USER"));

		authService.register(request());

		ArgumentCaptor<KeycloakUser> sent = ArgumentCaptor.forClass(KeycloakUser.class);
		verify(keycloakClient).createUser(sent.capture());
		assertThat(sent.getValue().getUsername()).isEqualTo("AB12C3D");
		assertThat(sent.getValue().getEnabled()).isTrue();
		assertThat(sent.getValue().phoneNumber()).isEqualTo(PHONE);
		assertThat(sent.getValue().getCredentials()).singleElement().satisfies(credential -> {
			assertThat(credential.getValue()).isEqualTo("Passw0rd");
			assertThat(credential.getTemporary()).isFalse();
		});
	}

	private static RegisterRequest request() {
		return new RegisterRequest("AB12C3D", "user@xbank.test", "Passw0rd", "Test", "User", PHONE);
	}

	private static KeycloakUser user(String id, long created) {
		return KeycloakUser.builder()
				.id(id)
				.username("ab12c3d")
				.email("user@xbank.test")
				.firstName("Test")
				.lastName("User")
				.createdTimestamp(created)
				.attributes(Map.of(KeycloakUser.PHONE_ATTRIBUTE, List.of(PHONE)))
				.build();
	}
}
