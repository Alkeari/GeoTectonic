package net.alkeari.geotectonic.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class ErosionPathGenerator {

    /**
     * Generates trunk waypoints for an erosion cave starting at (openingX, startY, openingZ).
     *
     * Heading: 0=north(−Z), 90=east(+X), 180=south(+Z), 270=west(−X).
     * Random draw order (deterministic, must not change):
     *   totalRun       — once at start
     *   initialHeading — once at start (0/90/180/270 degrees)
     *   per waypoint: stepXZ, driftDeg, descentY, wr, hr
     */
    public static List<PathPoint> generate(RandomSource random, int openingX, int openingZ, int startY) {
        List<PathPoint> points = new ArrayList<>();
        int totalRun = 60 + random.nextInt(61);            // 60–120 blocks, drawn first
        float headingDeg = random.nextInt(4) * 90.0f;     // 0, 90, 180, or 270

        int cx = openingX;
        int cy = startY;
        int cz = openingZ;
        int travelSoFar = 0;

        while (travelSoFar < totalRun && cy >= 20 && cy <= startY) {
            int stepXZ   = 5 + random.nextInt(4);              // 5–8 XZ per step
            int driftDeg = (random.nextInt(9) - 4) * 5;       // −20 to +20 in 5° steps
            int descentY = random.nextInt(2);                   // 0 or 1
            float wr     = 1.8f + random.nextFloat() * 1.2f;  // 1.8–3.0
            float hr     = 1.8f + random.nextFloat() * 0.7f;  // 1.8–2.5

            headingDeg = ((headingDeg + driftDeg) % 360 + 360) % 360;
            double rad = Math.toRadians(headingDeg);
            cx += (int) Math.round(Math.sin(rad) * stepXZ);
            cz -= (int) Math.round(Math.cos(rad) * stepXZ);
            cy -= descentY;

            points.add(new PathPoint(new BlockPos(cx, cy, cz), wr, hr));
            travelSoFar += stepXZ;
        }

        return points;
    }
}
