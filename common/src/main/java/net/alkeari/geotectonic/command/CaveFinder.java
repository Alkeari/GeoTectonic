package net.alkeari.geotectonic.command;

import net.alkeari.geotectonic.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import net.minecraft.world.level.block.Block;
import java.util.*;

public class CaveFinder {

    public record CaveType(String name, float probability, int range, int teleportY, String biomeNote) {}

    public static final List<CaveType> CAVE_LIST = List.of(
        new CaveType("fracture",     0.005f,  50,  0,  "all overworld"),
        new CaveType("karst",        0.006f,  32,  30, "temp 0.2-1.5"),
        new CaveType("cenote",       0.004f,  4,   35, "temp 0.5-1.5"),
        new CaveType("erosion",      0.006f,  10,  45, "all overworld"),
        new CaveType("sea_cave",     0.010f,  8,   55, "all overworld, flooded"),
        new CaveType("crystal_vein", 0.010f,  6,  -25, "all overworld, deep"),
        new CaveType("volcanic_vent",0.010f,  4,  -40, "all overworld, deep")
    );

    public static final Map<String, CaveType> CAVES;
    static {
        Map<String, CaveType> m = new LinkedHashMap<>();
        CAVE_LIST.forEach(c -> m.put(c.name(), c));
        CAVES = Collections.unmodifiableMap(m);
    }

    /**
     * Scans chunks in Chebyshev-distance rings outward from (playerChunkX, playerChunkZ).
     * Returns an actual standing position (1x2 gap of caveAirBlock) inside the nearest
     * matching cave chunk, or empty if none found within maxRadius.
     */
    public static float configProbability(String name) {
        try {
            return (float) switch (name) {
                case "fracture"  -> ModConfig.getFractureProbability();
                case "karst"     -> ModConfig.getKarstProbability();
                case "cenote"    -> ModConfig.getCenoteProbability();
                case "erosion"   -> ModConfig.getErosionProbability();
                case "sea_cave"     -> ModConfig.getSeaCaveProbability();
                case "crystal_vein" -> ModConfig.getCrystalVeinProbability();
                case "volcanic_vent"-> ModConfig.getVolcanicVentProbability();
                default -> 0.0;
            };
        } catch (Throwable t) {
            CaveType cave = CAVES.get(name);
            return cave != null ? cave.probability() : 0f;
        }
    }

    public static boolean isEnabled(String name) {
        try {
            return switch (name) {
                case "fracture"  -> ModConfig.isFractureEnabled();
                case "karst"     -> ModConfig.isKarstEnabled();
                case "cenote"    -> ModConfig.isCenoteEnabled();
                case "erosion"   -> ModConfig.isErosionEnabled();
                case "sea_cave"     -> ModConfig.isSeaCaveEnabled();
                case "crystal_vein" -> ModConfig.isCrystalVeinEnabled();
                case "volcanic_vent"-> ModConfig.isVolcanicVentEnabled();
                default -> true;
            };
        } catch (Throwable t) {
            return true;
        }
    }

    public static Optional<BlockPos> findNearest(ServerLevel level, int playerChunkX, int playerChunkZ,
                                                  CaveType cave, int maxRadius, Block caveAirBlock) {
        if (!isEnabled(cave.name())) return Optional.empty();
        long seed = level.getSeed();
        long typedSeed = seed ^ ((long) cave.name().hashCode() * 0x9E3779B97F4A7C15L);
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (ChunkPos pos : ring(playerChunkX, playerChunkZ, radius)) {
                if (!wouldGenerate(typedSeed, pos.x, pos.z, configProbability(cave.name()))) continue;
                if (!biomeMatches(level, pos, cave)) continue;
                BlockPos standingPos = findStandingSpot(level, pos, caveAirBlock, cave.teleportY());
                if (standingPos != null) return Optional.of(standingPos);
            }
        }
        return Optional.empty();
    }

static BlockPos findStandingSpot(ServerLevel level, ChunkPos chunkPos,
                                              Block caveAirBlock, int nearY) {
        level.getChunk(chunkPos.x, chunkPos.z); // ensure chunk is generated so carvers have run
        int minX = chunkPos.getMinBlockX(), maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ(), maxZ = chunkPos.getMaxBlockZ();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 2; // -2 so y+1 is always in bounds

        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).is(caveAirBlock)) continue;
                    pos.set(x, y + 1, z);
                    if (!level.getBlockState(pos).is(caveAirBlock)) continue;
                    int dist = Math.abs(y - nearY);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = new BlockPos(x, y, z);
                    }
                }
            }
        }
        return best;
    }

    private static boolean biomeMatches(ServerLevel level, ChunkPos pos, CaveType cave) {
        return switch (cave.name()) {
            case "karst" -> {
                float temp = level.getBiome(new BlockPos(pos.getMiddleBlockX(), 64, pos.getMiddleBlockZ()))
                                  .value().getBaseTemperature();
                yield temp >= 0.2f && temp <= 1.5f;
            }
            case "cenote" -> {
                float temp = level.getBiome(new BlockPos(pos.getMiddleBlockX(), 64, pos.getMiddleBlockZ()))
                                  .value().getBaseTemperature();
                yield temp >= 0.5f && temp <= 1.5f;
            }
            default -> true;
        };
    }

    /**
     * Counts how many of each cave type would generate within a square of chunk radius
     * centred on (playerChunkX, playerChunkZ).
     */
    public static Map<String, Integer> countAll(ServerLevel level, int playerChunkX, int playerChunkZ,
                                                 int radius) {
        long seed = level.getSeed();
        Map<String, Integer> counts = new LinkedHashMap<>();
        CAVE_LIST.forEach(c -> counts.put(c.name(), 0));

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                for (CaveType cave : CAVE_LIST) {
                    if (!isEnabled(cave.name())) continue;
                    long typedSeed = seed ^ ((long) cave.name().hashCode() * 0x9E3779B97F4A7C15L);
                    if (wouldGenerate(typedSeed, cx, cz, configProbability(cave.name()))) {
                        counts.merge(cave.name(), 1, Integer::sum);
                    }
                }
            }
        }
        return counts;
    }

    /**
     * Replicates Minecraft's ChunkGenerator.applyCarvers seed derivation exactly.
     * Each configured carver gets WorldgenRandom.setLargeFeatureSeed(levelSeed, chunkX, chunkZ)
     * then the first nextFloat() is the probability roll.
     */
    public static boolean wouldGenerate(long seed, int chunkX, int chunkZ, float probability) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureSeed(seed, chunkX, chunkZ);
        return random.nextFloat() < probability;
    }

    /**
     * Returns true if any chunk within a square of 'radius' chunks around
     * (playerChunkX, playerChunkZ) would generate the given cave type.
     */
    public static boolean wouldGenerateNear(ServerLevel level, int playerChunkX, int playerChunkZ,
                                             CaveType cave, int radius) {
        long seed = level.getSeed();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (wouldGenerate(seed, playerChunkX + dx, playerChunkZ + dz, cave.probability())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns all ChunkPos at exactly Chebyshev distance 'radius' from (cx, cz). */
    private static List<ChunkPos> ring(int cx, int cz, int radius) {
        if (radius == 0) return List.of(new ChunkPos(cx, cz));
        List<ChunkPos> ring = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            ring.add(new ChunkPos(cx + dx, cz - radius));
            ring.add(new ChunkPos(cx + dx, cz + radius));
        }
        for (int dz = -radius + 1; dz <= radius - 1; dz++) {
            ring.add(new ChunkPos(cx - radius, cz + dz));
            ring.add(new ChunkPos(cx + radius, cz + dz));
        }
        return ring;
    }
}
