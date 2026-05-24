package net.alkeari.geotectonic.forge;

import com.mojang.serialization.Codec;
import dev.architectury.platform.forge.EventBuses;
import net.alkeari.geotectonic.GeoTectonic;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(GeoTectonic.MOD_ID)
public class GeoTectonicForge {

    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, GeoTectonic.MOD_ID);

    public static final RegistryObject<Codec<AddCarverBiomeModifier>> ADD_CARVER_MODIFIER_CODEC =
        BIOME_MODIFIER_SERIALIZERS.register("add_carver", () -> AddCarverBiomeModifier.CODEC);

    @SuppressWarnings("removal")
    public GeoTectonicForge() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(GeoTectonic.MOD_ID, modBus);
        BIOME_MODIFIER_SERIALIZERS.register(modBus);
        GeoTectonic.init();
    }
}
