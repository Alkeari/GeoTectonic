package net.alkeari.geotectonic.cave;

import net.alkeari.geotectonic.registry.GeoTectonicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class VolcanicDecorator {

    private static final BlockState BASALT = Blocks.BASALT.defaultBlockState();
    private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();
    private static final BlockState DRIPSTONE = Blocks.DRIPSTONE_BLOCK.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();

    public static void decorate(ChunkAccess chunk, RandomSource rand, int minX, int minZ) {
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = chunk.getMaxBuildHeight() - 1; y >= -60; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!chunk.getBlockState(pos).is(GeoTectonicBlocks.FRACTURE_AIR.get())) continue;

                    BlockPos belowPos = pos.below();
                    BlockState below = chunk.getBlockState(belowPos);
                    BlockPos abovePos = pos.above();
                    BlockState above = chunk.getBlockState(abovePos);

                    if (below.isSolid()) {
                        placeFloor(chunk, rand, belowPos, y);
                    }

                    if (above.isSolid()) {
                        placeCeiling(chunk, rand, abovePos, y);
                    }

                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        BlockPos wallPos = pos.relative(dir);
                        if (chunk.getBlockState(wallPos).isSolid()) {
                            placeWall(chunk, rand, wallPos, y);
                        }
                    }
                }
            }
        }
    }

    private static void placeFloor(ChunkAccess chunk, RandomSource rand, BlockPos floorPos, int airY) {
        if (airY >= -20) {
            // Warm: no floor changes
        } else if (airY >= -40) {
            // Hot: 25% → MAGMA
            if (rand.nextFloat() < 0.25f) chunk.setBlockState(floorPos, MAGMA, false);
        } else {
            // Volcanic: 60% → MAGMA, then sub-check for lava pool
            if (rand.nextFloat() < 0.60f) {
                chunk.setBlockState(floorPos, MAGMA, false);
                // Lava pool: local Y minimum (solid below floor) with 45% chance
                if (chunk.getBlockState(floorPos.below()).isSolid() && rand.nextFloat() < 0.45f) {
                    chunk.setBlockState(floorPos, LAVA, false);
                    chunk.markPosForPostprocessing(floorPos);
                }
            }
        }
    }

    private static void placeCeiling(ChunkAccess chunk, RandomSource rand, BlockPos ceilPos, int airY) {
        // Hot band only
        if (airY >= -40 && airY < -20) {
            if (rand.nextFloat() < 0.15f) chunk.setBlockState(ceilPos, DRIPSTONE, false);
        }
    }

    private static void placeWall(ChunkAccess chunk, RandomSource rand, BlockPos wallPos, int airY) {
        float r = rand.nextFloat();
        if (airY > 64) {
            // Chimney: 5% basalt walls — crack is cool/dry near the surface
            if (r < 0.05f) chunk.setBlockState(wallPos, BASALT, false);
        } else if (airY >= -20) {
            // Warm: 10% → BASALT
            if (r < 0.10f) chunk.setBlockState(wallPos, BASALT, false);
        } else if (airY >= -40) {
            // Hot: 20% → BASALT, next 10% → MAGMA
            if (r < 0.20f) chunk.setBlockState(wallPos, BASALT, false);
            else if (r < 0.30f) chunk.setBlockState(wallPos, MAGMA, false);
        } else {
            // Volcanic: 40% → BASALT, next 20% → MAGMA, next 5% → LAVA fall
            if (r < 0.40f) {
                chunk.setBlockState(wallPos, BASALT, false);
            } else if (r < 0.60f) {
                chunk.setBlockState(wallPos, MAGMA, false);
            } else if (r < 0.65f) {
                // Lava fall: only if air exists above wall block
                if (chunk.getBlockState(wallPos.above()).is(GeoTectonicBlocks.FRACTURE_AIR.get())) {
                    chunk.setBlockState(wallPos, LAVA, false);
                    chunk.markPosForPostprocessing(wallPos);
                }
            }
        }
    }
}
