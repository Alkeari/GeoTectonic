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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

import java.util.List;
import java.util.function.Function;

public class KarstCarver extends AbstractGeoCarver<KarstCarverConfig> {

    public KarstCarver(Codec<KarstCarverConfig> codec) {
        super(codec);
    }

    @Override
    protected BlockState caveAirState() {
        return GeoTectonicBlocks.KARST_AIR.get().defaultBlockState();
    }

    @Override
    protected float minTemperature() { return 0.2f; }

    @Override
    protected float maxTemperature() { return 1.5f; }

    @Override
    public int getRange() { return 32; }

    @Override
    public boolean isStartChunk(KarstCarverConfig config, RandomSource random) {
        double prob;
        try { prob = ModConfig.getKarstProbability(); }
        catch (Throwable t) { prob = config.probability; }
        return ModConfig.isKarstEnabled() && random.nextFloat() < prob;
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

        // Surface crack: position-hash noise only — no RandomSource draws — chunk-consistent
        int noiseSeed = openingX * 73856093 ^ openingZ * 19349663;
        boolean crackOverlaps = openingX + 100 >= minX && openingX - 100 <= maxX
                && openingZ + 100 >= minZ && openingZ - 100 <= maxZ;
        if (crackOverlaps) {
            int localX = Math.min(15, Math.max(0, openingX - minX));
            int localZ = Math.min(15, Math.max(0, openingZ - minZ));
            int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ);
            carveSurfaceCrack(chunk, minX, minZ, maxX, maxZ, carvingMask,
                    openingX, openingZ, surfaceY, noiseSeed);
            anyCarved = true;
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

    private void carveSurfaceCrack(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                    CarvingMask carvingMask, int openingX, int openingZ,
                                    int surfaceY, int noiseSeed) {
        int crackBase = 40;
        int totalH = Math.max(1, surfaceY - crackBase);

        // Determine drift direction from noise (no RandomSource — deterministic across chunks)
        float driftAngle = ((noiseSeed ^ (noiseSeed >>> 13)) & 7) * (float) (Math.PI / 4);
        // Surface start is offset from openingX/Z, converges to openingX/Z at Y=crackBase
        int totalDriftX = (int) (Math.cos(driftAngle) * totalH * (2.0f / 3.0f));
        int totalDriftZ = (int) (Math.sin(driftAngle) * totalH * (2.0f / 3.0f));
        int surfaceX = openingX + totalDriftX;
        int surfaceZ = openingZ + totalDriftZ;

        for (int y = surfaceY; y >= crackBase; y--) {
            float t = (float) (surfaceY - y) / totalH; // 0.0 at surface, 1.0 at crackBase
            float wr = 1.5f + t * 1.0f;
            float hr = 2.0f + t * 1.0f;
            int crackX = surfaceX + (int) ((openingX - surfaceX) * t);
            int crackZ = surfaceZ + (int) ((openingZ - surfaceZ) * t);
            carveNoisyEllipsoid(chunk, minX, minZ, maxX, maxZ, carvingMask,
                    crackX, y, crackZ, wr, hr, noiseSeed + y * 13);
        }
    }
}
