package net.alkeari.geotectonic.cave;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

import java.util.function.Function;

public class CenoteCarver extends AbstractGeoCarver<CenoteCarverConfig> {

    public CenoteCarver(Codec<CenoteCarverConfig> codec) {
        super(codec);
    }

    @Override
    protected float minTemperature() { return 0.5f; }

    @Override
    protected float maxTemperature() { return 1.5f; }

    @Override
    public int getRange() { return 4; }

    @Override
    public boolean isStartChunk(CenoteCarverConfig config, RandomSource random) {
        return random.nextFloat() < config.probability;
    }

    @Override
    public boolean carve(CarvingContext context, CenoteCarverConfig config, ChunkAccess chunk,
                         Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource random,
                         Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask carvingMask) {

        int openingX = sourceChunkPos.getMinBlockX() + random.nextInt(16);
        int openingZ = sourceChunkPos.getMinBlockZ() + random.nextInt(16);

        if (!isTemperatureValid(biomeAccessor, openingX, openingZ)) return false;

        // All geometry drawn up-front from seeded random — identical across every carve() call
        int chamberY   = 20 + random.nextInt(20);
        float chamberWr = 8.0f + random.nextFloat() * 7.0f;
        float chamberHr = chamberWr * (0.50f + random.nextFloat() * 0.30f);

        // Always draw 3 lobe param sets for consistent random consumption; use numLobes of them
        int numLobes = 2 + random.nextInt(2);
        int[]   lobeOffX = new int[3];
        int[]   lobeOffZ = new int[3];
        float[] lobeWr   = new float[3];
        float[] lobeHr   = new float[3];
        for (int i = 0; i < 3; i++) {
            lobeOffX[i] = random.nextInt(11) - 5;
            lobeOffZ[i] = random.nextInt(11) - 5;
            lobeWr[i]   = chamberWr * (0.40f + random.nextFloat() * 0.30f);
            lobeHr[i]   = chamberHr * (0.35f + random.nextFloat() * 0.30f);
        }

        // Subtle funnel lean — uses a fixed reference height so the axis is identical in every chunk
        float driftX = (random.nextFloat() - 0.5f) * 0.038f;
        float driftZ = (random.nextFloat() - 0.5f) * 0.038f;
        // Funnel horizontal extent (conservative upper bound for overlap checks)
        float maxFunnelRadius = chamberWr * 0.80f + 2f;

        // Position-seeded noise (no random calls in carving loops — consistent cross-chunk)
        int noiseSeed = openingX * 73856093 ^ openingZ * 19349663;

        ChunkPos currentPos = chunk.getPos();
        int minX = currentPos.getMinBlockX();
        int minZ = currentPos.getMinBlockZ();
        int maxX = currentPos.getMaxBlockX();
        int maxZ = currentPos.getMaxBlockZ();

        float maxExtent = chamberWr + 7f;
        boolean chamberOverlaps = openingX + maxExtent >= minX && openingX - maxExtent <= maxX
                && openingZ + maxExtent >= minZ && openingZ - maxExtent <= maxZ;
        // True only for the chunk that contains the opening (used for decoration)
        boolean hasFunnel = openingX >= minX && openingX <= maxX
                && openingZ >= minZ && openingZ <= maxZ;
        // True for any chunk whose XZ range overlaps the funnel column
        boolean funnelOverlaps = openingX + maxFunnelRadius >= minX && openingX - maxFunnelRadius <= maxX
                && openingZ + maxFunnelRadius >= minZ && openingZ - maxFunnelRadius <= maxZ;
        if (!chamberOverlaps && !funnelOverlaps) return false;

        boolean anyCarved = false;

        if (chamberOverlaps) {
            anyCarved |= carveNoisyEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask,
                    openingX, chamberY, openingZ, chamberWr, chamberHr, noiseSeed);

            for (int i = 0; i < numLobes; i++) {
                anyCarved |= carveNoisyEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask,
                        openingX + lobeOffX[i], chamberY, openingZ + lobeOffZ[i],
                        lobeWr[i], lobeHr[i], noiseSeed + i + 1);
            }
        }

        // Flood all carved air below the water line (covers lobes automatically)
        anyCarved |= fillWaterBelow(chunk, minX, minZ, maxX, maxZ,
                openingX, chamberY, openingZ, chamberWr, chamberHr);

        if (funnelOverlaps) {
            int funnelBase = chamberY + (int) Math.ceil(chamberHr);

            // Scan the max surface height across the entire funnel footprint within this chunk.
            // Using only the center column leaves mountainous edges partially buried.
            int footprintR = (int) Math.ceil(maxFunnelRadius);
            int localCX = Math.min(15, Math.max(0, openingX - minX));
            int localCZ = Math.min(15, Math.max(0, openingZ - minZ));
            int surfaceY = 0;
            for (int dx = -footprintR; dx <= footprintR; dx++) {
                for (int dz = -footprintR; dz <= footprintR; dz++) {
                    surfaceY = Math.max(surfaceY,
                        chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG,
                            Math.min(15, Math.max(0, localCX + dx)),
                            Math.min(15, Math.max(0, localCZ + dz))));
                }
            }

            for (int y = funnelBase; y < surfaceY; y++) {
                float t = (float)(surfaceY - y) / Math.max(1, surfaceY - funnelBase);
                float baseFunnelWr = 2.0f + (1f - t) * (chamberWr * 0.65f - 2.0f);
                float undulation = 1.0f + 0.22f * (float) Math.sin(y * 0.65 + openingX * 0.12);
                float funnelWr = Math.max(1.5f, baseFunnelWr * undulation);

                // Drift uses a fixed reference height (200) so axis position is the same in every chunk
                int driftedX = openingX + (int)(driftX * Math.max(0, 200 - y));
                int driftedZ = openingZ + (int)(driftZ * Math.max(0, 200 - y));
                anyCarved |= carveNoisyEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask,
                        driftedX, y, driftedZ, funnelWr, 0.9f, noiseSeed + y * 7);
            }

            // Decoration runs only in the chunk that owns the opening
            if (hasFunnel) {
                decorateChamber(chunk, minX, minZ, maxX, maxZ, random,
                        openingX, chamberY, openingZ, chamberWr, chamberHr);
            }
        }

        return anyCarved;
    }

    /** Fills all CAVE_AIR below the water line (y < cy) within the chamber footprint. */
    private boolean fillWaterBelow(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                   int cx, int cy, int cz, float wr, float hr) {
        boolean any = false;
        int wrCeil = (int) Math.ceil(wr) + 6;
        int hrCeil = (int) Math.ceil(hr) + 2;
        int scanMinY = Math.max(chunk.getMinBuildHeight(), cy - hrCeil);

        for (int x = Math.max(minX, cx - wrCeil); x <= Math.min(maxX, cx + wrCeil); x++) {
            for (int z = Math.max(minZ, cz - wrCeil); z <= Math.min(maxZ, cz + wrCeil); z++) {
                for (int y = scanMinY; y < cy; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (chunk.getBlockState(pos).is(Blocks.CAVE_AIR)) {
                        chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
                        chunk.markPosForPostprocessing(pos);
                        any = true;
                    }
                }
            }
        }
        return any;
    }

    private void decorateChamber(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                 RandomSource rand, int cx, int cy, int cz, float wr, float hr) {
        int searchR  = (int) Math.ceil(wr) + 1;
        int searchMinX = Math.max(minX, cx - searchR);
        int searchMaxX = Math.min(maxX, cx + searchR);
        int searchMinZ = Math.max(minZ, cz - searchR);
        int searchMaxZ = Math.min(maxZ, cz + searchR);
        int scanMinY = Math.max(chunk.getMinBuildHeight(), cy - (int) Math.ceil(hr) - 1);
        int scanMaxY = Math.min(chunk.getMaxBuildHeight() - 1, cy + (int) Math.ceil(hr) + 1);

        for (int x = searchMinX; x <= searchMaxX; x++) {
            for (int z = searchMinZ; z <= searchMaxZ; z++) {
                for (int y = scanMinY; y <= scanMaxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    boolean isCaveAir = chunk.getBlockState(pos).is(Blocks.CAVE_AIR);
                    boolean isWater   = chunk.getBlockState(pos).is(Blocks.WATER);
                    if (!isCaveAir && !isWater) continue;

                    BlockPos above = pos.above();
                    BlockPos below = pos.below();

                    // Stalactites on dry ceiling
                    if (isCaveAir && chunk.getBlockState(above).isSolid() && rand.nextFloat() < 0.20f) {
                        chunk.setBlockState(pos, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                                .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                                .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP)
                                .setValue(PointedDripstoneBlock.WATERLOGGED, false), false);
                        continue;
                    }

                    // Calcite on chamber walls
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int wx = x + dir.getStepX(), wz = z + dir.getStepZ();
                        if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                        if (chunk.getBlockState(new BlockPos(wx, y, wz)).isSolid()
                                && rand.nextFloat() < 0.25f) {
                            chunk.setBlockState(new BlockPos(wx, y, wz),
                                    Blocks.CALCITE.defaultBlockState(), false);
                        }
                    }

                    // Clay on the submerged floor
                    if (isWater && chunk.getBlockState(below).isSolid() && rand.nextFloat() < 0.40f) {
                        chunk.setBlockState(below, Blocks.CLAY.defaultBlockState(), false);
                    }
                }
            }
        }
    }
}
