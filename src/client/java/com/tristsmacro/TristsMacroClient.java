package com.tristsmacro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class TristsMacroClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
        ClientCommandHandler.register(dispatcher);
    });
	}
}