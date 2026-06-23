package net.alkeari.geotectonic.forge;

import com.mojang.serialization.Codec;
import dev.architectury.platform.forge.EventBuses;
import net.alkeari.geotectonic.GeoTectonic;
import net.alkeari.geotectonic.config.forge.ModConfigImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.nio.file.Path;

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
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfigImpl.SPEC, "geotectonic.toml");
        modBus.addListener(ModConfigImpl::onConfigLoading);
        modBus.addListener(ModConfigImpl::onConfigReloading);
        modBus.addListener(GeoTectonicForge::onAddPackFinders);
        GeoTectonic.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> GeoTectonicForgeClient::init);
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        Path resourcePath = ModList.get().getModFileById(GeoTectonic.MOD_ID).getFile()
            .findResource("resourcepacks/" + GeoTectonic.OVERRIDE_PACK_ID);
        Pack pack = Pack.readMetaAndCreate(
            "geotectonic/" + GeoTectonic.OVERRIDE_PACK_ID,
            Component.literal("GeoTectonic Worldgen Overrides"),
            true,
            id -> new PathPackResources(id, resourcePath, true),
            PackType.SERVER_DATA,
            Pack.Position.TOP,
            PackSource.BUILT_IN
        );
        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}
