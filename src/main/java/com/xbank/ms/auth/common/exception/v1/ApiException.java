package com.xbank.ms.auth.common.exception.v1;

import java.util.List;
import lombok.Getter;

/** Base class for every exception that maps to a known error response. The message is the detail. */
@Getter
public class ApiException extends RuntimeException {

	private final ErrorCode errorCode;
	private final List<FieldViolation> violations;

	public ApiException(ErrorCode errorCode, String detail) {
		this(errorCode, detail, List.of());
	}

	public ApiException(ErrorCode errorCode, String detail, List<FieldViolation> violations) {
		super(detail);
		this.errorCode = errorCode;
		this.violations = List.copyOf(violations);
	}
}
