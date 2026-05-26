package net.alkeari.geotectonic.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.alkeari.geotectonic.GeoTectonic;
import net.alkeari.geotectonic.cave.FractureCarver;
import net.alkeari.geotectonic.cave.FractureCarverConfig;
import net.alkeari.geotectonic.cave.CenoteCarver;
import net.alkeari.geotectonic.cave.CenoteCarverConfig;
import net.alkeari.geotectonic.cave.CrystalVeinCarver;
import net.alkeari.geotectonic.cave.CrystalVeinCarverConfig;
import net.alkeari.geotectonic.cave.ErosionCarver;
import net.alkeari.geotectonic.cave.ErosionCarverConfig;
import net.alkeari.geotectonic.cave.KarstCarver;
import net.alkeari.geotectonic.cave.KarstCarverConfig;
import net.alkeari.geotectonic.cave.SeaCaveCarver;
import net.alkeari.geotectonic.cave.SeaCaveCarverConfig;
import net.alkeari.geotectonic.cave.VolcanicVentCarver;
import net.alkeari.geotectonic.cave.VolcanicVentCarverConfig;
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

    public static final RegistrySupplier<WorldCarver<SeaCaveCarverConfig>> SEA_CAVE =
        CARVERS.register("sea_cave", () -> new SeaCaveCarver(SeaCaveCarverConfig.CODEC));

    public static final RegistrySupplier<WorldCarver<CrystalVeinCarverConfig>> CRYSTAL_VEIN =
        CARVERS.register("crystal_vein", () -> new CrystalVeinCarver(CrystalVeinCarverConfig.CODEC));

    public static final RegistrySupplier<WorldCarver<VolcanicVentCarverConfig>> VOLCANIC_VENT =
        CARVERS.register("volcanic_vent", () -> new VolcanicVentCarver(VolcanicVentCarverConfig.CODEC));

    public static final ResourceKey<ConfiguredWorldCarver<?>> FRACTURE_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            ResourceLocation.fromNamespaceAndPath(GeoTectonic.MOD_ID, "fracture"));

    public static final ResourceKey<ConfiguredWorldCarver<?>> KARST_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            ResourceLocation.fromNamespaceAndPath(GeoTectonic.MOD_ID, "karst"));

    public static final ResourceKey<ConfiguredWorldCarver<?>> CENOTE_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            ResourceLocation.fromNamespaceAndPath(GeoTectonic.MOD_ID, "cenote"));

    public static final ResourceKey<ConfiguredWorldCarver<?>> EROSION_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            ResourceLocation.fromNamespaceAndPath(GeoTectonic.MOD_ID, "erosion"));

    public static final ResourceKey<ConfiguredWorldCarver<?>> SEA_CAVE_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            ResourceLocation.fromNamespaceAndPath(GeoTectonic.MOD_ID, "sea_cave"));

    public static final ResourceKey<ConfiguredWorldCarver<?>> CRYSTAL_VEIN_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            ResourceLocation.fromNamespaceAndPath(GeoTectonic.MOD_ID, "crystal_vein"));

    public static final ResourceKey<ConfiguredWorldCarver<?>> VOLCANIC_VENT_KEY =
        ResourceKey.create(Registries.CONFIGURED_CARVER,
            ResourceLocation.fromNamespaceAndPath(GeoTectonic.MOD_ID, "volcanic_vent"));

    public static void init() {
        CARVERS.register();
    }
}
