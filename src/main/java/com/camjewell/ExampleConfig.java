package com.camjewell;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("potionstoragedeposit")
public interface ExampleConfig extends Config {
	@ConfigItem(keyName = "showOverlay", name = "Show status overlay", description = "Display an overlay on the potion storage button showing if auto-deposit is enabled or disabled")
	default boolean showOverlay() {
		return true;
	}
}
