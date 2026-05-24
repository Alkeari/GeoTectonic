package net.alkeari.geotectonic.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class FracturePathGenerator {

    public static List<PathPoint> generate(RandomSource rand, int openingX, int openingZ, int startY, int driftStartY) {
        List<PathPoint> points = new ArrayList<>();

        int targetDepthY = -30 - rand.nextInt(31); // −30 to −60
        // Branch trigger is calculated within the drift zone only, not from the sky start.
        int totalDriftDescent = driftStartY - targetDepthY;
        int branchTriggerY = driftStartY - (int)(totalDriftDescent * 0.65f);

        float heading = rand.nextFloat() * (float)(Math.PI * 2);
        int x = openingX;
        int z = openingZ;
        int y = startY;
        boolean branched = false;

        while (y > targetDepthY) {
            float wr = FractureShape.widthRadius(y)  * (0.75f + rand.nextFloat() * 0.50f);
            float hr = FractureShape.heightRadius(y) * (0.80f + rand.nextFloat() * 0.40f);
            points.add(new PathPoint(new BlockPos(x, y, z), wr, hr));

            if (!branched && y <= branchTriggerY) {
                branched = true;
                int branchCount = 2 + rand.nextInt(2);
                float sectorSize = (float)(Math.PI * 2) / branchCount;
                for (int b = 0; b < branchCount; b++) {
                    float offset = (float)(Math.PI / 3)
                        + b * sectorSize
                        + (rand.nextFloat() - 0.5f) * (float)(Math.PI / 6);
                    generateBranch(rand, x, y, z, heading + offset, points);
                }
            }

            heading += (rand.nextFloat() - 0.5f) * (float)(Math.PI / 6); // ±15°
            int xzStep = 2 + rand.nextInt(2); // 2-3 blocks XZ per 1 Y
            x += (int)Math.round(Math.cos(heading) * xzStep);
            z += (int)Math.round(Math.sin(heading) * xzStep);
            y--;
        }

        return points;
    }

    private static void generateBranch(RandomSource rand, int startX, int startY, int startZ,
                                        float angle, List<PathPoint> points) {
        int x = startX;
        int y = startY;
        int z = startZ;
        int horizontalTravel = 0;

        while (y > -60 && horizontalTravel < 80) {
            float wr = FractureShape.widthRadius(y)  * (0.75f + rand.nextFloat() * 0.50f);
            float hr = FractureShape.heightRadius(y) * (0.80f + rand.nextFloat() * 0.40f);
            points.add(new PathPoint(new BlockPos(x, y, z), wr, hr));

            angle += (rand.nextFloat() - 0.5f) * (float)(Math.PI / 6);
            int xzStep = 2 + rand.nextInt(2);
            int dx = (int)Math.round(Math.cos(angle) * xzStep);
            int dz = (int)Math.round(Math.sin(angle) * xzStep);
            x += dx;
            z += dz;
            horizontalTravel += (int)Math.sqrt(dx * dx + dz * dz);
            y--;
        }
    }
}
