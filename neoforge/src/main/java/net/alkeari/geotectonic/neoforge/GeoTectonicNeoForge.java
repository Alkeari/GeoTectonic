package net.alkeari.geotectonic.neoforge;

import com.mojang.serialization.MapCodec;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.alkeari.geotectonic.GeoTectonic;
import net.alkeari.geotectonic.config.neoforge.ModConfigImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@Mod(GeoTectonic.MOD_ID)
public class GeoTectonicNeoForge {

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, GeoTectonic.MOD_ID);

    public static final Supplier<MapCodec<AddCarverBiomeModifier>> ADD_CARVER_MODIFIER_CODEC =
        BIOME_MODIFIER_SERIALIZERS.register("add_carver", () -> AddCarverBiomeModifier.CODEC);

    public GeoTectonicNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModConfigImpl.SPEC, "geotectonic.toml");
        modEventBus.addListener(ModConfigImpl::onConfigLoading);
        modEventBus.addListener(ModConfigImpl::onConfigReloading);
        GeoTectonic.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> GeoTectonicNeoForgeClient::init);
    }
}
