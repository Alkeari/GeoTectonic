package net.alkeari.MODID.fabric;

import net.alkeari.MODID.MODID;
import net.fabricmc.api.ModInitializer;

public class MODIDFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MODID.init();
    }
}
