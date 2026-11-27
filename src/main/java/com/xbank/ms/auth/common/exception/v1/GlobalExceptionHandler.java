package com.xbank.ms.auth.common.exception.v1;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns exceptions into ProblemDetail (RFC 9457) responses with a stable {@code code}. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String TYPE_BASE = "https://xbank.local/errors/";

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ProblemDetail> handleApi(ApiException ex, HttpServletRequest request) {
		return build(ex.getErrorCode(), ex.getMessage(), ex.getViolations(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
				.toList();
		return build(ErrorCode.VALIDATION_FAILED, "One or more fields are invalid", violations, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return build(ErrorCode.MALFORMED_REQUEST, "The request body could not be read", List.of(), request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
		return build(ErrorCode.NOT_FOUND, "No resource at this path", List.of(), request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ProblemDetail> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
		return build(ErrorCode.METHOD_NOT_ALLOWED, "Method " + ex.getMethod() + " is not supported here", List.of(), request);
	}

	/** Anything unexpected: log it in full, but give the client only a generic message. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return build(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", List.of(), request);
	}

	private static ResponseEntity<ProblemDetail> build(ErrorCode code, String detail, List<FieldViolation> violations,
			HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.getStatus(), detail);
		problem.setTitle(code.getTitle());
		problem.setType(URI.create(TYPE_BASE + code.name().toLowerCase(Locale.ROOT).replace('_', '-')));
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", code.name());
		if (!violations.isEmpty()) {
			problem.setProperty("errors", violations);
		}
		return ResponseEntity.status(code.getStatus())
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problem);
	}
}
