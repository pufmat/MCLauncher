package net.puffish.mclauncher;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OfflineAccount implements Account {
	private final String name;

	public OfflineAccount(String name) {
		this.name = name;
	}

	public static OfflineAccount login(String name) {
		return new OfflineAccount(name);
	}

	public String getName() {
		return name;
	}

	public String getUUID() {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + getName())
						.getBytes(StandardCharsets.UTF_8))
				.toString();
	}

	public String getAccessToken() {
		return "00000000000000000000000000000000";
	}

}
