package net.alkeari.geotectonic.cave;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
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

public class KarstCarver extends AbstractGeoCarver<KarstCarverConfig> {

    public KarstCarver(Codec<KarstCarverConfig> codec) {
        super(codec);
    }

    @Override
    protected float minTemperature() { return 0.2f; }

    @Override
    protected float maxTemperature() { return 1.5f; }

    @Override
    public int getRange() { return 32; }

    @Override
    public boolean isStartChunk(KarstCarverConfig config, RandomSource random) {
        return random.nextFloat() < config.probability;
    }

    @Override
    public boolean carve(CarvingContext context, KarstCarverConfig config, ChunkAccess chunk,
                         Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource random,
                         Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask carvingMask) {

        int openingX = sourceChunkPos.getMinBlockX() + random.nextInt(16);
        int openingZ = sourceChunkPos.getMinBlockZ() + random.nextInt(16);

        if (!isTemperatureValid(biomeAccessor, openingX, openingZ)) return false;

        int startY = 40; // fixed underground; shaft from terrain surface connects above
        KarstLayout layout = KarstChamberGenerator.generate(random, openingX, openingZ, startY);

        ChunkPos currentPos = chunk.getPos();
        int minX = currentPos.getMinBlockX();
        int minZ = currentPos.getMinBlockZ();
        int maxX = currentPos.getMaxBlockX();
        int maxZ = currentPos.getMaxBlockZ();

        boolean anyCarved = false;

        anyCarved |= carvePoints(layout.crackPoints(),        chunk, minX, minZ, maxX, maxZ, carvingMask);
        anyCarved |= carvePoints(List.of(layout.mainChamber()), chunk, minX, minZ, maxX, maxZ, carvingMask);
        anyCarved |= carvePoints(layout.satellites(),         chunk, minX, minZ, maxX, maxZ, carvingMask);
        anyCarved |= carvePoints(layout.passages(),           chunk, minX, minZ, maxX, maxZ, carvingMask);

        if (anyCarved) {
            PathPoint m = layout.mainChamber();
            KarstDecorator.decorate(chunk, random, minX, minZ, maxX, maxZ,
                m.center().getX(), m.center().getY(), m.center().getZ(),
                m.widthRadius(), m.heightRadius());
        }

        return anyCarved;
    }

    private boolean carvePoints(Iterable<PathPoint> points, ChunkAccess chunk,
                                int minX, int minZ, int maxX, int maxZ, CarvingMask mask) {
        boolean any = false;
        for (PathPoint p : points) {
            int cx = p.center().getX(), cy = p.center().getY(), cz = p.center().getZ();
            float wr = p.widthRadius(), hr = p.heightRadius();
            if (cx + wr < minX || cx - wr > maxX || cz + wr < minZ || cz - wr > maxZ) continue;
            any |= carveEllipsoid(chunk, minX, minZ, maxX, maxZ, mask, cx, cy, cz, wr, hr);
        }
        return any;
    }
}
