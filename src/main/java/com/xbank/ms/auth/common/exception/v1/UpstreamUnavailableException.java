package com.xbank.ms.auth.common.exception.v1;

/** A service the request depends on could not be reached. */
public class UpstreamUnavailableException extends ApiException {

	public UpstreamUnavailableException(String upstream) {
		super(ErrorCode.UPSTREAM_UNAVAILABLE, upstream + " is not reachable");
	}
}
