package com.xbank.ms.auth.auth.exception.v1;

import com.xbank.ms.auth.common.exception.v1.ApiException;
import com.xbank.ms.auth.common.exception.v1.ErrorCode;

public class PhoneAlreadyExistsException extends ApiException {

	public PhoneAlreadyExistsException() {
		super(ErrorCode.PHONE_ALREADY_EXISTS, "A user with this phone number already exists");
	}
}
