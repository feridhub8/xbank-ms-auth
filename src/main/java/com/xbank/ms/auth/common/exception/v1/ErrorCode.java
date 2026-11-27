package com.xbank.ms.auth.common.exception.v1;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** Every error code the API returns, with its HTTP status and English title. Features add their own. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "Not found"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),
	UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Upstream service unavailable"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");

	private final HttpStatus status;
	private final String title;
}
