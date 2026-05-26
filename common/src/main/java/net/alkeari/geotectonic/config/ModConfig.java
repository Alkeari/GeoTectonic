package net.alkeari.geotectonic.config;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class ModConfig {

    @ExpectPlatform
    public static double getFractureProbability() { throw new AssertionError(); }

    @ExpectPlatform
    public static double getKarstProbability() { throw new AssertionError(); }

    @ExpectPlatform
    public static double getCenoteProbability() { throw new AssertionError(); }

    @ExpectPlatform
    public static double getErosionProbability() { throw new AssertionError(); }

    @ExpectPlatform
    public static double getSeaCaveProbability() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isFractureEnabled() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isKarstEnabled() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isCenoteEnabled() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isErosionEnabled() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isSeaCaveEnabled() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isDiscoverySubtitleEnabled() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isAdvancementsEnabled() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isAmbientSoundsEnabled() { throw new AssertionError(); }

    @ExpectPlatform
    public static double getCrystalVeinProbability() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isCrystalVeinEnabled() { throw new AssertionError(); }

    @ExpectPlatform
    public static double getVolcanicVentProbability() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean isVolcanicVentEnabled() { throw new AssertionError(); }
}
