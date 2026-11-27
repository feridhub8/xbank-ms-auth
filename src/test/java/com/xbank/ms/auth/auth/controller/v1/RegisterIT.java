package com.xbank.ms.auth.auth.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.xbank.ms.auth.support.TestKeycloak;
import com.xbank.ms.auth.support.TestUsers;
import com.xbank.ms.auth.support.TestVault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/** specs/user-auth over real HTTP, against the test Vault and the test Keycloak. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class RegisterIT {

	private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};

	static {
		TestVault.start();
		TestKeycloak.start();
	}

	/**
	 * Gives this test its own application context pointing at the test Keycloak. Without it, Spring reuses a
	 * cached context from an earlier test whose Keycloak URL is the default, http://localhost:8180, which is
	 * the real local Keycloak.
	 */
	@DynamicPropertySource
	static void keycloak(DynamicPropertyRegistry registry) {
		registry.add("xbank.keycloak.url", TestKeycloak::url);
	}

	@LocalServerPort
	private int port;

	private RestClient api;

	@BeforeEach
	void setUp() {
		api = RestClient.builder()
				.baseUrl("http://localhost:" + port + "/api/v1/auth")
				.defaultStatusHandler(status -> true, (request, response) -> { })
				.build();
	}

	@Test
	void successfulRegistrationReturnsProfileWithUserRole() {
		Map<String, Object> user = newUser();

		ResponseEntity<Map<String, Object>> response = register(user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody())
				.containsEntry("fin", user.get("fin"))
				.containsEntry("email", user.get("email"))
				.containsEntry("firstName", "Test")
				.containsEntry("lastName", "User")
				.containsEntry("phoneNumber", user.get("phoneNumber"))
				.containsEntry("roles", List.of("USER"))
				.doesNotContainKeys("password", "accessToken", "refreshToken");
	}

	@Test
	void finIsReturnedInUppercase() {
		Map<String, Object> user = newUser();
		user.put("fin", "AB" + TestUsers.fin().substring(2));

		assertThat(register(user).getBody()).containsEntry("fin", user.get("fin"));
	}

	@Test
	void severalInvalidFieldsAreAllListed() {
		Map<String, Object> user = newUser();
		user.put("fin", "ab12");
		user.put("phoneNumber", "0501234567");

		ResponseEntity<Map<String, Object>> response = register(user);

		assertProblem(response, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
		assertThat(errorFields(response)).containsExactlyInAnyOrder("fin", "phoneNumber");
	}

	@Test
	void weakPasswordIsAValidationError() {
		Map<String, Object> user = newUser();
		user.put("password", "password");

		ResponseEntity<Map<String, Object>> response = register(user);

		assertProblem(response, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
		assertThat(errorFields(response)).containsExactly("password");
	}

	@Test
	void duplicateFinIsRejected() {
		Map<String, Object> first = registered();
		Map<String, Object> second = newUser();
		second.put("fin", first.get("fin"));

		assertProblem(register(second), HttpStatus.CONFLICT, "FIN_ALREADY_EXISTS");
	}

	@Test
	void duplicateEmailIsRejected() {
		Map<String, Object> first = registered();
		Map<String, Object> second = newUser();
		second.put("email", first.get("email"));

		assertProblem(register(second), HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
	}

	@Test
	void duplicatePhoneIsRejected() {
		Map<String, Object> first = registered();
		Map<String, Object> second = newUser();
		second.put("phoneNumber", first.get("phoneNumber"));

		assertProblem(register(second), HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS");
	}

	@Test
	void concurrentRegistrationsWithSamePhoneLeaveExactlyOneUser() throws Exception {
		for (int round = 0; round < 3; round++) {
			Map<String, Object> a = newUser();
			Map<String, Object> b = newUser();
			b.put("phoneNumber", a.get("phoneNumber"));

			List<ResponseEntity<Map<String, Object>>> responses = concurrently(() -> register(a), () -> register(b));

			assertThat(responses).extracting(r -> HttpStatus.valueOf(r.getStatusCode().value()))
					.containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
			responses.stream().filter(r -> r.getStatusCode() == HttpStatus.CONFLICT)
					.forEach(r -> assertProblem(r, HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS"));
			assertThat(usersWithPhone((String) a.get("phoneNumber"))).isEqualTo(1);
		}
	}

	// ---- helpers ---------------------------------------------------------------------------

	private ResponseEntity<Map<String, Object>> register(Map<String, Object> user) {
		return api.post().uri("/register").contentType(MediaType.APPLICATION_JSON).body(user).retrieve().toEntity(JSON);
	}

	private Map<String, Object> registered() {
		Map<String, Object> user = newUser();
		assertThat(register(user).getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return user;
	}

	private static Map<String, Object> newUser() {
		String fin = TestUsers.fin();
		return new HashMap<>(Map.of(
				"fin", fin,
				"email", "user-" + fin.toLowerCase() + "@xbank.test",
				"password", "Passw0rd",
				"firstName", "Test",
				"lastName", "User",
				"phoneNumber", TestUsers.phone()));
	}

	private static int usersWithPhone(String phone) {
		List<?> users = RestClient.create(TestKeycloak.url()).get()
				.uri(uri -> uri.path("/admin/realms/{realm}/users").queryParam("q", "{q}").queryParam("exact", true)
						.build(TestKeycloak.REALM, "phoneNumber:" + phone))
				.headers(h -> h.setBearerAuth(TestKeycloak.adminToken()))
				.retrieve()
				.body(List.class);
		return users.size();
	}

	private static void assertProblem(ResponseEntity<Map<String, Object>> response, HttpStatus status, String code) {
		assertThat(response.getStatusCode()).isEqualTo(status);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody()).containsEntry("status", status.value()).containsEntry("code", code);
	}

	@SuppressWarnings("unchecked")
	private static List<String> errorFields(ResponseEntity<Map<String, Object>> response) {
		List<Map<String, Object>> errors = (List<Map<String, Object>>) response.getBody().get("errors");
		return errors.stream().map(error -> (String) error.get("field")).toList();
	}

	@SafeVarargs
	private static <T> List<T> concurrently(Callable<T>... tasks) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService executor = Executors.newFixedThreadPool(tasks.length)) {
			List<Future<T>> futures = new ArrayList<>();
			for (Callable<T> task : tasks) {
				futures.add(executor.submit(() -> {
					start.await();
					return task.call();
				}));
			}
			start.countDown();
			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) {
				results.add(future.get());
			}
			return results;
		}
	}
}
