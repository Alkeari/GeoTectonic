package net.alkeari.geotectonic.fabric;

import net.alkeari.geotectonic.GeoTectonic;
import net.alkeari.geotectonic.config.fabric.ModConfigImpl;
import net.alkeari.geotectonic.registry.GeoTectonicCarvers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;

public class GeoTectonicFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModConfigImpl.init();
        GeoTectonic.init();
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
