package net.alkeari.geotectonic.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class KarstChamberGenerator {

    public static KarstLayout generate(RandomSource rand, int openingX, int openingZ, int startY) {
        int hubY = 10 + rand.nextInt(16); // 10-25

        // Surface crack: drifts 2-3 XZ per Y so the shaft is always diagonal, never vertical.
        List<PathPoint> crackPoints = new ArrayList<>();
        float crackHeading = rand.nextFloat() * (float)(Math.PI * 2);
        int crackX = openingX, crackZ = openingZ;
        for (int y = startY; y >= hubY; y--) {
            float wr = 1.5f + rand.nextFloat() * 1.0f;
            float hr = 2.0f + rand.nextFloat() * 1.5f;
            crackPoints.add(new PathPoint(new BlockPos(crackX, y, crackZ), wr, hr));
            crackHeading += (rand.nextFloat() - 0.5f) * (float)(Math.PI / 6);
            int xzStep = 2 + rand.nextInt(2); // 2-3 blocks XZ per 1 Y
            crackX += (int) Math.round(Math.cos(crackHeading) * xzStep);
            crackZ += (int) Math.round(Math.sin(crackHeading) * xzStep);
        }

        // Main chamber placed where the crack lands, not at the surface opening.
        float mainWr = (10 + rand.nextInt(5)) * (0.65f + rand.nextFloat() * 0.70f);
        float mainHr = (7  + rand.nextInt(4)) * (0.65f + rand.nextFloat() * 0.70f);
        PathPoint mainChamber = new PathPoint(new BlockPos(crackX, hubY, crackZ), mainWr, mainHr);

        // Satellites: 2-4 rooms branching off the hub (hub is at crackX, crackZ).
        int satCount = 2 + rand.nextInt(3);
        List<PathPoint> satellites = new ArrayList<>();
        List<PathPoint> passages  = new ArrayList<>();

        float sectorSize = (float) (Math.PI * 2) / satCount;
        for (int i = 0; i < satCount; i++) {
            float angle = i * sectorSize + (rand.nextFloat() - 0.5f) * sectorSize * 0.5f;
            int dist  = 15 + rand.nextInt(16); // 15-30 blocks
            int satX  = crackX + (int) Math.round(Math.cos(angle) * dist);
            int satZ  = crackZ + (int) Math.round(Math.sin(angle) * dist);
            int satY  = hubY + (rand.nextInt(11) - 5); // +/-5

            float satWr = (5 + rand.nextInt(4)) * (0.65f + rand.nextFloat() * 0.70f);
            float satHr = (4 + rand.nextInt(3)) * (0.65f + rand.nextFloat() * 0.70f);
            satellites.add(new PathPoint(new BlockPos(satX, satY, satZ), satWr, satHr));

            // Passage: one PathPoint every ~2 blocks between hub and satellite
            float dx = satX - crackX;
            float dy = satY - hubY;
            float dz = satZ - crackZ;
            int steps = Math.max(1, (int) (Math.sqrt(dx * dx + dy * dy + dz * dz) / 2));
            for (int s = 1; s < steps; s++) {
                float t  = (float) s / steps;
                int px   = crackX + (int) Math.round(dx * t);
                int py   = hubY   + (int) Math.round(dy * t);
                int pz   = crackZ + (int) Math.round(dz * t);
                passages.add(new PathPoint(new BlockPos(px, py, pz), 2.0f, 2.5f));
            }
        }

        return new KarstLayout(crackPoints, mainChamber, satellites, passages);
    }
}
