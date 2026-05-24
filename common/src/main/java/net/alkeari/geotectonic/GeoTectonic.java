package net.alkeari.geotectonic;

import net.alkeari.geotectonic.registry.GeoTectonicCarvers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoTectonic {
    public static final String MOD_ID = "geotectonic";
    public static final Logger LOGGER = LoggerFactory.getLogger("GeoTectonic");

    public static void init() {
        GeoTectonicCarvers.init();
        LOGGER.info("{} loaded.", MOD_ID);
    }
}
