package net.alkeari.geotectonic.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class CrystalVeinPathGenerator {

    public static List<PathPoint> generate(RandomSource random, int startX, int startZ, int startY, int initialHeading) {
        List<PathPoint> points = new ArrayList<>();

        int totalRun   = 20 + random.nextInt(21);   // 20–40 blocks
        float headingDeg = initialHeading;

        int cx = startX;
        int cy = startY;
        int cz = startZ;
        int travelSoFar = 0;

        while (travelSoFar < totalRun) {
            int stepXZ   = 4 + random.nextInt(3);             // 4–6 blocks
            int driftDeg = (random.nextInt(7) - 3) * 5;      // −15 to +15 in 5° steps
            int yDelta   = random.nextInt(10) < 2 ? -1 : 0;  // 20% chance −1
            float wr     = 1.5f;
            float hr     = 2.0f;

            headingDeg = ((headingDeg + driftDeg) % 360 + 360) % 360;
            double rad = Math.toRadians(headingDeg);
            cx += (int) Math.round(Math.sin(rad) * stepXZ);
            cz -= (int) Math.round(Math.cos(rad) * stepXZ);
            cy += yDelta;
            travelSoFar += stepXZ;

            points.add(new PathPoint(new BlockPos(cx, cy, cz), wr, hr));
        }

        // Replace last waypoint with ore-rich alcove dimensions
        if (!points.isEmpty()) {
            PathPoint last = points.get(points.size() - 1);
            points.set(points.size() - 1,
                new PathPoint(last.center(), 4.0f, 3.0f));
        }

        return points;
    }
}
