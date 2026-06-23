package net.alkeari.geotectonic.fabric;

import net.alkeari.geotectonic.GeoTectonic;
import net.alkeari.geotectonic.config.fabric.ModConfigImpl;
import net.alkeari.geotectonic.registry.GeoTectonicCarvers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;

public class GeoTectonicFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModConfigImpl.init();
        GeoTectonic.init();
        FabricLoader.getInstance().getModContainer(GeoTectonic.MOD_ID).ifPresent(container ->
            ResourceManagerHelper.registerBuiltinResourcePack(
                ResourceLocation.fromNamespaceAndPath(GeoTectonic.MOD_ID, GeoTectonic.OVERRIDE_PACK_ID),
                container,
                ResourcePackActivationType.ALWAYS_ENABLED
            )
        );
        BiomeModifications.addCarver(
            BiomeSelectors.all(),
            GenerationStep.Carving.AIR,
            GeoTectonicCarvers.FRACTURE_KEY
        );
        BiomeModifications.addCarver(
            BiomeSelectors.all(),
            GenerationStep.Carving.AIR,
            GeoTectonicCarvers.KARST_KEY
        );
        BiomeModifications.addCarver(
            BiomeSelectors.all(),
            GenerationStep.Carving.AIR,
            GeoTectonicCarvers.CENOTE_KEY
        );
        BiomeModifications.addCarver(
            BiomeSelectors.all(),
            GenerationStep.Carving.AIR,
            GeoTectonicCarvers.EROSION_KEY
        );
        BiomeModifications.addCarver(
            BiomeSelectors.all(),
            GenerationStep.Carving.AIR,
            GeoTectonicCarvers.SEA_CAVE_KEY
        );
        BiomeModifications.addCarver(
            BiomeSelectors.all(),
            GenerationStep.Carving.AIR,
            GeoTectonicCarvers.CRYSTAL_VEIN_KEY
        );
        BiomeModifications.addCarver(
            BiomeSelectors.all(),
            GenerationStep.Carving.AIR,
            GeoTectonicCarvers.VOLCANIC_VENT_KEY
        );
    }
}
