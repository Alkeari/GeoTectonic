package net.alkeari.geotectonic.cave;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

import java.util.List;
import java.util.function.Function;

public class ErosionCarver extends AbstractGeoCarver<ErosionCarverConfig> {

    public ErosionCarver(Codec<ErosionCarverConfig> codec) {
        super(codec);
    }

    @Override
    public int getRange() { return 10; }

    @Override
    public boolean isStartChunk(ErosionCarverConfig config, RandomSource random) {
        return random.nextFloat() < config.probability;
    }

    @Override
    public boolean carve(CarvingContext context, ErosionCarverConfig config, ChunkAccess chunk,
                         Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource random,
                         Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask carvingMask) {

        // All geometry drawn up-front — identical across every carve() call for this source chunk
        int openingX = sourceChunkPos.getMinBlockX() + random.nextInt(16);
        int openingZ = sourceChunkPos.getMinBlockZ() + random.nextInt(16);
        int startY   = 35 + random.nextInt(26);  // Y 35–60: shallow to mid depth

        List<PathPoint> waypoints = ErosionPathGenerator.generate(random, openingX, openingZ, startY);
        if (waypoints.isEmpty()) return false;

        // Position-based noise seed — no random draw, same value in every chunk
        int noiseSeed = openingX * 73856093 ^ openingZ * 19349663;

        ChunkPos currentPos = chunk.getPos();
        int minX = currentPos.getMinBlockX();
        int minZ = currentPos.getMinBlockZ();
        int maxX = currentPos.getMaxBlockX();
        int maxZ = currentPos.getMaxBlockZ();

        // Implicit start point at the cave origin (same radius as first waypoint)
        PathPoint startPt = new PathPoint(
                new BlockPos(openingX, startY, openingZ),
                waypoints.get(0).widthRadius(),
                waypoints.get(0).heightRadius());

        // Check if any segment's bounding box overlaps this chunk
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

        // Carve each segment by interpolating 1 block at a time.
        // Segments in the last 5 waypoints skip water placement (cave "dries out" at the far end).
        int waterCutoff = Math.max(0, waypoints.size() - 5);
        prev = startPt;
        for (int i = 0; i < waypoints.size(); i++) {
            PathPoint curr = waypoints.get(i);
            boolean withWater = i < waterCutoff;
            anyCarved |= carveSegment(chunk, minX, minZ, maxX, maxZ, carvingMask,
                    prev, curr, noiseSeed + i * 100, withWater);
            prev = curr;
        }

        // Decoration runs in every chunk that intersects the cave, not just the source chunk.
        // The RandomSource state after all geometry draws is deterministic per (source, target) pair,
        // so decoration is stable per chunk call even though it varies across chunks.
        if (anyCarved) {
            decoratePassage(chunk, minX, minZ, maxX, maxZ, random, startPt, waypoints);
        }

        return anyCarved;
    }

    /**
     * Carves a continuous tunnel between two control points by interpolating 1 block at a time.
     * If withWater is true, places a water source block 1 block below the ellipsoid centre at
     * every step — guaranteed to land in the carved interior of the passage (dist ≈ 0.16–0.25,
     * well inside the always-carved zone).
     */
    private boolean carveSegment(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                 CarvingMask carvingMask, PathPoint from, PathPoint to,
                                 int noiseSeed, boolean withWater) {
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
            float wr = (float) (from.widthRadius()  * (1.0 - t) + to.widthRadius()  * t);
            float hr = (float) (from.heightRadius() * (1.0 - t) + to.heightRadius() * t);

            // Quick bounds pre-check
            float ext = wr + 2f;
            if (bx + ext < minX || bx - ext > maxX || bz + ext < minZ || bz - ext > maxZ) continue;

            // Carve the passage ellipsoid
            anyCarved |= carveNoisyEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask,
                    bx, by, bz, wr, hr, noiseSeed + s * 7);

            // Water channel: place source 1 block below ellipsoid centre.
            // At (bx, by-1, bz) dist = (1/hr)² ≤ 0.25 — always carved, never skipped by noise.
            if (withWater && bx >= minX && bx <= maxX && bz >= minZ && bz <= maxZ) {
                int waterY = by - 1;
                if (waterY >= chunk.getMinBuildHeight() && waterY < chunk.getMaxBuildHeight()) {
                    BlockPos waterPos = new BlockPos(bx, waterY, bz);
                    if (chunk.getBlockState(waterPos).is(Blocks.CAVE_AIR)) {
                        chunk.setBlockState(waterPos, Blocks.WATER.defaultBlockState(), false);
                        chunk.markPosForPostprocessing(waterPos);
                    }
                }
            }
        }
        return anyCarved;
    }

    private void decoratePassage(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                 RandomSource rand, PathPoint startPt, List<PathPoint> waypoints) {
        // Y scan range: full height span of all waypoints, clamped to chunk
        int bMinY = chunk.getMaxBuildHeight(), bMaxY = chunk.getMinBuildHeight();
        for (PathPoint wp : waypoints) {
            int hr = (int) Math.ceil(wp.heightRadius()) + 3;
            bMinY = Math.min(bMinY, wp.center().getY() - hr);
            bMaxY = Math.max(bMaxY, wp.center().getY() + hr);
        }
        {
            int hr = (int) Math.ceil(startPt.heightRadius()) + 3;
            bMinY = Math.min(bMinY, startPt.center().getY() - hr);
            bMaxY = Math.max(bMaxY, startPt.center().getY() + hr);
        }
        bMinY = Math.max(chunk.getMinBuildHeight(), bMinY);
        bMaxY = Math.min(chunk.getMaxBuildHeight() - 1, bMaxY);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = bMinY; y <= bMaxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    boolean isCaveAir = chunk.getBlockState(pos).is(Blocks.CAVE_AIR);
                    boolean isWater   = chunk.getBlockState(pos).is(Blocks.WATER);
                    if (!isCaveAir && !isWater) continue;

                    if (isCaveAir) {
                        BlockPos above = pos.above();
                        BlockPos below = pos.below();
                        boolean solidAbove = above.getY() < chunk.getMaxBuildHeight()
                                && chunk.getBlockState(above).isSolid();
                        boolean solidBelow = below.getY() >= chunk.getMinBuildHeight()
                                && chunk.getBlockState(below).isSolid();

                        // Moss on ceiling (solid block directly above)
                        if (solidAbove && rand.nextFloat() < 0.30f) {
                            chunk.setBlockState(above, Blocks.MOSS_BLOCK.defaultBlockState(), false);
                            continue;
                        }

                        // Hanging roots (solid above, ≥2 cave-air below)
                        if (solidAbove && rand.nextFloat() < 0.15f) {
                            BlockPos b1 = pos.below();
                            BlockPos b2 = pos.below(2);
                            if (b1.getY() >= chunk.getMinBuildHeight() && b2.getY() >= chunk.getMinBuildHeight()
                                    && chunk.getBlockState(b1).is(Blocks.CAVE_AIR)
                                    && chunk.getBlockState(b2).is(Blocks.CAVE_AIR)) {
                                chunk.setBlockState(pos, Blocks.HANGING_ROOTS.defaultBlockState(), false);
                                continue;
                            }
                        }

                        // Clay on dry floor (away from the water channel)
                        if (solidBelow && rand.nextFloat() < 0.25f) {
                            boolean nearChannel = false;
                            // Check startPt as well — water runs from startPt through the first segment
                            int sdx = x - startPt.center().getX(), sdz = z - startPt.center().getZ();
                            if (sdx * sdx + sdz * sdz <= 2) nearChannel = true;
                            if (!nearChannel) {
                                for (PathPoint wp : waypoints) {
                                    int ddx = x - wp.center().getX(), ddz = z - wp.center().getZ();
                                    if (ddx * ddx + ddz * ddz <= 2) { nearChannel = true; break; }
                                }
                            }
                            if (!nearChannel)
                                chunk.setBlockState(below, Blocks.CLAY.defaultBlockState(), false);
                        }

                        // Upper-wall moss (at or above nearest path centre Y)
                        if (rand.nextFloat() < 0.20f) {
                            int nearestCy = Integer.MIN_VALUE;
                            for (PathPoint wp : waypoints) {
                                float ddx = x - wp.center().getX(), ddz = z - wp.center().getZ();
                                float rr = wp.widthRadius() + 2f;
                                if (ddx * ddx + ddz * ddz <= rr * rr)
                                    nearestCy = Math.max(nearestCy, wp.center().getY());
                            }
                            if (y >= nearestCy) {
                                for (Direction dir : Direction.Plane.HORIZONTAL) {
                                    int wx = x + dir.getStepX(), wz = z + dir.getStepZ();
                                    if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                                    BlockPos wallPos = new BlockPos(wx, y, wz);
                                    if (chunk.getBlockState(wallPos).isSolid()) {
                                        chunk.setBlockState(wallPos, Blocks.MOSS_BLOCK.defaultBlockState(), false);
                                        break;
                                    }
                                }
                            }
                        }

                    } else {
                        // Gravel under the water channel
                        BlockPos below = pos.below();
                        if (below.getY() >= chunk.getMinBuildHeight()
                                && chunk.getBlockState(below).isSolid()
                                && rand.nextFloat() < 0.40f) {
                            chunk.setBlockState(below, Blocks.GRAVEL.defaultBlockState(), false);
                        }
                    }
                }
            }
        }
    }
}
