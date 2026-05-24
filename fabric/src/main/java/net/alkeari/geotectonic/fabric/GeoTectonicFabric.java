package net.alkeari.geotectonic.fabric;

import net.alkeari.geotectonic.GeoTectonic;
import net.alkeari.geotectonic.registry.GeoTectonicCarvers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;

public class GeoTectonicFabric implements ModInitializer {
    @Override
    public void onInitialize() {
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
    }
}
