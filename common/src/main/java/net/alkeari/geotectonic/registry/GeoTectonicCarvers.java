package net.alkeari.geotectonic.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.alkeari.geotectonic.GeoTectonic;
import net.alkeari.geotectonic.cave.FractureCarver;
import net.alkeari.geotectonic.cave.FractureCarverConfig;
import net.alkeari.geotectonic.cave.CenoteCarver;
import net.alkeari.geotectonic.cave.CenoteCarverConfig;
import net.alkeari.geotectonic.cave.ErosionCarver;
import net.alkeari.geotectonic.cave.ErosionCarverConfig;
import net.alkeari.geotectonic.cave.KarstCarver;
import net.alkeari.geotectonic.cave.KarstCarverConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

public class GeoTectonicCarvers {

    public static final DeferredRegister<WorldCarver<?>> CARVERS =
        DeferredRegister.create(GeoTectonic.MOD_ID, Registries.CARVER);

    public static final RegistrySupplier<WorldCarver<FractureCarverConfig>> FRACTURE =
        CARVERS.register("fracture", () -> new FractureCarver(FractureCarverConfig.CODEC));

    public static final RegistrySupplier<WorldCarver<KarstCarverConfig>> KARST =
        CARVERS.register("karst", () -> new KarstCarver(KarstCarverConfig.CODEC));

    public static final RegistrySupplier<WorldCarver<CenoteCarverConfig>> CENOTE =
        CARVERS.register("cenote", () -> new CenoteCarver(CenoteCarverConfig.CODEC));

    public static final RegistrySupplier<WorldCarver<ErosionCarverConfig>> EROSION =
        CARVERS.register("erosion", () -> new ErosionCarver(ErosionCarverConfig.CODEC));

    public static final ResourceKey<ConfiguredWorldCarver<?>> FRACTURE_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            new ResourceLocation(GeoTectonic.MOD_ID, "fracture"));

    public static final ResourceKey<ConfiguredWorldCarver<?>> KARST_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            new ResourceLocation(GeoTectonic.MOD_ID, "karst"));

    public static final ResourceKey<ConfiguredWorldCarver<?>> CENOTE_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            new ResourceLocation(GeoTectonic.MOD_ID, "cenote"));

    public static final ResourceKey<ConfiguredWorldCarver<?>> EROSION_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            new ResourceLocation(GeoTectonic.MOD_ID, "erosion"));

    public static void init() {
        CARVERS.register();
    }
}
