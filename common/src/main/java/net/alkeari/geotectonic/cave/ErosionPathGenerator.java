package net.alkeari.geotectonic.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class ErosionPathGenerator {

    /**
     * Generates control-point waypoints for an erosion cave starting at (openingX, startY, openingZ).
     * ErosionCarver interpolates ellipsoids every 1 block between consecutive waypoints so they
     * always form a continuous tunnel regardless of control-point spacing.
     *
     * Heading: 0=north(−Z), 90=east(+X), 180=south(+Z), 270=west(−X).
     * Random draw order (must not change — cross-chunk consistency depends on it):
     *   totalRun       — once at start
     *   initialHeading — once at start (0/90/180/270 degrees)
     *   per waypoint:  stepXZ, driftDeg, descentY, wr, hr
     */
    public static List<PathPoint> generate(RandomSource random, int openingX, int openingZ, int startY) {
        List<PathPoint> points = new ArrayList<>();
        int totalRun = 60 + random.nextInt(61);           // 60–120 blocks total horizontal travel
        float headingDeg = random.nextInt(4) * 90.0f;    // 0, 90, 180, or 270
        int chamberTrigger = totalRun / 3 + random.nextInt(Math.max(1, totalRun / 3)); // 33–67% of run
        boolean chamberPlaced = false;

        int cx = openingX;
        int cy = startY;
        int cz = openingZ;
        int travelSoFar = 0;

        while (travelSoFar < totalRun && cy >= 10 && cy <= startY) {
            int stepXZ   = 5 + random.nextInt(4);             // 5–8 blocks between control points
            int driftDeg = (random.nextInt(9) - 4) * 5;      // −20 to +20 in 5° steps
            int descentY = random.nextInt(2);                  // 0 or 1
            float wr     = 1.8f + random.nextFloat() * 1.2f; // 1.8–3.0 width radius
            float hr     = 1.8f + random.nextFloat() * 0.7f; // 1.8–2.5 height radius

            headingDeg = ((headingDeg + driftDeg) % 360 + 360) % 360;
            double rad = Math.toRadians(headingDeg);
            cx += (int) Math.round(Math.sin(rad) * stepXZ);
            cz -= (int) Math.round(Math.cos(rad) * stepXZ);
            cy -= descentY;
            travelSoFar += stepXZ;

            // Widen one waypoint in the middle section into a pool chamber
            if (!chamberPlaced && travelSoFar >= chamberTrigger) {
                chamberPlaced = true;
                wr = 4.0f + random.nextFloat() * 1.5f; // 4.0–5.5
                hr = 3.0f + random.nextFloat() * 1.0f; // 3.0–4.0
            }

            points.add(new PathPoint(new BlockPos(cx, cy, cz), wr, hr));
        }

        return points;
    }
}
