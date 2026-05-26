package net.alkeari.geotectonic.cave;

import com.mojang.serialization.Codec;
import net.alkeari.geotectonic.config.ModConfig;
import net.alkeari.geotectonic.registry.GeoTectonicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

import java.util.List;
import java.util.function.Function;

public class SeaCaveCarver extends AbstractGeoCarver<SeaCaveCarverConfig> {

    private static final int SEA_LEVEL = 63;

    public SeaCaveCarver(Codec<SeaCaveCarverConfig> codec) {
        super(codec);
    }

    @Override
    protected BlockState caveAirState() {
        return GeoTectonicBlocks.SEA_CAVE_AIR.get().defaultBlockState();
    }

    @Override
    public int getRange() { return 8; }

    @Override
    public boolean isStartChunk(SeaCaveCarverConfig config, RandomSource random) {
        double prob;
        try { prob = ModConfig.getSeaCaveProbability(); }
        catch (Throwable t) { prob = config.probability; }
        return ModConfig.isSeaCaveEnabled() && random.nextFloat() < prob;
    }

    @Override
    public boolean carve(CarvingContext context, SeaCaveCarverConfig config, ChunkAccess chunk,
                         Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource random,
                         Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask carvingMask) {

        // All geometry drawn up-front — identical across every carve() call for this source chunk
        int openingX = sourceChunkPos.getMinBlockX() + random.nextInt(16);
        int openingZ = sourceChunkPos.getMinBlockZ() + random.nextInt(16);

        List<PathPoint> waypoints = SeaCavePathGenerator.generate(random, openingX, openingZ, SEA_LEVEL);
        if (waypoints.isEmpty()) return false;

        // Position-based noise seed — no random draw, same value in every chunk
        int noiseSeed = openingX * 73856093 ^ openingZ * 19349663;

        ChunkPos currentPos = chunk.getPos();
        int minX = currentPos.getMinBlockX();
        int minZ = currentPos.getMinBlockZ();
        int maxX = currentPos.getMaxBlockX();
        int maxZ = currentPos.getMaxBlockZ();

        PathPoint startPt = new PathPoint(
            new BlockPos(openingX, SEA_LEVEL - 7, openingZ),
            waypoints.get(0).widthRadius(),
            waypoints.get(0).heightRadius());

        boolean anyOverlap = false;
        PathPoint prev = startPt;
        for (PathPoint wp : waypoints) {
            float ext = Math.max(prev.widthRadius(), wp.widthRadius()) + 2f;
            int sx1 = Math.min(prev.center().getX(), wp.center().getX());
            int sx2 = Math.max(prev.center().getX(), wp.center().getX());
            int sz1 = Math.min(prev.center().getZ(), wp.center().getZ());
            int sz2 = Math.max(prev.center().getZ(), wp.center().getZ());
            if (sx2 + ext >= minX && sx1 - ext <= maxX && sz2 + ext >= minZ && sz1 - ext <= maxZ) {
                anyOverlap = true;
                break;
            }
            prev = wp;
        }
        if (!anyOverlap) return false;

        boolean anyCarved = false;
        prev = startPt;
        for (int i = 0; i < waypoints.size(); i++) {
            PathPoint curr = waypoints.get(i);
            anyCarved |= carveSegment(chunk, minX, minZ, maxX, maxZ, carvingMask,
                    prev, curr, noiseSeed + i * 100);
            prev = curr;
        }

        // Flood all SEA_CAVE_AIR below sea level, keeping top 2 rows for detection
        fillWaterBelow(chunk, minX, minZ, maxX, maxZ);

        if (anyCarved) {
            decorateCave(chunk, minX, minZ, maxX, maxZ, startPt, waypoints, noiseSeed);
        }

        return anyCarved;
    }

    private boolean carveSegment(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                 CarvingMask carvingMask, PathPoint from, PathPoint to, int noiseSeed) {
        double dx = to.center().getX() - from.center().getX();
        double dy = to.center().getY() - from.center().getY();
        double dz = to.center().getZ() - from.center().getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.01) return false;

        boolean anyCarved = false;
        int steps = (int) Math.ceil(dist);

        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            int bx = (int) Math.round(from.center().getX() + dx * t);
            int by = (int) Math.round(from.center().getY() + dy * t);
            int bz = (int) Math.round(from.center().getZ() + dz * t);
            float wr = (float) (from.widthRadius() * (1.0 - t) + to.widthRadius() * t);
            float hr = (float) (from.heightRadius() * (1.0 - t) + to.heightRadius() * t);

            float ext = wr + 2f;
            if (bx + ext < minX || bx - ext > maxX || bz + ext < minZ || bz - ext > maxZ) continue;

            anyCarved |= carveNoisyEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask,
                    bx, by, bz, wr, hr, noiseSeed + s * 7);
        }
        return anyCarved;
    }

    /**
     * Floods all SEA_CAVE_AIR blocks below sea level with water, but keeps the top 2 rows as
     * SEA_CAVE_AIR so CaveDetectionHandler and CaveFinder can locate the cave.
     * Water does not flow upward, so isolated ceiling rows stay dry after chunk load.
     */
    private void fillWaterBelow(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ) {
        int scanMaxY = SEA_LEVEL - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = chunk.getMinBuildHeight(); y <= scanMaxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!chunk.getBlockState(pos).is(GeoTectonicBlocks.SEA_CAVE_AIR.get())) continue;

                    // Keep top 2 rows as SEA_CAVE_AIR; flood everything below
                    BlockPos above1 = pos.above();
                    BlockPos above2 = pos.above(2);
                    boolean row1IsCaveAir = above1.getY() < chunk.getMaxBuildHeight()
                            && chunk.getBlockState(above1).is(GeoTectonicBlocks.SEA_CAVE_AIR.get());
                    boolean row2IsCaveAir = above2.getY() < chunk.getMaxBuildHeight()
                            && chunk.getBlockState(above2).is(GeoTectonicBlocks.SEA_CAVE_AIR.get());

                    if (row1IsCaveAir && row2IsCaveAir) {
                        chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
                        chunk.markPosForPostprocessing(pos);
                    }
                }
            }
        }
    }

    private void decorateCave(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                               PathPoint startPt, List<PathPoint> waypoints, int noiseSeed) {
        int bMinY = chunk.getMaxBuildHeight(), bMaxY = chunk.getMinBuildHeight();
        for (PathPoint wp : waypoints) {
            int hr = (int) Math.ceil(wp.heightRadius()) + 3;
            bMinY = Math.min(bMinY, wp.center().getY() - hr);
            bMaxY = Math.max(bMaxY, wp.center().getY() + hr);
        }
        int hrS = (int) Math.ceil(startPt.heightRadius()) + 3;
        bMinY = Math.min(bMinY, startPt.center().getY() - hrS);
        bMaxY = Math.max(bMaxY, startPt.center().getY() + hrS);
        bMinY = Math.max(chunk.getMinBuildHeight(), bMinY);
        bMaxY = Math.min(chunk.getMaxBuildHeight() - 1, bMaxY);

        PathPoint chamberPt = waypoints.get(waypoints.size() - 1);
        float chamberR = chamberPt.widthRadius() + 5f;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                float dcx = x - chamberPt.center().getX();
                float dcz = z - chamberPt.center().getZ();
                boolean inChamberZone = dcx * dcx + dcz * dcz <= chamberR * chamberR;

                for (int y = bMinY; y <= bMaxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!chunk.getBlockState(pos).is(Blocks.WATER)) continue;

                    BlockPos below = pos.below();
                    if (below.getY() < chunk.getMinBuildHeight()) continue;
                    boolean solidBelow = chunk.getBlockState(below).isSolid();

                    int hash = posHash(x, y, z, noiseSeed);
                    int b0 = hash & 0xFF;
                    int b1 = (hash >> 8) & 0xFF;

                    if (solidBelow) {
                        // Floor replacement
                        if (inChamberZone) {
                            if (b0 < 128)      chunk.setBlockState(below, Blocks.SAND.defaultBlockState(), false);
                            else if (b0 < 166) chunk.setBlockState(below, Blocks.CLAY.defaultBlockState(), false);
                        } else {
                            if (b0 < 77)       chunk.setBlockState(below, Blocks.GRAVEL.defaultBlockState(), false);
                            else if (b0 < 128) chunk.setBlockState(below, Blocks.SAND.defaultBlockState(), false);
                        }

                        // Seagrass on solid floor
                        int seagrassThresh = inChamberZone ? 51 : 38;
                        if (b1 < seagrassThresh) {
                            chunk.setBlockState(pos, Blocks.SEAGRASS.defaultBlockState(), false);
                            chunk.markPosForPostprocessing(pos);
                        }
                    }

                    // Calcite on adjacent solid walls
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int wx = x + dir.getStepX(), wz = z + dir.getStepZ();
                        if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                        BlockPos wallPos = new BlockPos(wx, y, wz);
                        if (!chunk.getBlockState(wallPos).isSolid()) continue;
                        int wallHash = posHash(wx, y, wz, noiseSeed + 7);
                        if ((wallHash & 0xFF) < 31) {
                            chunk.setBlockState(wallPos, Blocks.CALCITE.defaultBlockState(), false);
                        }
                    }
                }
            }
        }
    }
}
