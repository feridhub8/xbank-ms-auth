package com.xbank.ms.auth.support;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Fresh FINs and phone numbers, so tests that share one Keycloak never collide. */
public final class TestUsers {

	private TestUsers() {
	}

	/** 7 uppercase letters or digits. */
	public static String fin() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
	}

	/** +994 followed by 9 digits. */
	public static String phone() {
		return "+99450" + (1_000_000 + ThreadLocalRandom.current().nextInt(9_000_000));
	}
}
