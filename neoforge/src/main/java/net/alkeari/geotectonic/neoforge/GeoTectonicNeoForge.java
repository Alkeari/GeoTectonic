package net.alkeari.geotectonic.neoforge;

import com.mojang.serialization.MapCodec;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.alkeari.geotectonic.GeoTectonic;
import net.alkeari.geotectonic.config.neoforge.ModConfigImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
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
        modEventBus.addListener(GeoTectonicNeoForge::onAddPackFinders);
        GeoTectonic.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> GeoTectonicNeoForgeClient::init);
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        event.addPackFinders(
            ResourceLocation.fromNamespaceAndPath(GeoTectonic.MOD_ID, "resourcepacks/" + GeoTectonic.OVERRIDE_PACK_ID),
            PackType.SERVER_DATA,
            Component.literal("GeoTectonic Worldgen Overrides"),
            PackSource.BUILT_IN,
            true,
            Pack.Position.TOP
        );
    }
}
