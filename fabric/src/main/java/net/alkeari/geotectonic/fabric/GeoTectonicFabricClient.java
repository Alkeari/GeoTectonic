package net.alkeari.geotectonic.fabric;

import dev.architectury.event.events.client.ClientTickEvent;
import net.alkeari.geotectonic.client.CaveDetectionHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class GeoTectonicFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvent.CLIENT_POST.register(CaveDetectionHandler::onClientTick);
    }
}
