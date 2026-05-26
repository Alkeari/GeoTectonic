package net.alkeari.geotectonic.config.fabric;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ModConfigImpl {

    private record ConfigSnapshot(
        double fractureProbability,
        double karstProbability,
        double cenoteProbability,
        double erosionProbability,
        double seaCaveProbability,
        double crystalVeinProbability,
        double volcanicVentProbability,
        boolean fractureEnabled,
        boolean karstEnabled,
        boolean cenoteEnabled,
        boolean erosionEnabled,
        boolean seaCaveEnabled,
        boolean crystalVeinEnabled,
        boolean volcanicVentEnabled,
        boolean discoverySubtitleEnabled,
        boolean advancementsEnabled,
        boolean ambientSoundsEnabled
    ) {
        static ConfigSnapshot defaults() {
            return new ConfigSnapshot(0.005, 0.006, 0.004, 0.006, 0.010, 0.010, 0.010,
                true, true, true, true, true, true, true, true, true, true);
        }
    }

    private static final String TOML_TEMPLATE = """
            [cave_generation]
            fracture_probability = 0.005
            fracture_enabled = true
            karst_probability = 0.006
            karst_enabled = true
            cenote_probability = 0.004
            cenote_enabled = true
            erosion_probability = 0.006
            erosion_enabled = true
            sea_cave_probability = 0.010
            sea_cave_enabled = true
            crystal_vein_probability = 0.010
            crystal_vein_enabled = true
            volcanic_vent_probability = 0.010
            volcanic_vent_enabled = true

            [client]
            discovery_subtitle_enabled = true
            advancements_enabled = true
            ambient_sounds_enabled = true
            """;

    private static final AtomicReference<ConfigSnapshot> SNAPSHOT =
        new AtomicReference<>(ConfigSnapshot.defaults());

    private static Path configPath;
    private static long lastModified = -1;

    public static void init() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("geotectonic.toml");
        if (!Files.exists(configPath)) {
            try {
                Files.writeString(configPath, TOML_TEMPLATE);
            } catch (IOException e) {
                net.alkeari.geotectonic.GeoTectonic.LOGGER.error("Failed to write default GeoTectonic config", e);
            }
        }
        reload();
        ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "geotectonic-config-watcher");
            t.setDaemon(true);
            return t;
        });
        watcher.scheduleWithFixedDelay(ModConfigImpl::pollFile, 2, 2, TimeUnit.SECONDS);
    }

    private static void pollFile() {
        try {
            long modified = Files.getLastModifiedTime(configPath).toMillis();
            if (modified != lastModified) {
                lastModified = modified;
                reload();
            }
        } catch (IOException ignored) {}
    }

    private static void reload() {
        try {
            String text = Files.readString(configPath);
            UnmodifiableConfig root = TomlFormat.instance().createParser().parse(new StringReader(text));

            UnmodifiableConfig gen = root.get(List.of("cave_generation"));
            UnmodifiableConfig client = root.get(List.of("client"));

            ConfigSnapshot snap = new ConfigSnapshot(
                clamp(getDouble(gen, "fracture_probability",      0.005)),
                clamp(getDouble(gen, "karst_probability",         0.006)),
                clamp(getDouble(gen, "cenote_probability",        0.004)),
                clamp(getDouble(gen, "erosion_probability",       0.006)),
                clamp(getDouble(gen, "sea_cave_probability",      0.010)),
                clamp(getDouble(gen, "crystal_vein_probability",  0.010)),
                clamp(getDouble(gen, "volcanic_vent_probability", 0.010)),
                getBool(gen,    "fracture_enabled",                true),
                getBool(gen,    "karst_enabled",                   true),
                getBool(gen,    "cenote_enabled",                  true),
                getBool(gen,    "erosion_enabled",                 true),
                getBool(gen,    "sea_cave_enabled",                true),
                getBool(gen,    "crystal_vein_enabled",            true),
                getBool(gen,    "volcanic_vent_enabled",           true),
                getBool(client, "discovery_subtitle_enabled",      true),
                getBool(client, "advancements_enabled",            true),
                getBool(client, "ambient_sounds_enabled",          true)
            );
            SNAPSHOT.set(snap);
        } catch (Exception e) {
            net.alkeari.geotectonic.GeoTectonic.LOGGER.error("Failed to load GeoTectonic config", e);
        }
    }

    public static double getFractureProbability() { return SNAPSHOT.get().fractureProbability(); }
    public static double getKarstProbability()    { return SNAPSHOT.get().karstProbability(); }
    public static double getCenoteProbability()   { return SNAPSHOT.get().cenoteProbability(); }
    public static double getErosionProbability()  { return SNAPSHOT.get().erosionProbability(); }
    public static double getSeaCaveProbability()        { return SNAPSHOT.get().seaCaveProbability(); }
    public static double getCrystalVeinProbability()   { return SNAPSHOT.get().crystalVeinProbability(); }
    public static double getVolcanicVentProbability()  { return SNAPSHOT.get().volcanicVentProbability(); }

    public static boolean isFractureEnabled()          { return SNAPSHOT.get().fractureEnabled(); }
    public static boolean isKarstEnabled()             { return SNAPSHOT.get().karstEnabled(); }
    public static boolean isCenoteEnabled()            { return SNAPSHOT.get().cenoteEnabled(); }
    public static boolean isErosionEnabled()           { return SNAPSHOT.get().erosionEnabled(); }
    public static boolean isSeaCaveEnabled()            { return SNAPSHOT.get().seaCaveEnabled(); }
    public static boolean isCrystalVeinEnabled()        { return SNAPSHOT.get().crystalVeinEnabled(); }
    public static boolean isVolcanicVentEnabled()       { return SNAPSHOT.get().volcanicVentEnabled(); }

    public static boolean isDiscoverySubtitleEnabled() { return SNAPSHOT.get().discoverySubtitleEnabled(); }
    public static boolean isAdvancementsEnabled()      { return SNAPSHOT.get().advancementsEnabled(); }
    public static boolean isAmbientSoundsEnabled()     { return SNAPSHOT.get().ambientSoundsEnabled(); }

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    private static double getDouble(UnmodifiableConfig cfg, String key, double def) {
        if (cfg == null) return def;
        Object v = cfg.get(List.of(key));
        if (v instanceof Number n) return n.doubleValue();
        return def;
    }

    private static boolean getBool(UnmodifiableConfig cfg, String key, boolean def) {
        if (cfg == null) return def;
        Object v = cfg.get(List.of(key));
        if (v instanceof Boolean b) return b;
        return def;
    }
}
