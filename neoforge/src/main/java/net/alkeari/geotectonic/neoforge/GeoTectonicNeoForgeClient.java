package net.alkeari.geotectonic.neoforge;

import dev.architectury.event.events.client.ClientTickEvent;
import net.alkeari.geotectonic.client.CaveDetectionHandler;

public class GeoTectonicNeoForgeClient {

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(CaveDetectionHandler::onClientTick);
    }
}
