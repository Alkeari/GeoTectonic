package net.alkeari.geotectonic.cave;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

import java.util.function.Function;

public abstract class AbstractGeoCarver<C extends CarverConfiguration> extends WorldCarver<C> {

    protected AbstractGeoCarver(Codec<C> codec) {
        super(codec);
    }

    protected float minTemperature() { return Float.NEGATIVE_INFINITY; }
    protected float maxTemperature() { return Float.POSITIVE_INFINITY; }

    protected boolean isTemperatureValid(Function<BlockPos, Holder<Biome>> biomeAccessor, int x, int z) {
        float temp = biomeAccessor.apply(new BlockPos(x, 64, z)).value().getBaseTemperature();
        return temp >= minTemperature() && temp <= maxTemperature();
    }

    protected abstract BlockState caveAirState();

    protected boolean shouldCarveBlock(BlockState state) {
        if (state.isAir()) return false;
        if (state.is(Blocks.BEDROCK)) return false;
        var fluid = state.getFluidState();
        if (fluid.is(FluidTags.LAVA)) return false;
        if (fluid.is(FluidTags.WATER)) return false;
        return true;
    }

    protected boolean carveEllipsoid(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                     CarvingMask carvingMask, int cx, int cy, int cz,
                                     float wr, float hr) {
        if (wr <= 0f || hr <= 0f) return false;
        boolean anyCarved = false;
        int wrCeil = (int) Math.ceil(wr);
        int hrCeil = (int) Math.ceil(hr);

        for (int dy = -hrCeil; dy <= hrCeil; dy++) {
            int blockY = cy + dy;
            if (blockY < chunk.getMinBuildHeight() || blockY >= chunk.getMaxBuildHeight()) continue;

            float yFrac = (float) dy / hr;
            float xyRemainder = 1f - yFrac * yFrac;
            if (xyRemainder <= 0f) continue;

            for (int dx = -wrCeil; dx <= wrCeil; dx++) {
                int blockX = cx + dx;
                if (blockX < minX || blockX > maxX) continue;

                for (int dz = -wrCeil; dz <= wrCeil; dz++) {
                    int blockZ = cz + dz;
                    if (blockZ < minZ || blockZ > maxZ) continue;

                    float xFrac = (float) dx / wr;
                    float zFrac = (float) dz / wr;
                    if (xFrac * xFrac + zFrac * zFrac > xyRemainder) continue;

                    int localX = blockX - minX;
                    int localZ = blockZ - minZ;
                    if (carvingMask.get(localX, blockY, localZ)) continue;

                    BlockPos pos = new BlockPos(blockX, blockY, blockZ);
                    BlockState state = chunk.getBlockState(pos);
                    if (!shouldCarveBlock(state)) continue;

                    chunk.setBlockState(pos, caveAirState(), false);
                    carvingMask.set(localX, blockY, localZ);
                    anyCarved = true;
                }
            }
        }
        return anyCarved;
    }

    protected boolean carveNoisyEllipsoid(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                          CarvingMask carvingMask, int cx, int cy, int cz,
                                          float wr, float hr, int noiseSeed) {
        if (wr <= 0f || hr <= 0f) return false;
        boolean anyCarved = false;
        int wrCeil = (int) Math.ceil(wr) + 1;
        int hrCeil = (int) Math.ceil(hr) + 1;

        for (int dy = -hrCeil; dy <= hrCeil; dy++) {
            int blockY = cy + dy;
            if (blockY < chunk.getMinBuildHeight() || blockY >= chunk.getMaxBuildHeight()) continue;
            float yFrac    = (float) dy / hr;
            float yFracSq  = yFrac * yFrac;
            if (yFracSq > 1.25f) continue;

            for (int dx = -wrCeil; dx <= wrCeil; dx++) {
                int blockX = cx + dx;
                if (blockX < minX || blockX > maxX) continue;
                float xFrac = (float) dx / wr;

                for (int dz = -wrCeil; dz <= wrCeil; dz++) {
                    int blockZ = cz + dz;
                    if (blockZ < minZ || blockZ > maxZ) continue;
                    float zFrac = (float) dz / wr;

                    float dist = xFrac * xFrac + yFracSq + zFrac * zFrac;
                    if (dist > 1.15f) continue;

                    if (dist > 0.72f) {
                        int h = posHash(blockX, blockY, blockZ, noiseSeed);
                        if (dist > 1.0f) {
                            float chance = 0.45f * (1.15f - dist) / 0.15f;
                            if ((h & 0xFF) > (int)(chance * 255)) continue;
                        } else {
                            float skipChance = (dist - 0.72f) / 0.28f * 0.55f;
                            if ((h & 0xFF) < (int)(skipChance * 255)) continue;
                        }
                    }

                    int localX = blockX - minX;
                    int localZ = blockZ - minZ;
                    if (carvingMask.get(localX, blockY, localZ)) continue;
                    if (!shouldCarveBlock(chunk.getBlockState(new BlockPos(blockX, blockY, blockZ)))) continue;

                    chunk.setBlockState(new BlockPos(blockX, blockY, blockZ),
                            caveAirState(), false);
                    carvingMask.set(localX, blockY, localZ);
                    anyCarved = true;
                }
            }
        }
        return anyCarved;
    }

    protected static int posHash(int x, int y, int z, int seed) {
        int h = x * 1619 + y * 31337 + z * 6971 + seed;
        h ^= h >>> 14;
        h *= 1540483477;
        h ^= h >>> 24;
        return h;
    }
}
