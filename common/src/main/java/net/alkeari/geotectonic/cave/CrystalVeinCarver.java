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
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

import java.util.List;
import java.util.function.Function;

public class CrystalVeinCarver extends AbstractGeoCarver<CrystalVeinCarverConfig> {

    public CrystalVeinCarver(Codec<CrystalVeinCarverConfig> codec) {
        super(codec);
    }

    @Override
    protected BlockState caveAirState() {
        return GeoTectonicBlocks.CRYSTAL_VEIN_AIR.get().defaultBlockState();
    }

    @Override
    public int getRange() { return 6; }

    @Override
    public boolean isStartChunk(CrystalVeinCarverConfig config, RandomSource random) {
        double prob;
        try { prob = ModConfig.getCrystalVeinProbability(); }
        catch (Throwable t) { prob = config.probability; }
        return ModConfig.isCrystalVeinEnabled() && random.nextFloat() < prob;
    }

    @Override
    public boolean carve(CarvingContext context, CrystalVeinCarverConfig config, ChunkAccess chunk,
                         Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource random,
                         Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask carvingMask) {

        int openingX  = sourceChunkPos.getMinBlockX() + random.nextInt(16);
        int openingZ  = sourceChunkPos.getMinBlockZ() + random.nextInt(16);
        int caveY     = -40 + random.nextInt(26);               // Y -40 to -15
        int headingDeg = random.nextInt(4) * 90;               // cardinal only

        List<PathPoint> waypoints = CrystalVeinPathGenerator.generate(random, openingX, openingZ, caveY, headingDeg);
        if (waypoints.isEmpty()) return false;

        int noiseSeed = openingX * 73856093 ^ openingZ * 19349663;

        ChunkPos currentPos = chunk.getPos();
        int minX = currentPos.getMinBlockX();
        int minZ = currentPos.getMinBlockZ();
        int maxX = currentPos.getMaxBlockX();
        int maxZ = currentPos.getMaxBlockZ();

        PathPoint startPt = new PathPoint(new BlockPos(openingX, caveY, openingZ),
            waypoints.get(0).widthRadius(), waypoints.get(0).heightRadius());

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

        if (anyCarved) {
            decorateCrystalVein(chunk, minX, minZ, maxX, maxZ, startPt, waypoints, headingDeg, noiseSeed);
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

    private void decorateCrystalVein(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                     PathPoint startPt, List<PathPoint> waypoints,
                                     int headingDeg, int noiseSeed) {
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

        PathPoint alcovePt = waypoints.get(waypoints.size() - 1);
        float alcoveR = alcovePt.widthRadius() + 3f;

        // Cave-in fill: 2-block-deep zone on the OPPOSITE side of the first waypoint
        // from the path heading. Opposite heading = headingDeg + 180.
        double caveInRad = Math.toRadians((headingDeg + 180) % 360);
        int caveInDX = (int) Math.round(Math.sin(caveInRad) * 2);
        int caveInDZ = (int) Math.round(-Math.cos(caveInRad) * 2);
        int caveInCX = startPt.center().getX() + caveInDX;
        int caveInCY = startPt.center().getY();
        int caveInCZ = startPt.center().getZ() + caveInDZ;
        float caveInR = startPt.widthRadius() + 1f;

        // Fill cave-in zone
        int wrCeil = (int) Math.ceil(caveInR) + 1;
        int hrCeil = (int) Math.ceil(startPt.heightRadius()) + 1;
        for (int dy = -hrCeil; dy <= hrCeil; dy++) {
            int by = caveInCY + dy;
            if (by < chunk.getMinBuildHeight() || by >= chunk.getMaxBuildHeight()) continue;
            for (int dx = -wrCeil; dx <= wrCeil; dx++) {
                int bx = caveInCX + dx;
                if (bx < minX || bx > maxX) continue;
                for (int dz = -wrCeil; dz <= wrCeil; dz++) {
                    int bz = caveInCZ + dz;
                    if (bz < minZ || bz > maxZ) continue;
                    float xf = (float) dx / caveInR;
                    float yf = (float) dy / startPt.heightRadius();
                    float zf = (float) dz / caveInR;
                    if (xf * xf + yf * yf + zf * zf > 1.0f) continue;
                    BlockPos pos = new BlockPos(bx, by, bz);
                    if (!chunk.getBlockState(pos).is(GeoTectonicBlocks.CRYSTAL_VEIN_AIR.get())) continue;

                    int h = posHash(bx, by, bz, noiseSeed + 9999);
                    int b = h & 0xFF;
                    BlockState fill;
                    if      (b < 128) fill = Blocks.COBBLESTONE.defaultBlockState();
                    else if (b < 204) fill = Blocks.DEEPSLATE.defaultBlockState();
                    else if (b < 242) fill = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                    else              fill = Blocks.GRAVEL.defaultBlockState();
                    chunk.setBlockState(pos, fill, false);
                }
            }
        }

        // Cobwebs in cave-in zone (up to 3, attachment-validated)
        int cobwebsPlaced = 0;
        for (int dx = -wrCeil; dx <= wrCeil && cobwebsPlaced < 3; dx++) {
            for (int dz = -wrCeil; dz <= wrCeil && cobwebsPlaced < 3; dz++) {
                int bx = caveInCX + dx; int bz = caveInCZ + dz;
                if (bx < minX || bx > maxX || bz < minZ || bz > maxZ) continue;
                int h = posHash(bx, caveInCY, bz, noiseSeed + 8888);
                if ((h & 0xFF) >= 60) continue;
                BlockPos pos = new BlockPos(bx, caveInCY, bz);
                if (!chunk.getBlockState(pos).is(GeoTectonicBlocks.CRYSTAL_VEIN_AIR.get())) continue;
                if (!chunk.getBlockState(pos.above()).isSolid()) continue;
                chunk.setBlockState(pos, Blocks.COBWEB.defaultBlockState(), false);
                cobwebsPlaced++;
            }
        }

        // Main decoration scan
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                float dcx = x - alcovePt.center().getX();
                float dcz = z - alcovePt.center().getZ();
                boolean inAlcove = dcx * dcx + dcz * dcz <= alcoveR * alcoveR;

                float scx = x - startPt.center().getX();
                float scz = z - startPt.center().getZ();
                boolean inCaveIn = scx * scx + scz * scz <= (caveInR + 1) * (caveInR + 1);

                for (int y = bMinY; y <= bMaxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!chunk.getBlockState(pos).is(GeoTectonicBlocks.CRYSTAL_VEIN_AIR.get())) continue;

                    int h = posHash(x, y, z, noiseSeed);
                    int b0 = h & 0xFF;
                    int b1 = (h >> 8) & 0xFF;

                    BlockPos above = pos.above();
                    BlockPos below = pos.below();
                    boolean solidAbove = above.getY() < chunk.getMaxBuildHeight()
                            && chunk.getBlockState(above).isSolid();
                    boolean solidBelow = below.getY() >= chunk.getMinBuildHeight()
                            && chunk.getBlockState(below).isSolid();

                    // Ceiling dripstone tips (5%)
                    if (solidAbove && b0 < 13) {
                        chunk.setBlockState(pos, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                            .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                            .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP)
                            .setValue(PointedDripstoneBlock.WATERLOGGED, false), false);
                        continue;
                    }

                    // Floor
                    if (solidBelow) {
                        if      (b1 < 153) chunk.setBlockState(below, Blocks.DEEPSLATE.defaultBlockState(), false);
                        else if (b1 < 230) chunk.setBlockState(below, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), false);
                        else               chunk.setBlockState(below, Blocks.GRAVEL.defaultBlockState(), false);
                    }

                    // Wall ores and decorations (horizontal neighbors only)
                    if (inCaveIn) continue; // no ore exposure near cave-in

                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int wx = x + dir.getStepX(), wz = z + dir.getStepZ();
                        if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                        BlockPos wallPos = new BlockPos(wx, y, wz);
                        BlockState wallState = chunk.getBlockState(wallPos);
                        if (!wallState.isSolid()) continue;

                        int wh = posHash(wx, y, wz, noiseSeed + 17);
                        int wb = wh & 0xFF;
                        int wb1 = (wh >> 8) & 0xFF;

                        // Calcite veining 10%
                        if (wb < 26) {
                            chunk.setBlockState(wallPos, Blocks.CALCITE.defaultBlockState(), false);
                            continue;
                        }
                        // Amethyst cluster 3% (toward passage interior)
                        if (wb < 33) {
                            chunk.setBlockState(wallPos, Blocks.AMETHYST_CLUSTER.defaultBlockState()
                                .setValue(AmethystClusterBlock.FACING, dir.getOpposite()), false);
                            continue;
                        }

                        // Wall ores — wall textures only
                        if (inAlcove) {
                            if      (wb1 < 20)  chunk.setBlockState(wallPos, Blocks.COAL_ORE.defaultBlockState(), false);
                            else if (wb1 < 35)  chunk.setBlockState(wallPos, Blocks.COPPER_ORE.defaultBlockState(), false);
                            else if (wb1 < 55)  chunk.setBlockState(wallPos, Blocks.IRON_ORE.defaultBlockState(), false);
                            else if (wb1 < 65)  chunk.setBlockState(wallPos, Blocks.GOLD_ORE.defaultBlockState(), false);
                            else if (wb1 < 75)  chunk.setBlockState(wallPos, Blocks.REDSTONE_ORE.defaultBlockState(), false);
                            else if (wb1 < 83)  chunk.setBlockState(wallPos, Blocks.LAPIS_ORE.defaultBlockState(), false);
                        } else {
                            // Middle passage zone
                            if      (wb1 < 15)  chunk.setBlockState(wallPos, Blocks.COAL_ORE.defaultBlockState(), false);
                            else if (wb1 < 28)  chunk.setBlockState(wallPos, Blocks.COPPER_ORE.defaultBlockState(), false);
                            else if (wb1 < 38)  chunk.setBlockState(wallPos, Blocks.IRON_ORE.defaultBlockState(), false);
                            else if (wb1 < 42)  chunk.setBlockState(wallPos, Blocks.GOLD_ORE.defaultBlockState(), false);
                            else if (wb1 < 46)  chunk.setBlockState(wallPos, Blocks.REDSTONE_ORE.defaultBlockState(), false);
                        }
                    }
                }
            }
        }

        // Diamond ore: exactly 1-2 blocks deterministically placed in alcove walls
        placeDiamondOre(chunk, minX, minZ, maxX, maxZ, alcovePt, noiseSeed);
    }

    private void placeDiamondOre(ChunkAccess chunk, int minX, int minZ, int maxX, int maxZ,
                                  PathPoint alcovePt, int noiseSeed) {
        int cx = alcovePt.center().getX();
        int cy = alcovePt.center().getY();
        int cz = alcovePt.center().getZ();
        int r = (int) Math.ceil(alcovePt.widthRadius());

        // Deterministically select 1 or 2 diamond positions from wall blocks via posHash ranking
        int diamondTarget = 1 + ((posHash(cx, cy, cz, noiseSeed + 42) & 1)); // 1 or 2

        int placed = 0;
        // Iterate in deterministic order, place diamond at highest-hash wall blocks
        int bestHash1 = Integer.MIN_VALUE, bestHash2 = Integer.MIN_VALUE;
        BlockPos best1 = null, best2 = null;

        for (int dx = -r; dx <= r && placed < diamondTarget; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int bx = cx + dx, bz = cz + dz;
                if (bx < minX || bx > maxX || bz < minZ || bz > maxZ) continue;
                for (int dy = -2; dy <= 2; dy++) {
                    int by = cy + dy;
                    if (by < chunk.getMinBuildHeight() || by >= chunk.getMaxBuildHeight()) continue;

                    // Must be a solid block adjacent to alcove cave air
                    BlockPos wallPos = new BlockPos(bx, by, bz);
                    if (!chunk.getBlockState(wallPos).isSolid()) continue;

                    boolean adjacentToAir = false;
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        BlockPos adj = wallPos.relative(dir);
                        if (adj.getX() >= minX && adj.getX() <= maxX && adj.getZ() >= minZ && adj.getZ() <= maxZ
                                && chunk.getBlockState(adj).is(GeoTectonicBlocks.CRYSTAL_VEIN_AIR.get())) {
                            adjacentToAir = true; break;
                        }
                    }
                    if (!adjacentToAir) continue;

                    int h = posHash(bx, by, bz, noiseSeed + 7777);
                    if (h > bestHash1) {
                        bestHash2 = bestHash1; best2 = best1;
                        bestHash1 = h; best1 = wallPos;
                    } else if (h > bestHash2) {
                        bestHash2 = h; best2 = wallPos;
                    }
                }
            }
        }

        if (best1 != null) chunk.setBlockState(best1, Blocks.DIAMOND_ORE.defaultBlockState(), false);
        if (diamondTarget >= 2 && best2 != null)
            chunk.setBlockState(best2, Blocks.DIAMOND_ORE.defaultBlockState(), false);
    }
}
