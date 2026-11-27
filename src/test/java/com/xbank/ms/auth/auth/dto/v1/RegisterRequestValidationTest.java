package com.xbank.ms.auth.auth.dto.v1;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** specs/user-auth "Registration input validation", rule by rule. */
class RegisterRequestValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void validRequestHasNoViolations() {
		assertThat(invalidFields(valid())).isEmpty();
	}

	@Test
	void finMustBeSevenUppercaseLettersOrDigits() {
		for (String fin : new String[] { "ab12", "ab12c3d", "AB12C3", "AB12C3DE", "AB-2C3D" }) {
			RegisterRequest request = valid();
			request.setFin(fin);
			assertThat(invalidFields(request)).as(fin).containsExactly("fin");
		}
	}

	@Test
	void phoneMustBePlus994AndNineDigits() {
		for (String phone : new String[] { "0501234567", "+99450123456", "+9945012345678", "+994 501234567" }) {
			RegisterRequest request = valid();
			request.setPhoneNumber(phone);
			assertThat(invalidFields(request)).as(phone).containsExactly("phoneNumber");
		}
	}

	@Test
	void passwordMustMeetThePolicy() {
		for (String password : new String[] { "password", "PASSWORD1", "password1", "Password", "Pass1" }) {
			RegisterRequest request = valid();
			request.setPassword(password);
			assertThat(invalidFields(request)).as(password).containsExactly("password");
		}
	}

	@Test
	void emailAndNamesAreRequired() {
		RegisterRequest request = valid();
		request.setEmail("not-an-email");
		request.setFirstName(" ");
		request.setLastName(null);
		assertThat(invalidFields(request)).containsExactlyInAnyOrder("email", "firstName", "lastName");
	}

	private Set<String> invalidFields(RegisterRequest request) {
		return validator.validate(request).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	private static RegisterRequest valid() {
		return new RegisterRequest("AB12C3D", "user@xbank.test", "Passw0rd", "Test", "User", "+994501234567");
	}
}
