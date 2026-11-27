package com.xbank.ms.auth.common.exception.v1;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One invalid field in a request, listed under "errors" in the response. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldViolation {

	private String field;
	private String message;
}
