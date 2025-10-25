package net.typho.vibrancy;

import net.fabricmc.api.ClientModInitializer;

public class VibrancyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Vibrancy.init();
    }
}
