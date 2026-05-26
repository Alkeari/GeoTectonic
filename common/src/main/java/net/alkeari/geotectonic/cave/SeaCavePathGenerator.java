package net.alkeari.geotectonic.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class SeaCavePathGenerator {

    /**
     * Generates control-point waypoints for a sea cave starting at (mouthX, seaLevel-7, mouthZ).
     * SeaCaveCarver interpolates ellipsoids every 1 block between consecutive waypoints.
     *
     * Heading: 0=north(−Z), 90=east(+X), 180=south(+Z), 270=west(−X).
     * Random draw order (must not change — cross-chunk consistency depends on it):
     *   totalRun           — once at start
     *   initialHeading     — once at start (0/90/180/270 degrees)
     *   per waypoint:      stepXZ, driftDeg, yDelta, wr, hr
     *   chamber dims:      chamberWr, chamberHr (after loop exits, always drawn)
     */
    public static List<PathPoint> generate(RandomSource random, int mouthX, int mouthZ, int seaLevel) {
        List<PathPoint> points = new ArrayList<>();
        int totalRun = 30 + random.nextInt(31);  // 30–60 blocks horizontal travel
        float headingDeg = random.nextInt(4) * 90.0f;  // 0, 90, 180, or 270

        int cx = mouthX;
        int cy = seaLevel - 7;
        int cz = mouthZ;
        int travelSoFar = 0;

        while (travelSoFar < totalRun) {
            int stepXZ   = 4 + random.nextInt(5);             // 4–8 blocks between control points
            int driftDeg = (random.nextInt(9) - 4) * 5;      // −20 to +20 in 5° steps
            int yDelta   = random.nextInt(2);                  // 0 or 1 descent per step
            float wr     = 2.5f + random.nextFloat() * 1.5f;  // 2.5–4.0 width radius
            float hr     = 1.5f + random.nextFloat() * 1.0f;  // 1.5–2.5 height radius

            headingDeg = ((headingDeg + driftDeg) % 360 + 360) % 360;
            double rad = Math.toRadians(headingDeg);
            cx += (int) Math.round(Math.sin(rad) * stepXZ);
            cz -= (int) Math.round(Math.cos(rad) * stepXZ);
            cy -= yDelta;
            travelSoFar += stepXZ;

            points.add(new PathPoint(new BlockPos(cx, cy, cz), wr, hr));
        }

        // Always consume chamber draws for consistent random state, replace final waypoint dims
        float chamberWr = 5.0f + random.nextFloat() * 4.0f;  // 5.0–9.0
        float chamberHr = 3.0f + random.nextFloat() * 2.0f;  // 3.0–5.0
        if (!points.isEmpty()) {
            PathPoint last = points.get(points.size() - 1);
            points.set(points.size() - 1, new PathPoint(last.center(), chamberWr, chamberHr));
        }

        return points;
    }
}
