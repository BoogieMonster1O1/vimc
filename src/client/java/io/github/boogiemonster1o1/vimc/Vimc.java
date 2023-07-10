package io.github.boogiemonster1o1.vimc;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vimc implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("vimc");

	@Override
	public void onInitializeClient() {
		LOGGER.info("do you know how to exit vim");
	}
}
