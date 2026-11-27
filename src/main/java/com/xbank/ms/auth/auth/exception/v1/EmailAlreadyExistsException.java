package com.xbank.ms.auth.auth.exception.v1;

import com.xbank.ms.auth.common.exception.v1.ApiException;
import com.xbank.ms.auth.common.exception.v1.ErrorCode;

public class EmailAlreadyExistsException extends ApiException {

	public EmailAlreadyExistsException() {
		super(ErrorCode.EMAIL_ALREADY_EXISTS, "A user with this email already exists");
	}
}
