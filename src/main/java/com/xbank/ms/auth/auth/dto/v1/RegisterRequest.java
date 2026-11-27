package com.xbank.ms.auth.auth.dto.v1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

	@NotBlank(message = "FIN is required")
	@Pattern(regexp = "^[A-Z0-9]{7}$", message = "FIN must be 7 uppercase letters or digits")
	private String fin;

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be a valid address")
	@Size(max = 255, message = "Email must be at most 255 characters")
	private String email;

	@NotBlank(message = "Password is required")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
			message = "Password must be at least 8 characters with an uppercase letter, a lowercase letter, and a digit")
	private String password;

	@NotBlank(message = "First name is required")
	@Size(max = 255, message = "First name must be at most 255 characters")
	private String firstName;

	@NotBlank(message = "Last name is required")
	@Size(max = 255, message = "Last name must be at most 255 characters")
	private String lastName;

	@NotBlank(message = "Phone number is required")
	@Pattern(regexp = "^\\+994[0-9]{9}$", message = "Phone must be +994 followed by 9 digits")
	private String phoneNumber;
}
