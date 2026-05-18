package net.alkeari.MODID.forge;

import dev.architectury.platform.forge.EventBuses;
import net.alkeari.MODID.MODID;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MODID.MOD_ID)
public class MODIDForge {
    @SuppressWarnings("removal")
    public MODIDForge() {
        EventBuses.registerModEventBus(MODID.MOD_ID,
                FMLJavaModLoadingContext.get().getModEventBus());
        MODID.init();
    }
}
