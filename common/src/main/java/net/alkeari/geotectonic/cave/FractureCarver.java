package net.alkeari.geotectonic.cave;

import com.mojang.serialization.Codec;
import net.alkeari.geotectonic.config.ModConfig;
import net.alkeari.geotectonic.registry.GeoTectonicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

import java.util.List;
import java.util.function.Function;

public class FractureCarver extends AbstractGeoCarver<FractureCarverConfig> {

    private static final int DRIFT_START_Y = 64;

    public FractureCarver(Codec<FractureCarverConfig> codec) {
        super(codec);
    }

    @Override
    protected BlockState caveAirState() {
        return GeoTectonicBlocks.FRACTURE_AIR.get().defaultBlockState();
    }

    @Override
    public int getRange() {
        return 50;
    }

    @Override
    public boolean isStartChunk(FractureCarverConfig config, RandomSource random) {
        double prob;
        try { prob = ModConfig.getFractureProbability(); }
        catch (Throwable t) { prob = config.probability; }
        return ModConfig.isFractureEnabled() && random.nextFloat() < prob;
    }

    @Override
    public boolean carve(CarvingContext context, FractureCarverConfig config, ChunkAccess chunk,
                         Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource random,
                         Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask carvingMask) {

        int openingX = sourceChunkPos.getMinBlockX() + random.nextInt(16);
        int openingZ = sourceChunkPos.getMinBlockZ() + random.nextInt(16);
        int startY = context.getMinGenY() + context.getGenDepth() - 1;

        List<PathPoint> path = FracturePathGenerator.generate(random, openingX, openingZ, startY, DRIFT_START_Y);

        ChunkPos currentPos = chunk.getPos();
        int minX = currentPos.getMinBlockX();
        int minZ = currentPos.getMinBlockZ();
        int maxX = currentPos.getMaxBlockX();
        int maxZ = currentPos.getMaxBlockZ();

        boolean anyCarved = false;

        for (PathPoint point : path) {
            int cx = point.center().getX();
            int cy = point.center().getY();
            int cz = point.center().getZ();
            float wr = point.widthRadius();
            float hr = point.heightRadius();

            if (cx + wr < minX || cx - wr > maxX || cz + wr < minZ || cz - wr > maxZ) continue;
            anyCarved |= carveEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask, cx, cy, cz, wr, hr);
        }

        if (anyCarved) {
            VolcanicDecorator.decorate(chunk, random, minX, minZ);
        }

        return anyCarved;
    }
}
