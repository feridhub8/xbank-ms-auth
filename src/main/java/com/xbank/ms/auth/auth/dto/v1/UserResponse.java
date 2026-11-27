package com.xbank.ms.auth.auth.dto.v1;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

	private String fin;
	private String email;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private List<String> roles;
}
