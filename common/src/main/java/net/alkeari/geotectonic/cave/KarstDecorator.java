package net.alkeari.geotectonic.cave;

import net.alkeari.geotectonic.registry.GeoTectonicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.chunk.ChunkAccess;

public class KarstDecorator {

    private static final BlockState STALACTITE_TIP = Blocks.POINTED_DRIPSTONE.defaultBlockState()
        .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
        .setValue(PointedDripstoneBlock.THICKNESS,     DripstoneThickness.TIP)
        .setValue(PointedDripstoneBlock.WATERLOGGED,   false);

    private static final BlockState STALAGMITE_TIP = Blocks.POINTED_DRIPSTONE.defaultBlockState()
        .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
        .setValue(PointedDripstoneBlock.THICKNESS,     DripstoneThickness.TIP)
        .setValue(PointedDripstoneBlock.WATERLOGGED,   false);

    private static final BlockState CAVE_VINE_TIP =
        Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVinesBlock.BERRIES, false);

    private static final BlockState CALCITE = Blocks.CALCITE.defaultBlockState();
    private static final BlockState WATER   = Blocks.WATER.defaultBlockState();

    public static void decorate(ChunkAccess chunk, RandomSource rand,
                                int minX, int minZ, int maxX, int maxZ,
                                int hubX, int hubY, int hubZ,
                                float mainWr, float mainHr) {
        // Find lowest cave-air with solid floor inside the hub XZ footprint (water pool target Y)
        int waterY = Integer.MAX_VALUE;
        int hubRadius = (int) mainWr + 1;
        int searchMinX = Math.max(minX, hubX - hubRadius);
        int searchMaxX = Math.min(maxX, hubX + hubRadius);
        int searchMinZ = Math.max(minZ, hubZ - hubRadius);
        int searchMaxZ = Math.min(maxZ, hubZ + hubRadius);

        for (int x = searchMinX; x <= searchMaxX; x++) {
            for (int z = searchMinZ; z <= searchMaxZ; z++) {
                int scanMin = hubY - (int) mainHr - 1;
                int scanMax = hubY + (int) mainHr + 1;
                for (int y = Math.max(chunk.getMinBuildHeight(), scanMin);
                     y <= Math.min(chunk.getMaxBuildHeight() - 1, scanMax); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (chunk.getBlockState(pos).is(GeoTectonicBlocks.KARST_AIR.get())
                            && chunk.getBlockState(pos.below()).isSolid()) {
                        waterY = Math.min(waterY, y);
                    }
                }
            }
        }

        // Scan entire chunk for decoration opportunities
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!chunk.getBlockState(pos).is(GeoTectonicBlocks.KARST_AIR.get())) continue;

                    BlockState above = chunk.getBlockState(pos.above());
                    BlockState below = chunk.getBlockState(pos.below());

                    // Stalactite/vine: hang from solid non-dripstone ceiling only
                    if (above.isSolid() && !above.is(Blocks.POINTED_DRIPSTONE)) {
                        float r = rand.nextFloat();
                        if (r < 0.12f) {
                            chunk.setBlockState(pos, STALACTITE_TIP, false);
                            continue;
                        } else if (r < 0.20f) {
                            chunk.setBlockState(pos, CAVE_VINE_TIP, false);
                            continue;
                        }
                    }

                    // Stalagmite: grow from solid non-dripstone floor only
                    if (below.isSolid() && !below.is(Blocks.POINTED_DRIPSTONE) && rand.nextFloat() < 0.07f) {
                        chunk.setBlockState(pos, STALAGMITE_TIP, false);
                        continue;
                    }

                    // Calcite on surrounding wall blocks (skip out-of-chunk neighbours)
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int wx = x + dir.getStepX();
                        int wz = z + dir.getStepZ();
                        if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                        BlockPos wallPos = new BlockPos(wx, y, wz);
                        if (chunk.getBlockState(wallPos).isSolid() && rand.nextFloat() < 0.20f) {
                            chunk.setBlockState(wallPos, CALCITE, false);
                        }
                    }

                    // Water pool at lowest karst floor in hub area (hub XZ footprint only)
                    if (waterY != Integer.MAX_VALUE && y == waterY
                            && x >= searchMinX && x <= searchMaxX
                            && z >= searchMinZ && z <= searchMaxZ) {
                        chunk.setBlockState(pos, WATER, false);
                        chunk.markPosForPostprocessing(pos);
                    }
                }
            }
        }
    }
}
