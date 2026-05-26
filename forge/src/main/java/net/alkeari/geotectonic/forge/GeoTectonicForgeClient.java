package net.alkeari.geotectonic.forge;

import dev.architectury.event.events.client.ClientTickEvent;
import net.alkeari.geotectonic.client.CaveDetectionHandler;

public class GeoTectonicForgeClient {

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(CaveDetectionHandler::onClientTick);
    }
}
