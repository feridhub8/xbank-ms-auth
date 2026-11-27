package com.xbank.ms.auth.support;

import java.io.IOException;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.vault.VaultContainer;

/**
 * One Vault container per test run, set up with the real init-auth.sh from xbank-infrastructure.
 * The service's secret is written to all three environment paths with {@link #CLIENT_SECRET}.
 */
public final class TestVault {

	public static final String CLIENT_SECRET = "test-client-secret";
	public static final Network NETWORK = Network.newNetwork();

	private static final String ROOT_TOKEN = "root";
	private static final VaultContainer<?> VAULT = new VaultContainer<>("hashicorp/vault:1.21.4")
			.withVaultToken(ROOT_TOKEN)
			.withNetwork(NETWORK)
			.withNetworkAliases("vault")
			.withCopyFileToContainer(
					MountableFile.forClasspathResource("infrastructure/vault-setup/init-auth.sh", 0755),
					"/vault/setup/init-auth.sh");

	private static boolean started;

	private TestVault() {
	}

	/** Where tests send Keycloak calls unless they point at {@link TestKeycloak}: nothing listens there. */
	public static final String NO_KEYCLOAK = "http://127.0.0.1:1";

	/**
	 * Starts Vault once and points the app at it with AppRole ms-auth-local. Every test that starts the app
	 * calls this, so it also points Keycloak at {@link #NO_KEYCLOAK}: no test can reach the real local Keycloak.
	 */
	public static synchronized void start() {
		System.setProperty("KEYCLOAK_URL", NO_KEYCLOAK);
		if (!started) {
			VAULT.start();
			vault("VAULT_ADMIN_PASSWORD=admin /vault/setup/init-auth.sh");
			for (String env : new String[] { "local", "dev", "prod" }) {
				vault("vault kv put secret/xbank/" + env + "/ms-auth keycloak-client-secret=" + CLIENT_SECRET);
			}
			started = true;
		}
		useAppRole("local");
	}

	/** Sets VAULT_URL, VAULT_ROLE_ID, and a fresh VAULT_SECRET_ID for AppRole ms-auth-&lt;env&gt;. */
	public static void useAppRole(String env) {
		System.setProperty("VAULT_URL", url());
		System.setProperty("VAULT_ROLE_ID", roleId(env));
		System.setProperty("VAULT_SECRET_ID", newSecretId(env));
	}

	/** Vault URL from the test JVM (mapped host port). */
	public static String url() {
		return VAULT.getHttpHostAddress();
	}

	public static String roleId(String env) {
		return vault("vault read -field=role_id auth/approle/role/ms-auth-" + env + "/role-id").trim();
	}

	public static String newSecretId(String env) {
		return vault("vault write -f -field=secret_id auth/approle/role/ms-auth-" + env + "/secret-id").trim();
	}

	/** Runs a shell command in the Vault container with the root token; returns stdout. */
	public static String vault(String command) {
		try {
			Container.ExecResult result = VAULT.execInContainer("sh", "-c",
					"VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=" + ROOT_TOKEN + " " + command);
			if (result.getExitCode() != 0) {
				throw new IllegalStateException("Vault command failed: " + command + "\n" + result.getStderr());
			}
			return result.getStdout();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}
}
