package net.alkeari.MODID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MODID {
    public static final String MOD_ID = "MODID";
    public static final Logger LOGGER = LoggerFactory.getLogger("MODID");

    public static void init() {
        LOGGER.info("{} loaded.", MOD_ID);
    }
}
