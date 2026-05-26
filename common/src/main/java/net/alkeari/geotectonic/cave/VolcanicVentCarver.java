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

import java.util.function.Function;

public class VolcanicVentCarver extends AbstractGeoCarver<VolcanicVentCarverConfig> {

    public VolcanicVentCarver(Codec<VolcanicVentCarverConfig> codec) {
        super(codec);
    }

    @Override
    protected BlockState caveAirState() {
        return GeoTectonicBlocks.VOLCANIC_VENT_AIR.get().defaultBlockState();
    }

    @Override
    public int getRange() { return 4; }

    @Override
    public boolean isStartChunk(VolcanicVentCarverConfig config, RandomSource random) {
        double prob;
        try { prob = ModConfig.getVolcanicVentProbability(); }
        catch (Throwable t) { prob = config.probability; }
        return ModConfig.isVolcanicVentEnabled() && random.nextFloat() < prob;
    }

    @Override
    public boolean carve(CarvingContext context, VolcanicVentCarverConfig config, ChunkAccess chunk,
                         Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource random,
                         Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask carvingMask) {

        // All geometry drawn up-front before any chunk-bounds checks
        int centerX = sourceChunkPos.getMinBlockX() + 4 + random.nextInt(8);
        int centerZ = sourceChunkPos.getMinBlockZ() + 4 + random.nextInt(8);
        int chamberY       = -45 + (random.nextInt(7) - 3);  // Y -48 to -42
        int chamberWidth   = 12 + random.nextInt(4);          // 12–15
        int chamberHeight  = 6  + random.nextInt(3);          // 6–8
        int chimneyHeight  = 18 + random.nextInt(5);          // 18–22

        int noiseSeed = centerX * 73856093 ^ centerZ * 19349663;

        ChunkPos currentPos = chunk.getPos();
        int minX = currentPos.getMinBlockX();
        int minZ = currentPos.getMinBlockZ();
        int maxX = currentPos.getMaxBlockX();
        int maxZ = currentPos.getMaxBlockZ();

        float chamberWr = chamberWidth / 2.0f;
        float chamberHr = chamberHeight / 2.0f;
        int chamberCenter = chamberY + chamberHeight / 2;
        int chimneyBase   = chamberY + chamberHeight;
        int chimneyTop    = chimneyBase + chimneyHeight;

        float maxExtent = chamberWr + 2f;
        boolean chamberOverlaps = centerX + maxExtent >= minX && centerX - maxExtent <= maxX
                && centerZ + maxExtent >= minZ && centerZ - maxExtent <= maxZ;
        boolean chimneyOverlaps = centerX + 4 >= minX && centerX - 4 <= maxX
                && centerZ + 4 >= minZ && centerZ - 4 <= maxZ;

        if (!chamberOverlaps && !chimneyOverlaps) return false;

        boolean anyCarved = false;

        // Carve chamber
        if (chamberOverlaps) {
            anyCarved |= carveNoisyEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask,
                    centerX, chamberCenter, centerZ, chamberWr, chamberHr, noiseSeed);
        }

        // Carve chimney: linearly interpolate radius 1.5 → 1.0
        if (chimneyOverlaps) {
            for (int y = chimneyBase; y <= chimneyTop; y++) {
                float t = (float) (y - chimneyBase) / Math.max(1, chimneyHeight);
                float r = 1.5f * (1.0f - t) + 1.0f * t;
                anyCarved |= carveEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask,
                        centerX, y, centerZ, r, 0.6f);
            }
            // Top pocket
            anyCarved |= carveNoisyEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask,
                    centerX, chimneyTop, centerZ, 1.5f, 1.5f, noiseSeed + 13);
        }

        if (anyCarved) {
            decorateVolcanicVent(chunk, minX, minZ, maxX, maxZ,
                    centerX, centerZ, chamberY, chamberCenter, chamberHeight,
                    chimneyBase, chimneyTop, noiseSeed);
        }

        return anyCarved;
    }

    private void decorateVolcanicVent(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                      int centerX, int centerZ,
                                      int chamberY, int chamberCenter, int chamberHeight,
                                      int chimneyBase, int chimneyTop, int noiseSeed) {
        int scanMinY = Math.max(chunk.getMinBuildHeight(), chamberY - 4);
        int scanMaxY = Math.min(chunk.getMaxBuildHeight() - 1, chimneyTop + 2);

        int soulFirePlaced = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = scanMinY; y <= scanMaxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!chunk.getBlockState(pos).is(GeoTectonicBlocks.VOLCANIC_VENT_AIR.get())) continue;

                    int h = posHash(x, y, z, noiseSeed);
                    int b0 = h & 0xFF;
                    int b1 = (h >> 8) & 0xFF;

                    BlockPos abovePos = pos.above();
                    BlockPos belowPos = pos.below();
                    boolean solidAbove = abovePos.getY() < chunk.getMaxBuildHeight()
                            && chunk.getBlockState(abovePos).isSolid();
                    boolean solidBelow = belowPos.getY() >= chunk.getMinBuildHeight()
                            && chunk.getBlockState(belowPos).isSolid();

                    boolean inChamber  = y >= chamberY && y <= chamberY + chamberHeight;
                    boolean inChimney  = y >= chimneyBase && y < chimneyTop;
                    boolean inTopPocket = y >= chimneyTop;

                    if (inChamber) {
                        // Chamber ceiling: basalt 95%, gilded_blackstone 5%
                        if (solidAbove) {
                            if (b0 < 243) chunk.setBlockState(abovePos, Blocks.BASALT.defaultBlockState(), false);
                            else          chunk.setBlockState(abovePos, Blocks.GILDED_BLACKSTONE.defaultBlockState(), false);
                        }

                        // Chamber floor: magma 50%, blackstone 50%
                        if (solidBelow) {
                            if (b1 < 128) chunk.setBlockState(belowPos, Blocks.MAGMA_BLOCK.defaultBlockState(), false);
                            else          chunk.setBlockState(belowPos, Blocks.BLACKSTONE.defaultBlockState(), false);
                        }

                        // Chamber walls: blackstone 60%, basalt 25%, deepslate 15%
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            int wx = x + dir.getStepX(), wz = z + dir.getStepZ();
                            if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                            BlockPos wallPos = new BlockPos(wx, y, wz);
                            if (!chunk.getBlockState(wallPos).isSolid()) continue;
                            int wh = posHash(wx, y, wz, noiseSeed + 3);
                            int wb = wh & 0xFF;
                            if      (wb < 153) chunk.setBlockState(wallPos, Blocks.BLACKSTONE.defaultBlockState(), false);
                            else if (wb < 217) chunk.setBlockState(wallPos, Blocks.BASALT.defaultBlockState(), false);
                            else               chunk.setBlockState(wallPos, Blocks.DEEPSLATE.defaultBlockState(), false);
                        }

                    } else if (inChimney) {
                        // Chimney walls: blackstone 70%, basalt 20%, deepslate 10%
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            int wx = x + dir.getStepX(), wz = z + dir.getStepZ();
                            if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                            BlockPos wallPos = new BlockPos(wx, y, wz);
                            if (!chunk.getBlockState(wallPos).isSolid()) continue;
                            int wh = posHash(wx, y, wz, noiseSeed + 5);
                            int wb = wh & 0xFF;
                            if      (wb < 179) chunk.setBlockState(wallPos, Blocks.BLACKSTONE.defaultBlockState(), false);
                            else if (wb < 230) chunk.setBlockState(wallPos, Blocks.BASALT.defaultBlockState(), false);
                            else               chunk.setBlockState(wallPos, Blocks.DEEPSLATE.defaultBlockState(), false);
                        }

                    } else if (inTopPocket) {
                        // Top pocket: blackstone walls/ceiling, magma floor
                        if (solidAbove) chunk.setBlockState(abovePos, Blocks.BLACKSTONE.defaultBlockState(), false);
                        if (solidBelow) chunk.setBlockState(belowPos, Blocks.MAGMA_BLOCK.defaultBlockState(), false);
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            int wx = x + dir.getStepX(), wz = z + dir.getStepZ();
                            if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                            BlockPos wallPos = new BlockPos(wx, y, wz);
                            if (chunk.getBlockState(wallPos).isSolid())
                                chunk.setBlockState(wallPos, Blocks.BLACKSTONE.defaultBlockState(), false);
                        }
                    }

                    // Soul fire: place above basalt wall blocks in chamber (up to 3 total)
                    if (inChamber && soulFirePlaced < 3) {
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            if (soulFirePlaced >= 3) break;
                            int wx = x + dir.getStepX(), wz = z + dir.getStepZ();
                            if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                            BlockPos wallPos = new BlockPos(wx, y, wz);
                            if (!chunk.getBlockState(wallPos).is(Blocks.BASALT)) continue;
                            BlockPos aboveWall = wallPos.above();
                            if (aboveWall.getY() >= chunk.getMaxBuildHeight()) continue;
                            if (!chunk.getBlockState(aboveWall).is(GeoTectonicBlocks.VOLCANIC_VENT_AIR.get())) continue;
                            int sh = posHash(wx, y, wz, noiseSeed + 11);
                            if ((sh & 0xFF) < 40) {
                                chunk.setBlockState(aboveWall, Blocks.SOUL_FIRE.defaultBlockState(), false);
                                chunk.markPosForPostprocessing(aboveWall);
                                soulFirePlaced++;
                            }
                        }
                    }
                }
            }
        }

        // ONE lava source at geometric center of chamber floor
        placeLavaSource(chunk, minX, minZ, maxX, maxZ, centerX, centerZ, chamberY, chamberHeight);
    }

    private void placeLavaSource(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                  int centerX, int centerZ, int chamberY, int chamberHeight) {
        if (centerX < minX || centerX > maxX || centerZ < minZ || centerZ > maxZ) return;

        for (int dy = 0; dy >= -chamberHeight; dy--) {
            int y = chamberY + dy;
            if (y < chunk.getMinBuildHeight()) break;
            BlockPos pos = new BlockPos(centerX, y, centerZ);
            if (!chunk.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) continue;
            BlockPos above = pos.above();
            if (above.getY() >= chunk.getMaxBuildHeight()) continue;
            if (!chunk.getBlockState(above).is(GeoTectonicBlocks.VOLCANIC_VENT_AIR.get())) continue;
            chunk.setBlockState(pos, Blocks.LAVA.defaultBlockState(), false);
            chunk.markPosForPostprocessing(pos);
            return;
        }
    }
}
