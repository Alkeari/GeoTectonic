package net.alkeari.geotectonic.config.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class ModConfigImpl {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.DoubleValue FRACTURE_PROBABILITY;
    private static final ForgeConfigSpec.DoubleValue KARST_PROBABILITY;
    private static final ForgeConfigSpec.DoubleValue CENOTE_PROBABILITY;
    private static final ForgeConfigSpec.DoubleValue EROSION_PROBABILITY;
    private static final ForgeConfigSpec.DoubleValue SEA_CAVE_PROBABILITY;

    private static final ForgeConfigSpec.BooleanValue FRACTURE_ENABLED;
    private static final ForgeConfigSpec.BooleanValue KARST_ENABLED;
    private static final ForgeConfigSpec.BooleanValue CENOTE_ENABLED;
    private static final ForgeConfigSpec.BooleanValue EROSION_ENABLED;
    private static final ForgeConfigSpec.BooleanValue SEA_CAVE_ENABLED;

    private static final ForgeConfigSpec.DoubleValue  CRYSTAL_VEIN_PROBABILITY;
    private static final ForgeConfigSpec.DoubleValue  VOLCANIC_VENT_PROBABILITY;
    private static final ForgeConfigSpec.BooleanValue CRYSTAL_VEIN_ENABLED;
    private static final ForgeConfigSpec.BooleanValue VOLCANIC_VENT_ENABLED;

    private static final ForgeConfigSpec.BooleanValue DISCOVERY_SUBTITLE_ENABLED;
    private static final ForgeConfigSpec.BooleanValue ADVANCEMENTS_ENABLED;
    private static final ForgeConfigSpec.BooleanValue AMBIENT_SOUNDS_ENABLED;

    static {
        BUILDER.push("cave_generation");

        FRACTURE_PROBABILITY = BUILDER.defineInRange("fracture_probability", 0.005, 0.0, 1.0);
        FRACTURE_ENABLED     = BUILDER.define("fracture_enabled", true);

        KARST_PROBABILITY    = BUILDER.defineInRange("karst_probability", 0.006, 0.0, 1.0);
        KARST_ENABLED        = BUILDER.define("karst_enabled", true);

        CENOTE_PROBABILITY   = BUILDER.defineInRange("cenote_probability", 0.004, 0.0, 1.0);
        CENOTE_ENABLED       = BUILDER.define("cenote_enabled", true);

        EROSION_PROBABILITY  = BUILDER.defineInRange("erosion_probability", 0.006, 0.0, 1.0);
        EROSION_ENABLED      = BUILDER.define("erosion_enabled", true);

        SEA_CAVE_PROBABILITY = BUILDER.defineInRange("sea_cave_probability", 0.010, 0.0, 1.0);
        SEA_CAVE_ENABLED     = BUILDER.define("sea_cave_enabled", true);

        CRYSTAL_VEIN_PROBABILITY  = BUILDER.defineInRange("crystal_vein_probability", 0.010, 0.0, 1.0);
        CRYSTAL_VEIN_ENABLED      = BUILDER.define("crystal_vein_enabled", true);

        VOLCANIC_VENT_PROBABILITY = BUILDER.defineInRange("volcanic_vent_probability", 0.010, 0.0, 1.0);
        VOLCANIC_VENT_ENABLED     = BUILDER.define("volcanic_vent_enabled", true);

        BUILDER.pop();
        BUILDER.push("client");

        DISCOVERY_SUBTITLE_ENABLED = BUILDER.define("discovery_subtitle_enabled", true);
        ADVANCEMENTS_ENABLED       = BUILDER.define("advancements_enabled", true);
        AMBIENT_SOUNDS_ENABLED     = BUILDER.define("ambient_sounds_enabled", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static double getFractureProbability() { return clamp(FRACTURE_PROBABILITY.get()); }
    public static double getKarstProbability()    { return clamp(KARST_PROBABILITY.get()); }
    public static double getCenoteProbability()   { return clamp(CENOTE_PROBABILITY.get()); }
    public static double getErosionProbability()  { return clamp(EROSION_PROBABILITY.get()); }
    public static double getSeaCaveProbability()   { return clamp(SEA_CAVE_PROBABILITY.get()); }

    public static double getCrystalVeinProbability()  { return clamp(CRYSTAL_VEIN_PROBABILITY.get()); }
    public static double getVolcanicVentProbability() { return clamp(VOLCANIC_VENT_PROBABILITY.get()); }

    public static boolean isFractureEnabled()          { return FRACTURE_ENABLED.get(); }
    public static boolean isKarstEnabled()             { return KARST_ENABLED.get(); }
    public static boolean isCenoteEnabled()            { return CENOTE_ENABLED.get(); }
    public static boolean isErosionEnabled()           { return EROSION_ENABLED.get(); }
    public static boolean isSeaCaveEnabled()            { return SEA_CAVE_ENABLED.get(); }

    public static boolean isCrystalVeinEnabled()        { return CRYSTAL_VEIN_ENABLED.get(); }
    public static boolean isVolcanicVentEnabled()       { return VOLCANIC_VENT_ENABLED.get(); }

    public static boolean isDiscoverySubtitleEnabled() { return DISCOVERY_SUBTITLE_ENABLED.get(); }
    public static boolean isAdvancementsEnabled()      { return ADVANCEMENTS_ENABLED.get(); }
    public static boolean isAmbientSoundsEnabled()     { return AMBIENT_SOUNDS_ENABLED.get(); }

    public static void onConfigLoading(ModConfigEvent.Loading event) {}
    public static void onConfigReloading(ModConfigEvent.Reloading event) {}

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
