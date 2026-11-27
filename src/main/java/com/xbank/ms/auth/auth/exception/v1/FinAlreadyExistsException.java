package com.xbank.ms.auth.auth.exception.v1;

import com.xbank.ms.auth.common.exception.v1.ApiException;
import com.xbank.ms.auth.common.exception.v1.ErrorCode;

public class FinAlreadyExistsException extends ApiException {

	public FinAlreadyExistsException() {
		super(ErrorCode.FIN_ALREADY_EXISTS, "A user with this FIN already exists");
	}
}
