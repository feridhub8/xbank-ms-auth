package com.xbank.ms.auth.auth.exception.v1;

import com.xbank.ms.auth.common.exception.v1.ApiException;
import com.xbank.ms.auth.common.exception.v1.ErrorCode;
import com.xbank.ms.auth.common.exception.v1.FieldViolation;
import java.util.List;

/** Keycloak's password policy rejected the password. Reported like any other invalid field. */
public class PasswordRejectedException extends ApiException {

	public PasswordRejectedException() {
		super(ErrorCode.VALIDATION_FAILED, "One or more fields are invalid", List.of(new FieldViolation("password",
				"Password must be at least 8 characters with an uppercase letter, a lowercase letter, and a digit")));
	}
}
